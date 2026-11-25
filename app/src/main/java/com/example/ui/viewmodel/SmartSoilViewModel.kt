package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import com.example.data.ai.GeminiSoilService
import com.example.data.ble.BleSoilManager
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.SoilRecordEntity
import com.example.data.local.SoilRepository
import com.example.data.local.UserPreferencesState
import com.example.data.model.AiAdvisoryPlan
import com.example.data.model.AiRecommendationItem
import com.example.data.model.BleConnectionStatus
import com.example.data.model.BleState
import com.example.data.model.CropIdResult
import com.example.data.model.NpkLabExtractionResult
import com.example.data.model.NpkRegionalReferenceResult
import com.example.data.model.PlantHealthResult
import com.example.data.model.RecordCategory
import com.example.data.model.RecordOrigin
import com.example.data.model.SensorDisplayMode
import com.example.data.model.SensorReading
import com.example.data.model.SoilScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AsyncUiState<out T> {
    data object Idle : AsyncUiState<Nothing>
    data object Loading : AsyncUiState<Nothing>
    data class Success<T>(val data: T) : AsyncUiState<T>
    data class Error(val message: String) : AsyncUiState<Nothing>
}

class SmartSoilViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = SoilRepository(database.soilRecordDao())
    private val settingsStore = SettingsDataStore(application)
    val bleManager = BleSoilManager(application)
    private val aiService = GeminiSoilService(application)

    val preferences: StateFlow<UserPreferencesState> = settingsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesState())

    val bleState: StateFlow<BleState> = bleManager.bleState

    val allRecords: StateFlow<List<SoilRecordEntity>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _displayMode = MutableStateFlow(SensorDisplayMode.DEMO_NORMAL)
    val displayMode: StateFlow<SensorDisplayMode> = _displayMode.asStateFlow()

    val activeReading: StateFlow<SensorReading?> = combine(
        _displayMode,
        bleState
    ) { mode, ble ->
        when (mode) {
            SensorDisplayMode.LIVE_BLE -> {
                if (ble.status == BleConnectionStatus.CONNECTED) ble.lastReading else null
            }
            SensorDisplayMode.DEMO_NORMAL -> SensorReading(
                moistureRaw = 2408,
                moisturePercent = 44,
                ph = 6.4,
                temperatureC = 26.8,
                timestampMs = System.currentTimeMillis(),
                isDemo = true,
                isAlertDemo = false
            )
            SensorDisplayMode.DEMO_ALERT -> SensorReading(
                moistureRaw = 2768,
                moisturePercent = 24,
                ph = 7.9,
                temperatureC = 31.4,
                timestampMs = System.currentTimeMillis(),
                isDemo = true,
                isAlertDemo = true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Scan UI states
    private val _soilScanState = MutableStateFlow<AsyncUiState<SoilScanResult>>(AsyncUiState.Idle)
    val soilScanState: StateFlow<AsyncUiState<SoilScanResult>> = _soilScanState.asStateFlow()

    private val _cropIdState = MutableStateFlow<AsyncUiState<CropIdResult>>(AsyncUiState.Idle)
    val cropIdState: StateFlow<AsyncUiState<CropIdResult>> = _cropIdState.asStateFlow()

    private val _plantHealthState = MutableStateFlow<AsyncUiState<PlantHealthResult>>(AsyncUiState.Idle)
    val plantHealthState: StateFlow<AsyncUiState<PlantHealthResult>> = _plantHealthState.asStateFlow()

    private val _npkLabState = MutableStateFlow<AsyncUiState<NpkLabExtractionResult>>(AsyncUiState.Idle)
    val npkLabState: StateFlow<AsyncUiState<NpkLabExtractionResult>> = _npkLabState.asStateFlow()

    private val _npkRegionalState = MutableStateFlow<AsyncUiState<NpkRegionalReferenceResult>>(AsyncUiState.Idle)
    val npkRegionalState: StateFlow<AsyncUiState<NpkRegionalReferenceResult>> = _npkRegionalState.asStateFlow()

    private val _advisorState = MutableStateFlow<AsyncUiState<AiAdvisoryPlan>>(AsyncUiState.Idle)
    val advisorState: StateFlow<AsyncUiState<AiAdvisoryPlan>> = _advisorState.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.collectLatest { prefs ->
                bleManager.updateCalibration(prefs.calibration)
            }
        }
        viewModelScope.launch {
            bleState.collectLatest { ble ->
                if (ble.status == BleConnectionStatus.CONNECTED && ble.lastReading != null) {
                    _displayMode.value = SensorDisplayMode.LIVE_BLE
                }
            }
        }
    }

    fun clearToast() {
        _statusToast.value = null
    }

    fun setDisplayMode(mode: SensorDisplayMode) {
        _displayMode.value = mode
    }

    fun updateCropAndStage(cropName: String, stage: String) {
        viewModelScope.launch {
            settingsStore.updateCrop(cropName, stage)
            _statusToast.value = "Active crop updated to $cropName ($stage)"
        }
    }

    fun updateThresholds(minM: Int, maxM: Int, minPh: Double, maxPh: Double) {
        viewModelScope.launch {
            settingsStore.updateThresholds(minM, maxM, minPh, maxPh)
            _statusToast.value = "Crop thresholds saved."
        }
    }

    fun updateCalibration(dryAdc: Int, wetAdc: Int) {
        viewModelScope.launch {
            settingsStore.updateCalibration(dryAdc, wetAdc)
            _statusToast.value = "HW-390 moisture calibration saved (Dry=$dryAdc, Wet=$wetAdc)."
        }
    }

    fun saveCurrentSensorSnapshot() {
        val reading = activeReading.value
        val crop = preferences.value.selectedCropName
        if (reading == null) {
            _statusToast.value = "No valid sensor reading available to save."
            return
        }
        viewModelScope.launch {
            val isDemo = reading.isDemo
            val mText = reading.moisturePercent?.let { "$it%" } ?: "Unavailable"
            val phText = reading.ph?.toString() ?: "Unavailable"
            val tText = reading.temperatureC?.let { "$it°C" } ?: "Unavailable"
            repository.saveRecord(
                SoilRecordEntity(
                    category = RecordCategory.SENSOR_READING.name,
                    originBadge = if (isDemo) RecordOrigin.DEMO_DATA.name else RecordOrigin.LIVE_SENSOR.name,
                    isDemo = isDemo,
                    dataSource = if (isDemo) "Demo Mode Snapshot" else "ESP32 BLE (${bleState.value.connectedDeviceName ?: "Node"})",
                    title = "${if (isDemo) "Demo" else "Live"} Sensor Reading — $crop",
                    summary = "Moisture: $mText • pH: $phText • Temp: $tText",
                    fullDetails = buildString {
                        appendLine(if (isDemo) "SOURCE: DEMO DATA (Illustrative Example)" else "SOURCE: LIVE ESP32 BLE")
                        appendLine("Crop: $crop")
                        appendLine("Moisture: $mText (Raw ADC: ${reading.moistureRaw ?: "N/A"})")
                        appendLine("Soil pH: $phText")
                        appendLine("Temperature: $tText")
                        if (reading.validationErrors.isNotEmpty()) {
                            appendLine("Warnings: ${reading.validationErrors.joinToString("; ")}")
                        }
                    }
                )
            )
            _statusToast.value = "Sensor snapshot saved to Reports."
        }
    }

    // --- A. Soil Scan ---
    fun analyzeSoilPhoto(uri: Uri) {
        viewModelScope.launch {
            _soilScanState.value = AsyncUiState.Loading
            aiService.analyzeSoilImage(uri).onSuccess { result ->
                _soilScanState.value = AsyncUiState.Success(result)
            }.onFailure { err ->
                _soilScanState.value = AsyncUiState.Error(err.message ?: "Soil scan failed.")
            }
        }
    }

    fun saveSoilScanResult(result: SoilScanResult, imageUri: Uri?) {
        viewModelScope.launch {
            val savedImageUri = persistImageToAppStorage(imageUri)
            repository.saveRecord(
                SoilRecordEntity(
                    category = RecordCategory.SOIL_SCAN.name,
                    originBadge = if (result.isDemo) RecordOrigin.DEMO_DATA.name else RecordOrigin.AI_INTERPRETATION.name,
                    isDemo = result.isDemo,
                    dataSource = if (result.isDemo) "Demo Soil Scan Sample" else "FarmerBuddy Vision Soil Scan",
                    title = "${if (result.isDemo) "Demo " else ""}Soil Scan — ${result.possibleSoilType}",
                    summary = result.visibleCharacteristics,
                    fullDetails = buildString {
                        appendLine("Visible characteristics: ${result.visibleCharacteristics}")
                        appendLine("Possible soil type: ${result.possibleSoilType}")
                        appendLine("Organic matter indicators: ${result.organicMatterIndicators}")
                        appendLine("Uncertainty: ${result.uncertaintyExplanation}")
                        appendLine("Recommended additional tests: ${result.recommendedAdditionalTests.joinToString("; ")}")
                    },
                    imageUri = savedImageUri
                )
            )
            _statusToast.value = "Soil scan saved to Reports."
        }
    }

    fun loadDemoSoilScan() {
        val demo = SoilScanResult(
            visibleCharacteristics = "Dark brownish-gray crumbly aggregates with visible fine root fibers and slight surface crusting.",
            possibleSoilType = "Alluvial Clay Loam (visual estimate only)",
            organicMatterIndicators = "Moderate organic residue visible in upper topsoil crumbs.",
            uncertaintyExplanation = "DEMO RESULT: Photographs cannot measure soil pH, volumetric moisture %, or NPK nutrients. Subsurface clay/sand ratios require a jar settling test or lab particle analysis.",
            recommendedAdditionalTests = listOf(
                "Measure root-zone moisture with calibrated HW-390 ESP32 probe",
                "Run jar sedimentation test for sand/silt/clay fractions",
                "Submit composite topsoil sample for Soil Health Card NPK test"
            ),
            isDemo = true
        )
        _soilScanState.value = AsyncUiState.Success(demo)
    }

    fun resetSoilScan() {
        _soilScanState.value = AsyncUiState.Idle
    }

    // --- B. Crop Identification ---
    fun identifyCropPhoto(uri: Uri) {
        viewModelScope.launch {
            _cropIdState.value = AsyncUiState.Loading
            aiService.identifyCropImage(uri).onSuccess { result ->
                _cropIdState.value = AsyncUiState.Success(result)
            }.onFailure { err ->
                _cropIdState.value = AsyncUiState.Error(err.message ?: "Crop identification failed.")
            }
        }
    }

    fun loadDemoCropId() {
        _cropIdState.value = AsyncUiState.Success(
            CropIdResult(
                possibleCropName = "Paddy (Rice)",
                confidenceLevel = "Moderate",
                identifyingFeatures = "Narrow upright grass-like leaf blades with prominent midrib and tillering cluster at the base.",
                uncertaintyNotes = "DEMO RESULT: Young cereal grasses (rice, wheat, and sedges) can look similar before panicle emergence. Please confirm manually.",
                suggestedGrowthStage = "Vegetative",
                isDemo = true
            )
        )
    }

    fun confirmAndSaveCrop(
        confirmedCropName: String,
        confirmedStage: String,
        result: CropIdResult,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            val savedImageUri = persistImageToAppStorage(imageUri)
            settingsStore.updateCrop(confirmedCropName, confirmedStage)
            repository.saveRecord(
                SoilRecordEntity(
                    category = RecordCategory.CROP_SCAN.name,
                    originBadge = if (result.isDemo) RecordOrigin.DEMO_DATA.name else RecordOrigin.AI_INTERPRETATION.name,
                    isDemo = result.isDemo,
                    dataSource = if (result.isDemo) "Demo Crop Identification" else "FarmerBuddy Vision Crop Scan (Confirmed)",
                    title = "Confirmed Crop: $confirmedCropName ($confirmedStage)",
                    summary = "AI suggestion: ${result.possibleCropName} (${result.confidenceLevel} confidence)",
                    fullDetails = buildString {
                        appendLine("Confirmed Crop: $confirmedCropName")
                        appendLine("Growth Stage: $confirmedStage")
                        appendLine("Identifying features: ${result.identifyingFeatures}")
                        appendLine("Uncertainty notes: ${result.uncertaintyNotes}")
                    },
                    imageUri = savedImageUri
                )
            )
            _statusToast.value = "Confirmed & saved active crop: $confirmedCropName"
        }
    }

    fun resetCropId() {
        _cropIdState.value = AsyncUiState.Idle
    }

    // --- C. Plant Health Scan ---
    fun analyzePlantHealthPhoto(uri: Uri) {
        val crop = preferences.value.selectedCropName
        viewModelScope.launch {
            _plantHealthState.value = AsyncUiState.Loading
            aiService.analyzePlantHealthImage(uri, crop).onSuccess { result ->
                _plantHealthState.value = AsyncUiState.Success(result)
            }.onFailure { err ->
                _plantHealthState.value = AsyncUiState.Error(err.message ?: "Plant health scan failed.")
            }
        }
    }

    fun savePlantHealthResult(result: PlantHealthResult, imageUri: Uri?) {
        val crop = preferences.value.selectedCropName
        viewModelScope.launch {
            val savedImageUri = persistImageToAppStorage(imageUri)
            repository.saveRecord(
                SoilRecordEntity(
                    category = RecordCategory.PLANT_HEALTH_SCAN.name,
                    originBadge = if (result.isDemo) RecordOrigin.DEMO_DATA.name else RecordOrigin.AI_INTERPRETATION.name,
                    isDemo = result.isDemo,
                    dataSource = if (result.isDemo) "Demo Plant Health Sample" else "FarmerBuddy Vision Plant Check",
                    title = "${if (result.isDemo) "Demo " else ""}Plant Health Check — $crop",
                    summary = result.visibleSymptoms,
                    fullDetails = buildString {
                        appendLine("Visible symptoms: ${result.visibleSymptoms}")
                        appendLine("Possible explanations: ${result.possibleExplanations.joinToString("; ")}")
                        appendLine("Suggested field checks: ${result.suggestedChecks.joinToString("; ")}")
                        appendLine("Cautious next steps: ${result.cautiousNextSteps.joinToString("; ")}")
                        appendLine("Limitations: ${result.diagnosisLimitations}")
                    },
                    imageUri = savedImageUri
                )
            )
            _statusToast.value = "Plant health check saved to Reports."
        }
    }

    fun loadDemoPlantHealth() {
        val demo = PlantHealthResult(
            visibleSymptoms = "Mild yellowing along lower leaf margins with scattered small tan spots on older leaves.",
            possibleExplanations = listOf(
                "Early nitrogen or potassium mobility stress in older leaves",
                "Temporary root-zone moisture stress or waterlogging",
                "Early foliar fungal spotting (cannot be confirmed without lab microscopy)"
            ),
            suggestedChecks = listOf(
                "Inspect leaf undersides for mites, aphids, or stem borer damage",
                "Compare ESP32 soil moisture and pH against crop thresholds",
                "Check whether symptoms appear across the whole plot or in low-lying patches"
            ),
            cautiousNextSteps = listOf(
                "Remove heavily senesced lower leaves for airflow if fungal spots spread",
                "Verify soil nutrient status via Soil Health Card before applying fertilizer",
                "Consult a local Krishi Vigyan Kendra (KVK) or agronomist if spread accelerates"
            ),
            diagnosisLimitations = "DEMO RESULT: Image-based inspection cannot confirm pathogens, viral infections, or exact nutrient deficiencies.",
            isDemo = true
        )
        _plantHealthState.value = AsyncUiState.Success(demo)
    }

    fun resetPlantHealth() {
        _plantHealthState.value = AsyncUiState.Idle
    }

    // --- D. NPK Report (Method 1 & Method 2) ---
    fun extractNpkLabReport(uri: Uri) {
        viewModelScope.launch {
            _npkLabState.value = AsyncUiState.Loading
            aiService.extractNpkFromReportImage(uri).onSuccess { result ->
                _npkLabState.value = AsyncUiState.Success(result)
            }.onFailure { err ->
                _npkLabState.value = AsyncUiState.Error(err.message ?: "Failed to extract lab report.")
            }
        }
    }

    fun resetNpkLab() {
        _npkLabState.value = AsyncUiState.Idle
    }

    fun loadDemoLabReport() {
        _npkLabState.value = AsyncUiState.Success(
            NpkLabExtractionResult(
                nitrogen = "245",
                phosphorus = "18.5",
                potassium = "190",
                unit = "kg/ha",
                labSource = "Illustrative Sample Soil Health Card",
                reportDate = "2026-10-15",
                extractionNotes = "DEMO SAMPLE: Verify every extracted number against your printed laboratory card before saving.",
                isUnreadable = false,
                isDemo = true
            )
        )
    }

    fun saveVerifiedLabReport(
        nitrogen: String,
        phosphorus: String,
        potassium: String,
        unit: String,
        labSource: String,
        reportDate: String,
        isDemo: Boolean,
        originalImageUri: Uri?
    ) {
        viewModelScope.launch {
            val savedImageUri = persistImageToAppStorage(originalImageUri)
            repository.saveRecord(
                SoilRecordEntity(
                    category = RecordCategory.NPK_REPORT.name,
                    originBadge = if (isDemo) RecordOrigin.DEMO_DATA.name else RecordOrigin.LAB_REPORT.name,
                    isDemo = isDemo,
                    dataSource = labSource.ifBlank { "Uploaded Laboratory Report" },
                    title = "${if (isDemo) "Demo " else ""}Soil Lab NPK Report",
                    summary = "N: $nitrogen • P: $phosphorus • K: $potassium ($unit)",
                    fullDetails = buildString {
                        appendLine(if (isDemo) "DEMO SAMPLE LAB REPORT (Illustrative)" else "VERIFIED UPLOADED LABORATORY REPORT")
                        appendLine("Lab / Source: $labSource")
                        appendLine("Report Date: $reportDate")
                        appendLine("Available Nitrogen (N): $nitrogen $unit")
                        appendLine("Available Phosphorus (P): $phosphorus $unit")
                        appendLine("Available Potassium (K): $potassium $unit")
                        if (savedImageUri != null) {
                            appendLine("Original Uploaded Image Preserved: $savedImageUri")
                        }
                    },
                    imageUri = savedImageUri
                )
            )
            _statusToast.value = "Lab NPK report saved to Reports."
        }
    }

    fun fetchRegionalNpkReference(crop: String, region: String, soilType: String) {
        if (crop.isBlank() || region.isBlank()) {
            _npkRegionalState.value = AsyncUiState.Error("Please enter both Crop and State/District/Region.")
            return
        }
        viewModelScope.launch {
            _npkRegionalState.value = AsyncUiState.Loading
            aiService.fetchRegionalNpkReference(crop.trim(), region.trim(), soilType.trim()).onSuccess { res ->
                _npkRegionalState.value = AsyncUiState.Success(res)
            }.onFailure { err ->
                _npkRegionalState.value = AsyncUiState.Error(err.message ?: "Could not fetch regional reference.")
            }
        }
    }

    fun saveRegionalNpkReference(res: NpkRegionalReferenceResult) {
        viewModelScope.launch {
            repository.saveRecord(
                SoilRecordEntity(
                    category = RecordCategory.NPK_REPORT.name,
                    originBadge = RecordOrigin.REFERENCE_ESTIMATE.name,
                    isDemo = false,
                    dataSource = res.source,
                    title = "Regional NPK Reference — ${res.crop} (${res.region})",
                    summary = "REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST. ${res.sufficiencyStatus}",
                    fullDetails = buildString {
                        appendLine("REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST.")
                        appendLine("Crop: ${res.crop}")
                        appendLine("Region: ${res.region}")
                        appendLine("Soil Type: ${res.soilType}")
                        appendLine("Source: ${res.source}")
                        appendLine("Reference Date: ${res.referenceDate}")
                        appendLine("Overview: ${res.nutrientOverview}")
                        appendLine("Limitations: ${res.limitations}")
                    }
                )
            )
            _statusToast.value = "Regional NPK guide saved to Reports."
        }
    }

    private suspend fun persistImageToAppStorage(sourceUri: Uri?): String? {
        if (sourceUri == null) return null
        return withContext(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val dir = File(app.filesDir, "saved_scans").apply { mkdirs() }
                val dest = File(dir, "scan_${UUID.randomUUID()}.jpg")

                val orientation = app.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                app.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

                var sampleSize = 1
                while (bounds.outWidth / sampleSize > 1600 || bounds.outHeight / sampleSize > 1600) {
                    sampleSize *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = app.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                } ?: return@withContext null

                val degrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                val scale = minOf(900f / decoded.width, 900f / decoded.height, 1f)
                val finalBitmap = if (scale < 1f || degrees != 0f) {
                    val m = Matrix().apply {
                        if (scale < 1f) postScale(scale, scale)
                        if (degrees != 0f) postRotate(degrees)
                    }
                    Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true).also {
                        if (it != decoded) decoded.recycle()
                    }
                } else decoded

                FileOutputStream(dest).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
                }
                finalBitmap.recycle()
                Uri.fromFile(dest).toString()
            } catch (e: Exception) {
                Log.w("SmartSoilViewModel", "Could not persist scan image copy: ${e.message}")
                null
            }
        }
    }

    // --- AI Advisor ---
    fun buildAdvisorContextSnapshot(): String {
        val prefs = preferences.value
        val reading = activeReading.value
        val records = allRecords.value
        val latestSoil = records.firstOrNull { it.category == RecordCategory.SOIL_SCAN.name }
        val latestPlant = records.firstOrNull { it.category == RecordCategory.PLANT_HEALTH_SCAN.name }
        val latestLabNpk = records.firstOrNull {
            it.category == RecordCategory.NPK_REPORT.name && it.originBadge == RecordOrigin.LAB_REPORT.name
        }
        val latestRegNpk = records.firstOrNull {
            it.category == RecordCategory.NPK_REPORT.name && it.originBadge == RecordOrigin.REFERENCE_ESTIMATE.name
        }

        return buildString {
            appendLine("Crop: ${prefs.selectedCropName} (Growth Stage: ${prefs.growthStage})")
            appendLine("Configured Moisture Target: ${prefs.customMinMoisture}%-${prefs.customMaxMoisture}%, Preferred pH: ${prefs.customMinPh}-${prefs.customMaxPh}")
            if (reading != null) {
                val tag = if (reading.isDemo) "DEMO DATA (Illustrative only — NOT live reading)" else "LIVE ESP32 BLE (Verified)"
                appendLine("Sensor Source: $tag")
                appendLine("Moisture: ${reading.moisturePercent?.let { "$it%" } ?: "Unavailable"} (Raw ADC: ${reading.moistureRaw ?: "N/A"})")
                appendLine("Soil pH: ${reading.ph ?: "Unavailable"}")
                appendLine("Soil Temperature: ${reading.temperatureC?.let { "$it°C" } ?: "Unavailable"}")
            } else {
                appendLine("Sensor Readings: Not connected / Unavailable")
            }
            if (latestSoil != null) {
                appendLine("Latest Soil Scan (${latestSoil.originBadge}): ${latestSoil.title} — ${latestSoil.summary}")
            } else {
                appendLine("Latest Soil Scan: None recorded yet")
            }
            if (latestPlant != null) {
                appendLine("Latest Plant Health (${latestPlant.originBadge}): ${latestPlant.title} — ${latestPlant.summary}")
            }
            if (latestLabNpk != null) {
                appendLine("Verified Lab NPK Report: ${latestLabNpk.title} — ${latestLabNpk.summary}")
            } else {
                appendLine("Verified Lab NPK Report: None uploaded yet")
            }
            if (latestRegNpk != null) {
                appendLine("Regional Reference Estimate (NOT A LAB TEST): ${latestRegNpk.title} — ${latestRegNpk.summary}")
            }
        }
    }

    fun requestAiAdvice(topic: String, question: String) {
        val snapshot = buildAdvisorContextSnapshot()
        viewModelScope.launch {
            _advisorState.value = AsyncUiState.Loading
            aiService.generateAdvisoryPlan(topic, question, snapshot).onSuccess { plan ->
                _advisorState.value = AsyncUiState.Success(plan)
            }.onFailure { err ->
                _advisorState.value = AsyncUiState.Error(err.message ?: "Failed to generate AI advice.")
            }
        }
    }

    fun loadDemoAdvice() {
        val snapshot = buildAdvisorContextSnapshot()
        val demoPlan = AiAdvisoryPlan(
            summary = "DEMO ADVISORY: Guidance based on current display values for ${preferences.value.selectedCropName}. Verify with a live sensor reading and Soil Health Card before making field changes.",
            recommendations = listOf(
                AiRecommendationItem(
                    category = "Irrigation & Moisture",
                    identifiedIssue = "Evaluating capacitive moisture threshold against crop target (${preferences.value.customMinMoisture}-${preferences.value.customMaxMoisture}%).",
                    whyItMatters = "Root-zone moisture directly governs nutrient uptake and oxygen availability during ${preferences.value.growthStage}.",
                    suggestedNextStep = "Measure at 2-3 spots in the plot using the calibrated HW-390 sensor before scheduling irrigation; apply organic mulch to reduce surface evaporation.",
                    supportingEvidence = "Calibrated capacitive moisture percentage and DS18B20 temperature probe.",
                    limitationsAndUncertainty = "DEMO OUTPUT: HW-390 readings depend on soil compaction and dry/wet calibration."
                ),
                AiRecommendationItem(
                    category = "pH & Organic Soil Improvement",
                    identifiedIssue = "Preventing premature amendment from a single pH probe check.",
                    whyItMatters = "Soil buffering capacity varies by clay and organic matter content; single probe readings can drift if uncalibrated.",
                    suggestedNextStep = "Incorporate well-decomposed compost or farmyard manure between crop cycles to buffer pH naturally, and confirm with a lab buffer pH test if readings stay outside ${preferences.value.customMinPh}-${preferences.value.customMaxPh}.",
                    supportingEvidence = "Standard agronomy soil buffering principles and organic matter management.",
                    limitationsAndUncertainty = "DEMO OUTPUT: Exact amendment quantities cannot be prescribed without a laboratory Soil Health Card."
                )
            ),
            contextSnapshot = snapshot,
            isDemo = true
        )
        _advisorState.value = AsyncUiState.Success(demoPlan)
    }

    fun saveAdvisoryPlanToReports(plan: AiAdvisoryPlan) {
        viewModelScope.launch {
            repository.saveRecord(
                SoilRecordEntity(
                    category = RecordCategory.AI_RECOMMENDATION.name,
                    originBadge = if (plan.isDemo) RecordOrigin.DEMO_DATA.name else RecordOrigin.AI_INTERPRETATION.name,
                    isDemo = plan.isDemo,
                    dataSource = if (plan.isDemo) "Demo AI Advisory Sample" else "Gemini Soil Advisor",
                    title = "${if (plan.isDemo) "Demo " else ""}AI Soil Plan — ${preferences.value.selectedCropName}",
                    summary = plan.summary,
                    fullDetails = buildString {
                        appendLine(plan.summary)
                        appendLine()
                        appendLine("--- Context Snapshot ---")
                        appendLine(plan.contextSnapshot)
                        plan.recommendations.forEachIndexed { i, rec ->
                            appendLine()
                            appendLine("${i + 1}. ${rec.category}: ${rec.identifiedIssue}")
                            appendLine("   Why it matters: ${rec.whyItMatters}")
                            appendLine("   Next step: ${rec.suggestedNextStep}")
                            appendLine("   Evidence: ${rec.supportingEvidence}")
                            appendLine("   Limitations: ${rec.limitationsAndUncertainty}")
                        }
                    }
                )
            )
            _statusToast.value = "AI advice saved to Reports."
        }
    }

    // --- Reports CRUD ---
    fun deleteRecord(id: String) {
        viewModelScope.launch {
            repository.deleteRecord(id)
            _statusToast.value = "Report deleted."
        }
    }

    fun clearDemoRecords() {
        viewModelScope.launch {
            repository.clearDemoRecords()
            _statusToast.value = "Demo records cleared."
        }
    }

    fun seedDemoRecords() {
        viewModelScope.launch {
            repository.seedDemoSampleRecords()
            _statusToast.value = "Loaded illustrative demo reports."
        }
    }

    fun resetAllProjectData() {
        viewModelScope.launch {
            repository.resetAllRecords()
            settingsStore.resetAll()
            _soilScanState.value = AsyncUiState.Idle
            _cropIdState.value = AsyncUiState.Idle
            _plantHealthState.value = AsyncUiState.Idle
            _npkLabState.value = AsyncUiState.Idle
            _npkRegionalState.value = AsyncUiState.Idle
            _advisorState.value = AsyncUiState.Idle
            _statusToast.value = "All project reports and calibration settings reset."
        }
    }
}
