package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ble.BleSoilManager
import com.example.data.local.UserPreferencesState
import com.example.data.model.BleConnectionStatus
import com.example.data.model.BleState
import com.example.data.model.CropCatalog
import com.example.ui.components.ClayCard
import com.example.ui.components.ClayPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.LightErrorContainer
import com.example.ui.theme.LightOnErrorContainer
import com.example.ui.theme.LightOnSurface
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightPrimary
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceContainer
import com.example.ui.theme.MonoSmallStyle
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.WarningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleAndHardwareSheet(
    bleManager: BleSoilManager,
    bleState: BleState,
    preferences: UserPreferencesState,
    onUpdateCrop: (String, String) -> Unit,
    onUpdateThresholds: (Int, Int, Double, Double) -> Unit,
    onUpdateCalibration: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            bleManager.startScanAndConnect()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LightSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "FARMERBUDDY · SMART SOIL",
                style = MaterialTheme.typography.labelSmall,
                color = LightPrimary
            )
            Text(
                text = "ESP32 BLE & Crop Calibration",
                style = MaterialTheme.typography.headlineMedium,
                color = LightOnSurface
            )

            // 1. BLE Connection Panel
            ClayCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ESP32 Bluetooth LE", style = MaterialTheme.typography.titleLarge)
                    val (label, bg, fg) = when (bleState.status) {
                        BleConnectionStatus.CONNECTED -> Triple("Connected", SuccessContainer, OnSuccessContainer)
                        BleConnectionStatus.SCANNING -> Triple("Scanning...", WarningContainer, OnWarningContainer)
                        BleConnectionStatus.CONNECTING -> Triple("Connecting...", WarningContainer, OnWarningContainer)
                        BleConnectionStatus.BLE_UNSUPPORTED -> Triple("Unsupported", LightErrorContainer, LightOnErrorContainer)
                        BleConnectionStatus.BLUETOOTH_DISABLED -> Triple("BT Off", WarningContainer, OnWarningContainer)
                        BleConnectionStatus.PERMISSION_REQUIRED -> Triple("Need Permission", WarningContainer, OnWarningContainer)
                        BleConnectionStatus.ERROR -> Triple("Error", LightErrorContainer, LightOnErrorContainer)
                        BleConnectionStatus.DISCONNECTED -> Triple("Disconnected", LightSurfaceContainer, LightOnSurfaceVariant)
                    }
                    StatusPill(text = label, containerColor = bg, contentColor = fg)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Service UUID: ${preferences.calibration.serviceUuid}\nChar UUID: ${preferences.calibration.characteristicUuid}",
                    style = MonoSmallStyle,
                    color = LightOnSurfaceVariant
                )

                if (bleState.connectedDeviceName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Device: ${bleState.connectedDeviceName} (${bleState.connectedDeviceAddress ?: ""})",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightPrimary
                    )
                }

                if (bleState.lastReading?.timestampMs != null) {
                    val fmt = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault())
                    Text(
                        text = "Last packet: ${fmt.format(Date(bleState.lastReading.timestampMs))}",
                        style = MonoSmallStyle,
                        color = LightOnSurfaceVariant
                    )
                }

                if (bleState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = bleState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = LightOnErrorContainer
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (bleState.status == BleConnectionStatus.CONNECTED) {
                        OutlinedButton(
                            onClick = { bleManager.disconnect() },
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("ble_disconnect_btn")
                        ) {
                            Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Disconnect")
                        }
                        ClayPrimaryButton(
                            text = "Reconnect",
                            icon = Icons.Default.Refresh,
                            onClick = { bleManager.reconnectLastDevice() },
                            modifier = Modifier.weight(1f),
                            testTag = "ble_reconnect_btn"
                        )
                    } else {
                        ClayPrimaryButton(
                            text = if (bleState.status == BleConnectionStatus.SCANNING) "Scanning..." else "Connect ESP32",
                            icon = Icons.Default.Bluetooth,
                            enabled = bleState.status != BleConnectionStatus.SCANNING && bleState.status != BleConnectionStatus.BLE_UNSUPPORTED,
                            onClick = {
                                if (bleManager.hasRequiredPermissions()) {
                                    bleManager.startScanAndConnect()
                                } else {
                                    blePermissionLauncher.launch(bleManager.requiredPermissions())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "ble_connect_btn"
                        )
                    }
                }
            }

            // 2. Crop & Growth Stage Selector
            var cropExpanded by remember { mutableStateOf(false) }
            var stageExpanded by remember { mutableStateOf(false) }
            var chosenCrop by remember(preferences.selectedCropName) { mutableStateOf(preferences.selectedCropName) }
            var chosenStage by remember(preferences.growthStage) { mutableStateOf(preferences.growthStage) }

            ClayCard {
                Text("Crop & Threshold Profile", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = cropExpanded,
                    onExpandedChange = { cropExpanded = !cropExpanded }
                ) {
                    OutlinedTextField(
                        value = chosenCrop,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Active Crop") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cropExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = cropExpanded,
                        onDismissRequest = { cropExpanded = false }
                    ) {
                        CropCatalog.presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text("${preset.name} (M ${preset.minMoisturePct}-${preset.maxMoisturePct}%, pH ${preset.minPh}-${preset.maxPh})") },
                                onClick = {
                                    chosenCrop = preset.name
                                    cropExpanded = false
                                    onUpdateCrop(chosenCrop, chosenStage)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = stageExpanded,
                    onExpandedChange = { stageExpanded = !stageExpanded }
                ) {
                    OutlinedTextField(
                        value = chosenStage,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Growth Stage") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = stageExpanded,
                        onDismissRequest = { stageExpanded = false }
                    ) {
                        CropCatalog.growthStages.forEach { stage ->
                            DropdownMenuItem(
                                text = { Text(stage) },
                                onClick = {
                                    chosenStage = stage
                                    stageExpanded = false
                                    onUpdateCrop(chosenCrop, chosenStage)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                var minMText by remember(preferences.customMinMoisture) { mutableStateOf(preferences.customMinMoisture.toString()) }
                var maxMText by remember(preferences.customMaxMoisture) { mutableStateOf(preferences.customMaxMoisture.toString()) }
                var minPhText by remember(preferences.customMinPh) { mutableStateOf(preferences.customMinPh.toString()) }
                var maxPhText by remember(preferences.customMaxPh) { mutableStateOf(preferences.customMaxPh.toString()) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minMText,
                        onValueChange = { minMText = it },
                        label = { Text("Min Moist %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxMText,
                        onValueChange = { maxMText = it },
                        label = { Text("Max Moist %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minPhText,
                        onValueChange = { minPhText = it },
                        label = { Text("Min pH") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxPhText,
                        onValueChange = { maxPhText = it },
                        label = { Text("Max pH") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val minM = minMText.toIntOrNull() ?: preferences.customMinMoisture
                        val maxM = maxMText.toIntOrNull() ?: preferences.customMaxMoisture
                        val minP = minPhText.toDoubleOrNull() ?: preferences.customMinPh
                        val maxP = maxPhText.toDoubleOrNull() ?: preferences.customMaxPh
                        onUpdateThresholds(minM, maxM, minP, maxP)
                    },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                ) {
                    Text("Save Custom Crop Thresholds")
                }
            }

            // 3. HW-390 Capacitive Moisture Calibration
            var dryAdcText by remember(preferences.calibration.dryAdcValue) { mutableStateOf(preferences.calibration.dryAdcValue.toString()) }
            var wetAdcText by remember(preferences.calibration.wetAdcValue) { mutableStateOf(preferences.calibration.wetAdcValue.toString()) }

            ClayCard {
                Text("HW-390 Moisture ADC Calibration", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Capacitive moisture sensors vary by soil & board. Record your ESP32 ADC value in dry air (0%) and submerged in water (100%).",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dryAdcText,
                        onValueChange = { dryAdcText = it },
                        label = { Text("Dry ADC (0%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wetAdcText,
                        onValueChange = { wetAdcText = it },
                        label = { Text("Wet ADC (100%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val dry = dryAdcText.toIntOrNull() ?: 3200
                        val wet = wetAdcText.toIntOrNull() ?: 1400
                        onUpdateCalibration(dry, wet)
                    },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                ) {
                    Text("Apply ADC Calibration")
                }
            }

            // 4. Hardware & Wiring Quick Reference
            ClayCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = LightPrimary)
                    Text("ESP32 DevKit V1 Hardware Summary", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• HW-390 Capacitive Moisture -> GPIO 34 (ADC1_CH6, 3.3V)\n" +
                        "• DS18B20 Temperature -> GPIO 4 (OneWire + 4.7kΩ pull-up to 3.3V)\n" +
                        "• ADIY Soil pH + BNC -> GPIO 35 (Verify 5V vs 3.3V output! Use resistor voltage divider if module outputs >3.3V before connecting to ESP32 ADC)\n" +
                        "• Arduino IDE Firmware: /esp32_firmware/smart_soil_esp32/smart_soil_esp32.ino",
                    style = MonoSmallStyle,
                    color = LightOnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
