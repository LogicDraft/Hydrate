package com.gowtham.hydrate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WaterLogEntity::class, DailyStatsEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HydrateDatabase : RoomDatabase() {
    abstract fun waterLogDao(): WaterLogDao
    abstract fun dailyStatsDao(): DailyStatsDao
}
