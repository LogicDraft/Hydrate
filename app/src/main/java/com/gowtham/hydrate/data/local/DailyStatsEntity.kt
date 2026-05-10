package com.gowtham.hydrate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val date: String,
    val totalMl: Int,
    val goalCompleted: Boolean,
)
