# FarmerBuddy (SMART SOIL) — Testing Checklist & Verification Status

## Verified Automated Checks (Android Gradle Build & Test Compilation)
- [x] **Gradle Debug Build (`compile_applet`)**: Verified Kotlin, Jetpack Compose, Room + KSP, DataStore, Coil, and Firebase AI Logic + App Check compile cleanly after the FarmerBuddy redesign and scanner audit.
- [x] **Unit & Robolectric Test Compilation (`gradle :app:compileDebugUnitTestKotlin`)**: Verified `ExampleRobolectricTest` (updated for `"FarmerBuddy"` and `"SCAN YOUR SOIL"`) and `ExampleUnitTest` (ESP32 BLE JSON parser tests) compile cleanly.

## Blocked in Container Environment
- **Robolectric Test Execution (`gradle :app:testDebugUnitTest`)**: Failed in the sandbox container because Robolectric `MavenArtifactFetcher` encountered `IOException` downloading Android SDK 36 artifacts from Maven Central.
- **Web TypeScript/Vite Build (`npm run build`) & Node API Tests**: Not executed inside this Android container; verify locally or in Vercel deployment logs (`tsc --noEmit && vite build`).

## Manual / Device, Web & Hardware Verification Needed
- [ ] **ESP32 BLE Connection**: Flash `/esp32_firmware/smart_soil_esp32/smart_soil_esp32.ino` to ESP32 DevKit V1, verify Serial Monitor JSON output at 115200 baud, and test `Connect ESP32`, `Disconnect`, and `Reconnect` on Chrome/Edge (Web Bluetooth) and on a physical Android phone.
- [ ] **HW-390 & ADIY pH Calibration**: Measure dry-air and submerged ADC counts for HW-390, verify pH module output voltage is <= 3.3V (using a resistor divider if needed), and calibrate with pH 4.01 / 7.00 buffers.
- [ ] **5-Scanner End-to-End Flow & Persistent Save (Web & Android)**:
  - Verify `Take Photo` (camera) and `Upload Image` (gallery) both open properly across **Soil Scan**, **Identify Crop**, **Plant Health**, and **NPK Lab Card OCR**, and verify rotated portrait photos decode upright (EXIF orientation applied on Android).
  - Verify selected photo shows **Image Preview**, requires **Start Scan**, displays **Analysing...**, renders **Scan Complete** with **Save Result** and **Scan Again**, and preserves the error + **Try Again** flow on oversized/corrupted files or network failures.
  - Verify **Regional NPK Guide** displays `REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST` and saves explicitly via **Save Result**.
  - Verify **Save Result** persists records in Room + internal `filesDir/saved_scans/` (Android) and quota-safe `localStorage` thumbnails (Web) across app restarts, with snackbar/toast confirmation.
- [ ] **Vercel Deployment & Live Gemini API**: Confirm `GEMINI_API_KEY` is set in Vercel Environment Variables and test all `/api/*` endpoints (`/api/analyze-soil`, `/api/identify-crop`, `/api/plant-health`, `/api/npk-extract`, `/api/npk-regional`, `/api/advisor`).

## Known Limitations
1. **Android Emulator BLE**: Standard emulators lack physical Bluetooth LE radio hardware; BLE scanning shows `"Unsupported"` / `"Disconnected"` and Demo Mode should be used when presenting without a physical Android phone + ESP32.
2. **Visual & Regional Estimates**: Soil/Plant photo scans and Regional NPK references cannot replace laboratory Soil Health Card testing or calibrated physical probes.
3. **ADIY pH Probe Wiring**: `PH_PROBE_VERIFIED_AND_ENABLED` defaults to `false` in ESP32 firmware until electrical compatibility (<= 3.3V ADC input) and two-point buffer calibration are verified.
