package com.gowtham.hydrate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {

    @Query("SELECT * FROM water_logs WHERE timestampMillis BETWEEN :start AND :end ORDER BY timestampMillis DESC")
    fun observeLogsBetween(start: Long, end: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE timestampMillis BETWEEN :start AND :end ORDER BY timestampMillis DESC")
    suspend fun getLogsBetween(start: Long, end: Long): List<WaterLogEntity>

    @Query("SELECT * FROM water_logs ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatest(): WaterLogEntity?

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WaterLogEntity): Long

    @Query("DELETE FROM water_logs WHERE timestampMillis BETWEEN :start AND :end")
    suspend fun deleteBetween(start: Long, end: Long)

    @Query("DELETE FROM water_logs")
    suspend fun deleteAll()
}
