/*
 * SMART SOIL — ESP32 DevKit V1 BLE Sensor Node Firmware (Arduino IDE)
 *
 * Hardware:
 *  - ESP32 DevKit V1
 *  - HW-390 Capacitive Soil Moisture Sensor (Analog -> GPIO 34)
 *  - DS18B20 Waterproof Temperature Sensor (OneWire -> GPIO 4, 4.7k pull-up to 3.3V)
 *  - ADIY Soil pH Module with BNC Probe (Analog -> GPIO 35)
 *    CRITICAL SAFETY NOTE: Verify your pH module output voltage with a multimeter BEFORE
 *    connecting to ESP32 GPIO 35. ESP32 GPIO pins are 3.3V max (NOT 5V tolerant).
 *    If your module is powered by 5V and outputs >3.3V, use a resistor voltage divider!
 *
 * Required Arduino Libraries:
 *  - OneWire (by Jim Studt et al.)
 *  - DallasTemperature (by Miles Burton)
 *  - ArduinoJson (by Benoit Blanchon, v6 or v7)
 *  - Built-in ESP32 BLE libraries (BLEDevice, BLEServer, BLEUtils, BLE2902)
 */

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <ArduinoJson.h>

// ==========================================
// 1. CONFIGURABLE GPIO PINS & BLE UUIDs
// ==========================================
#define PIN_MOISTURE_ADC   34   // Input-only ADC1_CH6 (safe with Wi-Fi/BLE)
#define PIN_PH_ADC         35   // Input-only ADC1_CH7 (verify <=3.3V before wiring!)
#define PIN_DS18B20_DATA    4   // OneWire digital pin with 4.7k pull-up to 3.3V

#define BLE_DEVICE_NAME    "SMART_SOIL_ESP32"
#define SERVICE_UUID       "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHAR_SENSOR_UUID   "beb5483e-36e1-4688-b7f5-ea07361b26a8"

// ==========================================
// 2. CALIBRATION CONFIGURATION
// ==========================================
// Measure your HW-390 raw ADC in dry air (0%) and in water (100%) via Serial Monitor:
const int MOISTURE_DRY_ADC = 3200;
const int MOISTURE_WET_ADC = 1400;

// Set to true ONLY AFTER verifying pH module voltage <= 3.3V and calibrating with pH 4.0 & 7.0 buffers:
const bool PH_PROBE_VERIFIED_AND_ENABLED = false;
const float PH_SLOPE = -5.70f;       // Replace with your calibrated slope
const float PH_OFFSET = 21.34f;      // Replace with your calibrated offset

const unsigned long PUBLISH_INTERVAL_MS = 3000;

// ==========================================
// 3. GLOBALS
// ==========================================
OneWire oneWire(PIN_DS18B20_DATA);
DallasTemperature tempSensors(&oneWire);

BLEServer* pServer = nullptr;
BLECharacteristic* pSensorChar = nullptr;
bool deviceConnected = false;
unsigned long lastPublishMs = 0;

class SoilServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    deviceConnected = true;
    Serial.println("[BLE] Phone connected");
  }
  void onDisconnect(BLEServer* server) override {
    deviceConnected = false;
    Serial.println("[BLE] Phone disconnected — restarting advertising");
    server->getAdvertising()->start();
  }
};

// Average multiple ADC samples for stability
int readAveragedAdc(int pin, int samples = 10) {
  long sum = 0;
  for (int i = 0; i < samples; i++) {
    sum += analogRead(pin);
    delay(10);
  }
  return (int)(sum / samples);
}

int computeMoisturePercent(int rawAdc) {
  if (MOISTURE_DRY_ADC == MOISTURE_WET_ADC) return -1;
  float pct = ((float)(MOISTURE_DRY_ADC - rawAdc) / (float)(MOISTURE_DRY_ADC - MOISTURE_WET_ADC)) * 100.0f;
  if (pct < 0.0f) pct = 0.0f;
  if (pct > 100.0f) pct = 100.0f;
  return (int)roundf(pct);
}

void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.println("\n=== SMART SOIL ESP32 Starting ===");

  analogReadResolution(12); // 0..4095
  tempSensors.begin();

  BLEDevice::init(BLE_DEVICE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new SoilServerCallbacks());

  BLEService* pService = pServer->createService(SERVICE_UUID);
  pSensorChar = pService->createCharacteristic(
      CHAR_SENSOR_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
  );
  pSensorChar->addDescriptor(new BLE2902());
  pService->start();

  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  BLEDevice::startAdvertising();

  Serial.println("[BLE] Advertising as SMART_SOIL_ESP32");
}

void loop() {
  unsigned long now = millis();
  if (now - lastPublishMs < PUBLISH_INTERVAL_MS) {
    delay(50);
    return;
  }
  lastPublishMs = now;

  // 1. Read HW-390 Moisture
  int moistureRaw = readAveragedAdc(PIN_MOISTURE_ADC, 12);
  bool moistureValid = (moistureRaw > 50 && moistureRaw < 4090);
  int moisturePct = moistureValid ? computeMoisturePercent(moistureRaw) : -1;

  // 2. Read DS18B20 Temperature
  tempSensors.requestTemperatures();
  float tempC = tempSensors.getTempCByIndex(0);
  bool tempValid = (tempC != DEVICE_DISCONNECTED_C && tempC > -40.0f && tempC < 85.0f);

  // 3. Read ADIY pH Probe (only if verified & enabled)
  bool phValid = false;
  float phValue = 0.0f;
  if (PH_PROBE_VERIFIED_AND_ENABLED) {
    int phRaw = readAveragedAdc(PIN_PH_ADC, 15);
    float voltage = (phRaw / 4095.0f) * 3.3f;
    float candidate = PH_SLOPE * voltage + PH_OFFSET;
    if (candidate >= 0.0f && candidate <= 14.0f) {
      phValue = roundf(candidate * 100.0f) / 100.0f;
      phValid = true;
    }
  }

  // 4. Build JSON packet — NEVER send fake values for disconnected sensors
  StaticJsonDocument<192> doc;
  if (moistureValid) {
    doc["moisture_raw"] = moistureRaw;
    doc["moisture_percent"] = moisturePct;
  } else {
    doc["moisture_raw"] = nullptr;
    doc["moisture_percent"] = nullptr;
  }

  if (phValid) {
    doc["ph"] = phValue;
  } else {
    doc["ph"] = nullptr;
  }

  if (tempValid) {
    doc["temperature_c"] = roundf(tempC * 10.0f) / 10.0f;
  } else {
    doc["temperature_c"] = nullptr;
  }

  doc["timestamp"] = now / 1000;

  String payload;
  serializeJson(doc, payload);
  Serial.println(payload);

  if (deviceConnected) {
    pSensorChar->setValue(payload.c_str());
    pSensorChar->notify();
  }
}
