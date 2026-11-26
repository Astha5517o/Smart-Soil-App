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
            // The currently working ESP32 sketch transmits a single raw ADC integer,
            // for example "2380", rather than a JSON object.
            if (trimmed.matches(Regex("\\d{1,4}"))) {
                val raw = trimmed.toInt()
                if (raw !in 0..4095) {
                    return Result.failure(IllegalArgumentException("Invalid raw ADC packet (expected 0..4095)."))
                }
                return Result.success(
                    SensorReading(
                        moistureRaw = raw,
                        moisturePercent = calibration.computeMoistureFromRaw(raw),
                        ph = null,
                        temperatureC = null,
                        timestampMs = receivedAtMs,
                        isDemo = false,
                        isAlertDemo = false,
                        validationErrors = listOf(
                            "This ESP32 firmware sends moisture only. pH and temperature are not included in its BLE packet."
                        )
                    )
                )
            }
            if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
                return Result.failure(IllegalArgumentException("Unsupported sensor packet. Expected a raw ADC integer or JSON object."))
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

        // Scan without a hardware filter, then validate each advertisement in scanCallback.
        // Some Android BLE stacks miss devices when a strict 128-bit service filter is used.
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        isScanning = true
        try {
            scanner.startScan(null, settings, scanCallback)
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
            val serviceUuid = runCatching { UUID.fromString(calibrationConfig.serviceUuid) }.getOrNull()
            val advertisedServices = result.scanRecord?.serviceUuids.orEmpty()
            val advertisedName = result.scanRecord?.deviceName
                ?: runCatching { result.device.name }.getOrNull()
            val hasExpectedService = serviceUuid != null &&
                advertisedServices.any { it.uuid == serviceUuid }
            val hasExpectedName = advertisedName?.startsWith("SMART_SOIL", ignoreCase = true) == true

            // Ignore unrelated nearby BLE devices; never connect to the first random device.
            if (!hasExpectedService && !hasExpectedName) return

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
                errorMessage = "BLE scan failed (code $errorCode). Check Bluetooth permissions and try again. Scan error: $errorCode"
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
            if (gatt !== activeGatt) {
                gatt.close()
                return
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.close()
                activeGatt = null
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.ERROR,
                    errorMessage = "ESP32 connection failed (GATT status $status). Turn the ESP32 off/on and reconnect."
                )
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.CONNECTING,
                    errorMessage = "Connected to ESP32. Discovering sensor service..."
                )
                if (!gatt.discoverServices()) {
                    _bleState.value = _bleState.value.copy(
                        status = BleConnectionStatus.ERROR,
                        errorMessage = "Could not start GATT service discovery. Tap Reconnect."
                    )
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close()
                if (activeGatt == gatt) activeGatt = null
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.DISCONNECTED,
                    lastReading = null,
                    errorMessage = "ESP32 disconnected. Check its power and reconnect."
                )
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== activeGatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.ERROR,
                    errorMessage = "Service discovery failed (GATT status $status)."
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
                    errorMessage = "ESP32 connected, but the configured sensor service/characteristic was not found. Check both UUIDs."
                )
                return
            }

            val supportsNotify = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
            val supportsIndicate = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
            val supportsRead = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
            if (!supportsNotify && !supportsIndicate && !supportsRead) {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.ERROR,
                    errorMessage = "Sensor characteristic supports neither notifications nor reads."
                )
                return
            }

            if (supportsNotify || supportsIndicate) {
                val localEnabled = gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(CCCD_UUID)
                if (!localEnabled || descriptor == null) {
                    if (supportsRead) {
                        _bleState.value = _bleState.value.copy(
                            status = BleConnectionStatus.CONNECTING,
                            errorMessage = "Notifications unavailable; using live characteristic reads."
                        )
                        gatt.readCharacteristic(characteristic)
                    } else {
                        _bleState.value = _bleState.value.copy(
                            status = BleConnectionStatus.ERROR,
                            errorMessage = "Could not enable BLE notifications (CCCD descriptor missing)."
                        )
                    }
                    return
                }

                val enableValue = if (supportsNotify) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                }
                val writeStarted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, enableValue) == BluetoothGatt.GATT_SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = enableValue
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
                if (!writeStarted) {
                    if (supportsRead) {
                        _bleState.value = _bleState.value.copy(
                            status = BleConnectionStatus.CONNECTING,
                            errorMessage = "Notification setup failed; trying characteristic reads."
                        )
                        gatt.readCharacteristic(characteristic)
                    } else {
                        _bleState.value = _bleState.value.copy(
                            status = BleConnectionStatus.ERROR,
                            errorMessage = "Could not subscribe to ESP32 sensor notifications."
                        )
                    }
                }
                // Do not start a read until the descriptor write callback; Android GATT
                // permits one outstanding operation at a time.
                return
            }

            if (supportsRead) {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.CONNECTING,
                    errorMessage = "Notifications not supported; using characteristic reads."
                )
                gatt.readCharacteristic(characteristic)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (gatt !== activeGatt || descriptor.uuid != CCCD_UUID) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.CONNECTED,
                    errorMessage = null
                )
                val service = gatt.getService(UUID.fromString(calibrationConfig.serviceUuid))
                val characteristic = service?.getCharacteristic(UUID.fromString(calibrationConfig.characteristicUuid))
                if (characteristic != null &&
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                ) {
                    gatt.readCharacteristic(characteristic)
                }
            } else {
                val service = gatt.getService(UUID.fromString(calibrationConfig.serviceUuid))
                val characteristic = service?.getCharacteristic(UUID.fromString(calibrationConfig.characteristicUuid))
                val supportsRead = characteristic != null &&
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
                if (supportsRead && characteristic != null) {
                    _bleState.value = _bleState.value.copy(
                        status = BleConnectionStatus.CONNECTING,
                        errorMessage = "Notification subscription failed; falling back to reads."
                    )
                    gatt.readCharacteristic(characteristic)
                } else {
                    _bleState.value = _bleState.value.copy(
                        status = BleConnectionStatus.ERROR,
                        errorMessage = "ESP32 notification subscription failed (GATT status $status)."
                    )
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (gatt === activeGatt) handlePacketBytes(value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (gatt !== activeGatt) return
            @Suppress("DEPRECATION")
            characteristic.value?.let { handlePacketBytes(it) }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (gatt !== activeGatt) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handlePacketBytes(value)
                if (_bleState.value.status == BleConnectionStatus.CONNECTING) {
                    _bleState.value = _bleState.value.copy(status = BleConnectionStatus.CONNECTED, errorMessage = null)
                }
            } else {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.ERROR,
                    errorMessage = "Could not read the ESP32 sensor characteristic (GATT status $status)."
                )
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (gatt !== activeGatt) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                characteristic.value?.let { handlePacketBytes(it) }
                if (_bleState.value.status == BleConnectionStatus.CONNECTING) {
                    _bleState.value = _bleState.value.copy(status = BleConnectionStatus.CONNECTED, errorMessage = null)
                }
            } else {
                _bleState.value = _bleState.value.copy(
                    status = BleConnectionStatus.ERROR,
                    errorMessage = "Could not read the ESP32 sensor characteristic (GATT status $status)."
                )
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
