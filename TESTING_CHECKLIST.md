# SMART SOIL — Testing Checklist & Verification Status

## Verified Automated Checks (Local JVM / Robolectric & Gradle Build)
- [x] **Gradle Debug Build (`compile_applet`)**: Verified Kotlin, Jetpack Compose, Room + KSP, DataStore, Coil, and Firebase AI Logic + App Check compile cleanly.
- [x] **App Identity & Resources (`ExampleRobolectricTest`)**: Verified `R.string.app_name` equals `"Smart Soil"`.
- [x] **Local-First Persistence & Demo/Live Separation (`ExampleRobolectricTest`)**: Verified Room `SoilRecordDao` inserts live & demo records and `deleteDemoRecords()` removes only demo records while preserving live records.
- [x] **ESP32 BLE JSON Validation (`ExampleUnitTest`)**:
  - [x] Valid JSON packets preserve `moisture_raw` and compute calibrated `moisture_percent`.
  - [x] Disconnected DS18B20 (`-127.0`) and `null` pH values surface as `null` (unavailable) with validation warnings rather than fabricating fake readings.
  - [x] Out-of-range values (`moisture_raw: 9999`, `ph: 19.5`, `temperature_c: 250.0`) and malformed non-JSON packets are rejected cleanly.

## Manual / Device & Hardware Verification Needed (Pending Physical Hardware)
- [ ] **ESP32 BLE Connection**: Flash `/esp32_firmware/smart_soil_esp32/smart_soil_esp32.ino` to ESP32 DevKit V1, verify Serial Monitor JSON output at 115200 baud, and test `Connect ESP32`, `Disconnect`, and `Reconnect` on a physical Android phone with Bluetooth enabled.
- [ ] **HW-390 & ADIY pH Calibration**: Measure dry-air and submerged ADC counts for HW-390, verify pH module output voltage is ≤ 3.3V (using a resistor divider if needed), and calibrate with pH 4.01 / 7.00 buffers.
- [ ] **Camera Capture & Photo Picker**: Test `Pick Photo` and `Take Photo` flows on Android device/emulator across Soil Scan, Crop Identification, Plant Health Scan, and NPK Lab Report.
- [ ] **Live Firebase AI Logic Inference**: Test Soil Scan, Crop Identification, Plant Health Scan, NPK Extraction, Regional Reference, and AI Advisor with network connectivity on device/streaming emulator.

## Known Limitations
1. **Android Emulator BLE**: Standard emulators lack physical Bluetooth LE radio hardware; BLE scanning shows `"Unsupported"` / `"Disconnected"` and Demo Mode should be used when presenting without a physical Android phone + ESP32.
2. **Visual & Regional Estimates**: Soil/Plant photo scans and Regional NPK references cannot replace laboratory Soil Health Card testing or calibrated physical probes.
3. **ADIY pH Probe Wiring**: `PH_PROBE_VERIFIED_AND_ENABLED` defaults to `false` in ESP32 firmware until electrical compatibility (≤ 3.3V ADC input) and two-point buffer calibration are verified.
