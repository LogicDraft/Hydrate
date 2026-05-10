package com.gowtham.hydrate.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gowtham.hydrate.data.local.DailyStatsDao
import com.gowtham.hydrate.data.local.DailyStatsEntity
import com.gowtham.hydrate.data.local.WaterLogDao
import com.gowtham.hydrate.data.local.WaterLogEntity
import com.gowtham.hydrate.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrationRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val waterLogDao: WaterLogDao,
    private val dailyStatsDao: DailyStatsDao,
) : HydrationRepository {

    private val wakeTimeKey = stringPreferencesKey("wake_time")
    private val sleepTimeKey = stringPreferencesKey("sleep_time")
    private val dailyGoalKey = intPreferencesKey("daily_goal")
    private val cupSizeKey = intPreferencesKey("cup_size")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val snoozeMinutesKey = intPreferencesKey("snooze_minutes")
    private val onboardedKey = booleanPreferencesKey("onboarded")

    private val zoneId = ZoneId.systemDefault()
    private val todayStartMillis = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val tomorrowStartMillis = LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    override val preferences: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            wakeTime = preferences[wakeTimeKey]?.let(java.time.LocalTime::parse) ?: java.time.LocalTime.of(7, 0),
            sleepTime = preferences[sleepTimeKey]?.let(java.time.LocalTime::parse) ?: java.time.LocalTime.of(23, 0),
            dailyGoalMl = preferences[dailyGoalKey] ?: 2500,
            cupSizeMl = preferences[cupSizeKey] ?: 250,
            notificationsEnabled = preferences[notificationsEnabledKey] ?: true,
            snoozeMinutes = preferences[snoozeMinutesKey] ?: 60,
            onboarded = preferences[onboardedKey] ?: false,
        )
    }

    override val todayLogs: Flow<List<WaterLogEntity>> = waterLogDao.observeLogsBetween(todayStartMillis, tomorrowStartMillis)
    override val recentStats: Flow<List<DailyStatsEntity>> = dailyStatsDao.observeRecent(30)

    override suspend fun savePreferences(preferences: UserPreferences) {
        dataStore.edit { stored ->
            stored[wakeTimeKey] = preferences.wakeTime.toString()
            stored[sleepTimeKey] = preferences.sleepTime.toString()
            stored[dailyGoalKey] = preferences.dailyGoalMl
            stored[cupSizeKey] = preferences.cupSizeMl
            stored[notificationsEnabledKey] = preferences.notificationsEnabled
            stored[snoozeMinutesKey] = preferences.snoozeMinutes
            stored[onboardedKey] = preferences.onboarded
        }
    }

    override suspend fun logWater(amountMl: Int, timestamp: Instant) {
        val localDate = timestamp.atZone(zoneId).toLocalDate().toString()
        val currentStats = dailyStatsDao.getByDate(localDate)
        val currentPreferences = getPreferencesSnapshot()
        val updatedTotal = (currentStats?.totalMl ?: 0) + amountMl
        waterLogDao.insert(WaterLogEntity(timestampMillis = timestamp.toEpochMilli(), amountMl = amountMl))
        dailyStatsDao.upsert(
            DailyStatsEntity(
                date = localDate,
                totalMl = updatedTotal,
                goalCompleted = updatedTotal >= currentPreferences.dailyGoalMl,
            ),
        )
    }

    override suspend fun clearToday() {
        waterLogDao.deleteBetween(todayStartMillis, tomorrowStartMillis)
        val today = LocalDate.now(zoneId).toString()
        dailyStatsDao.upsert(DailyStatsEntity(date = today, totalMl = 0, goalCompleted = false))
    }

    override suspend fun eraseAllData() {
        waterLogDao.deleteAll()
        dailyStatsDao.deleteAll()
        savePreferences(UserPreferences())
    }

    override suspend fun updateOnboardingComplete() {
        savePreferences(getPreferencesSnapshot().copy(onboarded = true))
    }

    override suspend fun getLatestLogTimestamp(): Instant? = waterLogDao.getLatest()?.timestampMillis?.let(Instant::ofEpochMilli)

    override suspend fun getPreferencesSnapshot(): UserPreferences = preferences.first()
}
