package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoilRecordDao {
    @Query("SELECT * FROM soil_records ORDER BY timestampMs DESC")
    fun observeAllRecords(): Flow<List<SoilRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SoilRecordEntity)

    @Query("DELETE FROM soil_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM soil_records WHERE isDemo = 1")
    suspend fun deleteDemoRecords()

    @Query("DELETE FROM soil_records")
    suspend fun deleteAllRecords()
}
