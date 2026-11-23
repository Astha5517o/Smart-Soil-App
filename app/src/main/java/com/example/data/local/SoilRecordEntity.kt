package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "soil_records")
data class SoilRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val category: String,      // RecordCategory.name
    val originBadge: String,   // RecordOrigin.name
    val isDemo: Boolean,
    val dataSource: String,
    val title: String,
    val summary: String,
    val fullDetails: String,
    val imageUri: String? = null
)
