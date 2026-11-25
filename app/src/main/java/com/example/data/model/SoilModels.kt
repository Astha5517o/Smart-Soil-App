package com.example.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class CropProfile(
    val id: String,
    val name: String,
    val minMoisturePct: Int,
    val maxMoisturePct: Int,
    val minPh: Double,
    val maxPh: Double,
    val optimalTempMinC: Double,
    val optimalTempMaxC: Double
)

object CropCatalog {
    val presets: List<CropProfile> = listOf(
        CropProfile(
            id = "paddy",
            name = "Paddy (Rice)",
            minMoisturePct = 38,
            maxMoisturePct = 55,
            minPh = 5.5,
            maxPh = 7.0,
            optimalTempMinC = 20.0,
            optimalTempMaxC = 32.0
        ),
        CropProfile(
            id = "wheat",
            name = "Wheat",
            minMoisturePct = 28,
            maxMoisturePct = 45,
            minPh = 6.0,
            maxPh = 7.5,
            optimalTempMinC = 15.0,
            optimalTempMaxC = 25.0
        ),
        CropProfile(
            id = "maize",
            name = "Maize (Corn)",
            minMoisturePct = 30,
            maxMoisturePct = 50,
            minPh = 5.8,
            maxPh = 7.0,
            optimalTempMinC = 18.0,
            optimalTempMaxC = 30.0
        ),
        CropProfile(
            id = "tomato",
            name = "Tomato",
            minMoisturePct = 35,
            maxMoisturePct = 55,
            minPh = 6.0,
            maxPh = 6.8,
            optimalTempMinC = 18.0,
            optimalTempMaxC = 29.0
        ),
        CropProfile(
            id = "cotton",
            name = "Cotton",
            minMoisturePct = 25,
            maxMoisturePct = 45,
            minPh = 6.0,
            maxPh = 8.0,
            optimalTempMinC = 20.0,
            optimalTempMaxC = 32.0
        ),
        CropProfile(
            id = "pulses",
            name = "Pulses / Chickpea",
            minMoisturePct = 22,
            maxMoisturePct = 40,
            minPh = 6.0,
            maxPh = 7.5,
            optimalTempMinC = 16.0,
            optimalTempMaxC = 28.0
        )
    )

    val growthStages: List<String> = listOf(
        "Seedling",
        "Vegetative",
        "Flowering / Reproductive",
        "Maturity"
    )

    fun findByIdOrName(query: String): CropProfile {
        return presets.firstOrNull {
            it.id.equals(query, ignoreCase = true) ||
                it.name.equals(query, ignoreCase = true)
        } ?: presets.first()
    }
}

enum class SensorDisplayMode {
    LIVE_BLE,
    DEMO_NORMAL,
    DEMO_ALERT
}

enum class BleConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    BLE_UNSUPPORTED,
    BLUETOOTH_DISABLED,
    PERMISSION_REQUIRED,
    ERROR
}

@Immutable
data class CalibrationConfig(
    val dryAdcValue: Int = 3200,
    val wetAdcValue: Int = 1400,
    val serviceUuid: String = "4fafc201-1fb5-459e-8fcc-c5c9c331914b",
    val characteristicUuid: String = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
) {
    fun computeMoistureFromRaw(rawAdc: Int): Int? {
        if (dryAdcValue == wetAdcValue) return null
        val span = (dryAdcValue - wetAdcValue).toDouble()
        val pct = ((dryAdcValue - rawAdc) / span) * 100.0
        return pct.toInt().coerceIn(0, 100)
    }
}

@Immutable
data class SensorReading(
    val moistureRaw: Int? = null,
    val moisturePercent: Int? = null,
    val ph: Double? = null,
    val temperatureC: Double? = null,
    val timestampMs: Long? = null,
    val isDemo: Boolean = false,
    val isAlertDemo: Boolean = false,
    val validationErrors: List<String> = emptyList()
)

@Immutable
data class BleState(
    val status: BleConnectionStatus = BleConnectionStatus.DISCONNECTED,
    val connectedDeviceName: String? = null,
    val connectedDeviceAddress: String? = null,
    val lastReading: SensorReading? = null,
    val lastRawPacket: String? = null,
    val errorMessage: String? = null
)

enum class RecordCategory(val label: String) {
    SENSOR_READING("Sensor Readings"),
    SOIL_SCAN("Soil Scans"),
    CROP_SCAN("Crop Scans"),
    PLANT_HEALTH_SCAN("Plant Health"),
    NPK_REPORT("NPK Reports"),
    AI_RECOMMENDATION("AI Advice")
}

enum class RecordOrigin(val badgeText: String) {
    LIVE_SENSOR("LIVE SENSOR"),
    DEMO_DATA("DEMO DATA"),
    LAB_REPORT("LAB REPORT"),
    REFERENCE_ESTIMATE("REFERENCE ESTIMATE"),
    AI_INTERPRETATION("AI INTERPRETATION")
}

@Immutable
data class SoilScanResult(
    val visibleCharacteristics: String,
    val possibleSoilType: String,
    val organicMatterIndicators: String,
    val uncertaintyExplanation: String,
    val recommendedAdditionalTests: List<String>,
    val isDemo: Boolean = false
)

@Immutable
data class CropIdResult(
    val possibleCropName: String,
    val confidenceLevel: String,
    val identifyingFeatures: String,
    val uncertaintyNotes: String,
    val suggestedGrowthStage: String,
    val isDemo: Boolean = false
)

@Immutable
data class PlantHealthResult(
    val visibleSymptoms: String,
    val possibleExplanations: List<String>,
    val suggestedChecks: List<String>,
    val cautiousNextSteps: List<String>,
    val diagnosisLimitations: String,
    val isDemo: Boolean = false
)

@Immutable
data class NpkLabExtractionResult(
    val nitrogen: String,
    val phosphorus: String,
    val potassium: String,
    val unit: String,
    val labSource: String,
    val reportDate: String,
    val extractionNotes: String,
    val isUnreadable: Boolean = false,
    val isDemo: Boolean = false
)

@Immutable
data class NpkRegionalReferenceResult(
    val crop: String,
    val region: String,
    val soilType: String,
    val sufficiencyStatus: String,
    val nutrientOverview: String,
    val source: String,
    val referenceDate: String,
    val limitations: String,
    val isInsufficientInfo: Boolean = false,
    val isDemo: Boolean = false
)

@Immutable
data class AiRecommendationItem(
    val category: String,
    val identifiedIssue: String,
    val whyItMatters: String,
    val suggestedNextStep: String,
    val supportingEvidence: String,
    val limitationsAndUncertainty: String
)

@Immutable
data class AiAdvisoryPlan(
    val summary: String,
    val recommendations: List<AiRecommendationItem>,
    val contextSnapshot: String,
    val isDemo: Boolean = false
)
