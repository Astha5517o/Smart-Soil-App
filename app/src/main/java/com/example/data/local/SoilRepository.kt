package com.example.data.local

import com.example.data.model.RecordCategory
import com.example.data.model.RecordOrigin
import kotlinx.coroutines.flow.Flow

interface SoilRecordStorage {
    val allRecords: Flow<List<SoilRecordEntity>>
    suspend fun saveRecord(record: SoilRecordEntity)
    suspend fun deleteRecord(id: String)
    suspend fun clearDemoRecords()
    suspend fun resetAllRecords()
}

class SoilRepository(
    private val dao: SoilRecordDao
) : SoilRecordStorage {

    override val allRecords: Flow<List<SoilRecordEntity>> = dao.observeAllRecords()

    override suspend fun saveRecord(record: SoilRecordEntity) {
        dao.insertRecord(record)
    }

    override suspend fun deleteRecord(id: String) {
        dao.deleteById(id)
    }

    override suspend fun clearDemoRecords() {
        dao.deleteDemoRecords()
    }

    override suspend fun resetAllRecords() {
        dao.deleteAllRecords()
    }

    suspend fun seedDemoSampleRecords() {
        val now = System.currentTimeMillis()
        val samples = listOf(
            SoilRecordEntity(
                timestampMs = now - 3_600_000L,
                category = RecordCategory.SENSOR_READING.name,
                originBadge = RecordOrigin.DEMO_DATA.name,
                isDemo = true,
                dataSource = "Demo Mode (Simulated Exhibition Packet)",
                title = "Demo Sensor Snapshot — Paddy Plot",
                summary = "Moisture: 44% • pH: 6.4 • Temp: 26.8°C",
                fullDetails = "DEMO DATA (Illustrative Example)\nMoisture Raw ADC: 2408 (Calibrated 44%)\nSoil pH: 6.4 (Near Neutral)\nTemperature: 26.8°C (DS18B20)\nStatus: Within preferred Paddy (Rice) range."
            ),
            SoilRecordEntity(
                timestampMs = now - 7_200_000L,
                category = RecordCategory.SOIL_SCAN.name,
                originBadge = RecordOrigin.DEMO_DATA.name,
                isDemo = true,
                dataSource = "Sample Exhibition Soil Scan",
                title = "Clay Loam Visual Assessment (Demo)",
                summary = "Dark brown clod structure with moderate organic residue.",
                fullDetails = "DEMO SAMPLE — Visual Soil Check\nVisible characteristics: Dark grayish-brown aggregates holding shape when lightly pressed.\nPossible soil type: Clay loam / alluvial loam.\nUncertainty: Visual inspection cannot determine pH, moisture percentage, or NPK nutrients.\nNext tests: Confirm moisture with HW-390 probe and lab NPK test."
            ),
            SoilRecordEntity(
                timestampMs = now - 10_800_000L,
                category = RecordCategory.NPK_REPORT.name,
                originBadge = RecordOrigin.REFERENCE_ESTIMATE.name,
                isDemo = true,
                dataSource = "Sample Regional Extension Reference (Demo)",
                title = "Paddy Regional Reference (Illustrative)",
                summary = "REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST.",
                fullDetails = "REFERENCE ESTIMATE — NOT AN ACTUAL SOIL TEST.\nCrop: Paddy (Rice)\nRegion: Indo-Gangetic Alluvial Plains (Illustrative Demo)\nNote: Regional averages vary widely by plot history. Use a Soil Health Card or lab test before applying fertilizers."
            )
        )
        samples.forEach { dao.insertRecord(it) }
    }
}
