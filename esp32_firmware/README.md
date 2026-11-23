# SMART SOIL — ESP32 Firmware, Wiring & Calibration Guide

## Hardware Bill of Materials
- **ESP32 DevKit V1** (3.3V logic, 12-bit ADC)
- **HW-390 Capacitive Soil Moisture Sensor**
- **DS18B20 Waterproof Temperature Sensor** + **4.7kΩ pull-up resistor**
- **ADIY Soil pH Module with BNC Probe**

## Wiring Table

| Sensor | Sensor Pin | ESP32 DevKit V1 Pin | Notes |
|---|---|---|---|
| **HW-390 Moisture** | VCC | `3V3` | Power at 3.3V so analog output stays within 0–3.3V |
| **HW-390 Moisture** | GND | `GND` | Common ground |
| **HW-390 Moisture** | AOUT | `GPIO 34` | ADC1_CH6 (input-only, safe during BLE) |
| **DS18B20 Temp** | VCC (Red) | `3V3` | 3.3V supply |
| **DS18B20 Temp** | GND (Black) | `GND` | Common ground |
| **DS18B20 Temp** | DATA (Yellow) | `GPIO 4` | Connect a **4.7kΩ resistor** between `GPIO 4` and `3V3` |
| **ADIY Soil pH** | V+ / VCC | Verify datasheet (often 5V) | **Check electrical compatibility before wiring!** |
| **ADIY Soil pH** | GND | `GND` | Common ground |
| **ADIY Soil pH** | Po / Analog Out | `GPIO 35` *(via divider if >3.3V)* | **Measure Po with a multimeter first.** If Po can exceed 3.3V, use a resistor divider (e.g. 10kΩ + 20kΩ) before GPIO 35. |

## BLE UUIDs (Must Match Android App)
- **Device Name:** `SMART_SOIL_ESP32`
- **Service UUID:** `4fafc201-1fb5-459e-8fcc-c5c9c331914b`
- **Sensor Characteristic UUID (Read + Notify):** `beb5483e-36e1-4688-b7f5-ea07361b26a8`

## Arduino IDE Setup & Upload
1. Install **ESP32 Board Support** in Arduino IDE Boards Manager (`esp32 by Espressif Systems`).
2. Install libraries via Library Manager:
   - `OneWire`
   - `DallasTemperature`
   - `ArduinoJson`
3. Open `esp32_firmware/smart_soil_esp32/smart_soil_esp32.ino`.
4. Select Board: **DOIT ESP32 DEVKIT V1**, select the USB COM/tty port, and click **Upload**.
5. Open **Serial Monitor** at **115200 baud**. Verify JSON lines print every 3 seconds, e.g.:
   `{"moisture_raw":2408,"moisture_percent":44,"ph":null,"temperature_c":26.8,"timestamp":12}`

## Sensor Calibration
1. **HW-390 Moisture:** Hold the dry sensor in air, read `moisture_raw` on Serial Monitor, and enter it as `Dry ADC (0%)` in the app's **BLE & Thresholds** sheet. Submerge up to the white line in water to read `Wet ADC (100%)`.
2. **ADIY pH Module:** Only after verifying output voltage ≤ 3.3V, immerse the BNC probe in pH 7.00 and pH 4.01 buffer solutions, calculate `PH_SLOPE` and `PH_OFFSET`, and set `PH_PROBE_VERIFIED_AND_ENABLED = true`.
