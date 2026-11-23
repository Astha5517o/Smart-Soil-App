package com.example.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.BleConnectionStatus
import com.example.data.model.BleState
import com.example.data.model.CalibrationConfig
import com.example.data.model.SensorReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

private const val TAG = "BleSoilManager"
private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

/**
 * Pure Kotlin parser & strict validator for incoming ESP32 BLE JSON payloads.
 * Rejects impossible or missing values without silently converting them into fake readings.
 */
object SensorPayloadParser {
    fun parseAndValidate(
        rawJson: String,
        calibration: CalibrationConfig,
        receivedAtMs: Long = System.currentTimeMillis()
    ): Result<SensorReading> {
        return try {
            val trimmed = rawJson.trim()
            if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
                return Result.failure(IllegalArgumentException("Malformed sensor packet (not JSON object)."))
            }
            val json = JSONObject(trimmed)
            val errors = mutableListOf<String>()

            val moistureRaw: Int? = if (json.has("moisture_raw") && !json.isNull("moisture_raw")) {
                val raw = json.optDouble("moisture_raw", Double.NaN)
                if (raw.isFinite() && raw in 0.0..4095.0) {
                    raw.toInt()
                } else {
                    errors.add("Invalid moisture_raw ADC value (must be 0..4095).")
                    null
                }
            } else {
                null
            }

            // Use calibrated moisture from app config when raw ADC is provided,
            // or validate explicit moisture_percent from firmware if valid.
            val moisturePercent: Int? = if (moistureRaw != null) {
                calibration.computeMoistureFromRaw(moistureRaw)
            } else if (json.has("moisture_percent") && !json.isNull("moisture_percent")) {
                val pct = json.optDouble("moisture_percent", Double.NaN)
                if (pct.isFinite() && pct in 0.0..100.0) {
                    pct.toInt()
                } else {
                    errors.add("Invalid moisture_percent (must be 0..100).")
                    null
                }
            } else {
                errors.add("Moisture sensor reading unavailable.")
                null
            }

            val ph: Double? = if (json.has("ph") && !json.isNull("ph")) {
                val v = json.optDouble("ph", Double.NaN)
                if (v.isFinite() && v in 0.0..14.0) {
                    kotlin.math.round(v * 100.0) / 100.0
                } else {
                    errors.add("Invalid pH reading (must be 0.0..14.0).")
                    null
                }
            } else {
                errors.add("pH probe reading unavailable or unverified.")
                null
            }

            val temperatureC: Double? = if (json.has("temperature_c") && !json.isNull("temperature_c")) {
                val t = json.optDouble("temperature_c", Double.NaN)
                // DS18B20 valid physical range -55..125°C; -127.0 indicates disconnected probe
                if (t.isFinite() && t != -127.0 && t in -20.0..85.0) {
                    kotlin.math.round(t * 10.0) / 10.0
                } else {
                    errors.add("Invalid or disconnected DS18B20 temperature reading ($t°C).")
                    null
                }
            } else {
                errors.add("DS18B20 temperature reading unavailable.")
                null
            }

            if (moisturePercent == null && ph == null && temperatureC == null && moistureRaw == null) {
                return Result.failure(
                    IllegalArgumentException(
                        errors.firstOrNull() ?: "Packet contained no valid sensor readings."
                    )
                )
            }

            val reading = SensorReading(
                moistureRaw = moistureRaw,
                moisturePercent = moisturePercent,
                ph = ph,
                temperatureC = temperatureC,
                timestampMs = receivedAtMs,
                isDemo = false,
                isAlertDemo = false,
                validationErrors = errors
            )
            Result.success(reading)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to parse sensor JSON: ${e.message}"))
        }
    }
}

class BleSoilManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _bleState = MutableStateFlow(
        BleState(
            status = if (!hasBleHardware()) {
                BleConnectionStatus.BLE_UNSUPPORTED
            } else {
                BleConnectionStatus.DISCONNECTED
            }
        )
    )
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    private var calibrationConfig = CalibrationConfig()
    private var activeGatt: BluetoothGatt? = null
    private var scanTimeoutJob: Job? = null
    private var lastDevice: BluetoothDevice? = null
    private var isScanning = false

    fun updateCalibration(config: CalibrationConfig) {
        calibrationConfig = config
        // Recompute calibrated moisture if a live raw reading is present
        val current = _bleState.value.lastReading
        if (current != null && !current.isDemo && current.moistureRaw != null) {
            val recalibrated = config.computeMoistureFromRaw(current.moistureRaw)
            _bleState.value = _bleState.value.copy(
                lastReading = current.copy(moisturePercent = recalibrated)
            )
        }
    }

    fun hasBleHardware(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
            bluetoothAdapter != null
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanAndConnect() {
        if (!hasBleHardware()) {
            _bleState.value = _bleState.value.copy(
                status = BleConnectionStatus.BLE_UNSUPPORTED,
                errorMessage = "This device does not support Bluetooth Low Energy. Use Demo Mode for exhibition testing."
            )
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _bleState.value = _bleState.value.copy(
                status = BleConnectionStatus.BLUETOOTH_DISABLED,
                errorMessage = "Bluetooth is turned off. Please enable Bluetooth and try again."
            )
            return
        }
        if (!hasRequiredPermissions()) {
            _bleState.value = _bleState.value.copy(
                status = BleConnectionStatus.PERMISSION_REQUIRED,
                errorMessage = "Bluetooth permission is required to scan for the ESP32 sensor node."
            )
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _bleState.value = _bleState.value.copy(
                status = BleConnectionStatus.ERROR,
                errorMessage = "BLE scanner is unavailable on this device."
            )
            return
        }

        stopScanningInternal()
        _bleState.value = _bleState.value.copy(
            status = BleConnectionStatus.SCANNING,
            errorMessage = null
        )

        val serviceUuid = runCatching { UUID.fromString(calibrationConfig.serviceUuid) }.getOrNull()
        val filters = if (serviceUuid != null) {
            listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(serviceUuid))
                    .build()
            )
        } else {
            emptyList()
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        isScanning = true
        try {
            // Scan with service filter first; fallback handled by user retry if needed
            scanner.startScan(filters, settings, scanCallback)
        } catch (e: SecurityException) {
            isScanning = false
            _bleState.value = _bleState.value.copy(
                status = BleConnectionStatus.PERMISSION_REQUIRED,
                errorMessage = "Bluetooth scan permission denied."
            )
            return
        } catch (e: Exception) {
            isScanning = false
            _bleState.value = _bleState.value.copy(
                status = BleConnectionStatus.ERROR,
                errorMessage = "Failed to start BLE scan: ${e.localizedMessage}"
            )
            return
        }

        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(12_000L)
            if (isScanning) {
                stopScanningInternal()
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.DISCONNECTED,
                    errorMessage = "No SMART SOIL ESP32 found nearby. Ensure the ESP32 is powered on and advertising BLE, or switch to Demo Mode."
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun reconnectLastDevice() {
        val device = lastDevice
        if (device != null && hasRequiredPermissions() && bluetoothAdapter?.isEnabled == true) {
            connectToDevice(device)
        } else {
            startScanAndConnect()
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScanningInternal()
        scanTimeoutJob?.cancel()
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing GATT", e)
        }
        activeGatt = null
        _bleState.value = BleState(
            status = if (hasBleHardware()) BleConnectionStatus.DISCONNECTED else BleConnectionStatus.BLE_UNSUPPORTED,
            connectedDeviceName = null,
            connectedDeviceAddress = null,
            lastReading = null,
            lastRawPacket = null,
            errorMessage = null
        )
    }

    @SuppressLint("MissingPermission")
    private fun stopScanningInternal() {
        if (!isScanning) return
        isScanning = false
        try {
            if (hasRequiredPermissions()) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping BLE scan", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            stopScanningInternal()
            scanTimeoutJob?.cancel()
            connectToDevice(device)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            scanTimeoutJob?.cancel()
            _bleState.value = _bleState.value.copy(
                status = BleConnectionStatus.ERROR,
                errorMessage = "BLE scan failed (code $errorCode). Please verify Bluetooth is enabled."
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (!hasRequiredPermissions()) return
        lastDevice = device
        val deviceName = runCatching { device.name }.getOrNull() ?: "ESP32 Soil Node"
        _bleState.value = _bleState.value.copy(
            status = BleConnectionStatus.CONNECTING,
            connectedDeviceName = deviceName,
            connectedDeviceAddress = device.address,
            errorMessage = null
        )
        try {
            activeGatt?.close()
            activeGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            _bleState.value = _bleState.value.copy(
                status = BleConnectionStatus.ERROR,
                errorMessage = "Failed to connect GATT: ${e.localizedMessage}"
            )
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_CONNECTED) {
                gatt.close()
                if (activeGatt == gatt) activeGatt = null
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.DISCONNECTED,
                    errorMessage = "ESP32 BLE connection lost (status $status). Tap Reconnect to try again."
                )
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.CONNECTED,
                    errorMessage = null
                )
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close()
                if (activeGatt == gatt) activeGatt = null
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.DISCONNECTED,
                    errorMessage = "Disconnected from ESP32."
                )
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.ERROR,
                    errorMessage = "Service discovery failed (status $status)."
                )
                return
            }

            val serviceUuid = runCatching { UUID.fromString(calibrationConfig.serviceUuid) }.getOrNull()
            val charUuid = runCatching { UUID.fromString(calibrationConfig.characteristicUuid) }.getOrNull()
            val service = serviceUuid?.let { gatt.getService(it) }
            val characteristic = charUuid?.let { service?.getCharacteristic(it) }

            if (characteristic == null) {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.ERROR,
                    errorMessage = "Soil sensor GATT characteristic not found on connected device."
                )
                return
            }

            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            }
            // Also trigger an initial read
            gatt.readCharacteristic(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handlePacketBytes(value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            characteristic.value?.let { handlePacketBytes(it) }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handlePacketBytes(value)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                characteristic.value?.let { handlePacketBytes(it) }
            }
        }
    }

    private fun handlePacketBytes(bytes: ByteArray) {
        val rawText = String(bytes, StandardCharsets.UTF_8)
        val parsed = SensorPayloadParser.parseAndValidate(rawText, calibrationConfig)
        parsed.onSuccess { reading ->
            _bleState.value = _bleState.value.copy(
                lastReading = reading,
                lastRawPacket = rawText,
                errorMessage = if (reading.validationErrors.isNotEmpty()) {
                    reading.validationErrors.joinToString(" ")
                } else {
                    null
                }
            )
        }.onFailure { err ->
            Log.w(TAG, "Rejected invalid BLE sensor packet: $rawText", err)
            _bleState.value = _bleState.value.copy(
                lastRawPacket = rawText,
                errorMessage = "Rejected invalid ESP32 packet: ${err.message}"
            )
        }
    }
}
