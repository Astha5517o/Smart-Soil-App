package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CalibrationConfig
import com.example.data.model.CropCatalog
import com.example.data.model.CropProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_soil_prefs")

data class UserPreferencesState(
    val selectedCropName: String = "Paddy (Rice)",
    val growthStage: String = "Vegetative",
    val customMinMoisture: Int = 38,
    val customMaxMoisture: Int = 55,
    val customMinPh: Double = 5.5,
    val customMaxPh: Double = 7.0,
    val calibration: CalibrationConfig = CalibrationConfig()
) {
    fun toEffectiveCropProfile(): CropProfile {
        val base = CropCatalog.findByIdOrName(selectedCropName)
        return base.copy(
            name = selectedCropName,
            minMoisturePct = customMinMoisture,
            maxMoisturePct = customMaxMoisture,
            minPh = customMinPh,
            maxPh = customMaxPh
        )
    }
}

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val CROP_NAME = stringPreferencesKey("crop_name")
        val GROWTH_STAGE = stringPreferencesKey("growth_stage")
        val MIN_MOISTURE = intPreferencesKey("min_moisture")
        val MAX_MOISTURE = intPreferencesKey("max_moisture")
        val MIN_PH = stringPreferencesKey("min_ph")
        val MAX_PH = stringPreferencesKey("max_ph")
        val DRY_ADC = intPreferencesKey("dry_adc")
        val WET_ADC = intPreferencesKey("wet_adc")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        val cropName = prefs[Keys.CROP_NAME] ?: "Paddy (Rice)"
        val preset = CropCatalog.findByIdOrName(cropName)
        UserPreferencesState(
            selectedCropName = cropName,
            growthStage = prefs[Keys.GROWTH_STAGE] ?: "Vegetative",
            customMinMoisture = prefs[Keys.MIN_MOISTURE] ?: preset.minMoisturePct,
            customMaxMoisture = prefs[Keys.MAX_MOISTURE] ?: preset.maxMoisturePct,
            customMinPh = prefs[Keys.MIN_PH]?.toDoubleOrNull() ?: preset.minPh,
            customMaxPh = prefs[Keys.MAX_PH]?.toDoubleOrNull() ?: preset.maxPh,
            calibration = CalibrationConfig(
                dryAdcValue = prefs[Keys.DRY_ADC] ?: 3200,
                wetAdcValue = prefs[Keys.WET_ADC] ?: 1400
            )
        )
    }

    suspend fun updateCrop(cropName: String, growthStage: String) {
        val preset = CropCatalog.findByIdOrName(cropName)
        context.dataStore.edit { prefs ->
            prefs[Keys.CROP_NAME] = cropName
            prefs[Keys.GROWTH_STAGE] = growthStage
            prefs[Keys.MIN_MOISTURE] = preset.minMoisturePct
            prefs[Keys.MAX_MOISTURE] = preset.maxMoisturePct
            prefs[Keys.MIN_PH] = preset.minPh.toString()
            prefs[Keys.MAX_PH] = preset.maxPh.toString()
        }
    }

    suspend fun updateThresholds(minMoisture: Int, maxMoisture: Int, minPh: Double, maxPh: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MIN_MOISTURE] = minMoisture.coerceIn(5, 95)
            prefs[Keys.MAX_MOISTURE] = maxMoisture.coerceIn(minMoisture + 5, 100)
            prefs[Keys.MIN_PH] = minPh.coerceIn(3.5, 9.0).toString()
            prefs[Keys.MAX_PH] = maxPh.coerceIn(minPh + 0.3, 10.5).toString()
        }
    }

    suspend fun updateCalibration(dryAdc: Int, wetAdc: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DRY_ADC] = dryAdc.coerceIn(500, 4095)
            prefs[Keys.WET_ADC] = wetAdc.coerceIn(100, 4095)
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
