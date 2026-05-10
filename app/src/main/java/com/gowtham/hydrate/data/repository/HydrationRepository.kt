package com.gowtham.hydrate.data.repository

import com.gowtham.hydrate.data.local.DailyStatsEntity
import com.gowtham.hydrate.data.local.WaterLogEntity
import com.gowtham.hydrate.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface HydrationRepository {
    val preferences: Flow<UserPreferences>
    val skippedReminderTimestamps: Flow<Set<Long>>
    val todayLogs: Flow<List<WaterLogEntity>>
    val recentStats: Flow<List<DailyStatsEntity>>

    suspend fun savePreferences(preferences: UserPreferences)
    suspend fun logWater(amountMl: Int, timestamp: Instant = Instant.now())
    suspend fun undoLastLog(): WaterLogEntity?
    suspend fun skipReminderSlot(timestampMillis: Long)
    suspend fun clearSkippedReminderSlots()
    suspend fun clearToday()
    suspend fun eraseAllData()
    suspend fun updateOnboardingComplete()
    suspend fun getLatestLogTimestamp(): Instant?
    suspend fun getPreferencesSnapshot(): UserPreferences
}
