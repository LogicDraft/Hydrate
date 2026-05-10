package com.gowtham.hydrate.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.gowtham.hydrate.data.local.DailyStatsDao
import com.gowtham.hydrate.data.local.HydrateDatabase
import com.gowtham.hydrate.data.local.WaterLogDao
import com.gowtham.hydrate.data.repository.HydrationRepository
import com.gowtham.hydrate.data.repository.HydrationRepositoryImpl
import com.gowtham.hydrate.domain.scheduler.HydrationNotificationManager
import com.gowtham.hydrate.domain.scheduler.HydrationScheduler
import com.gowtham.hydrate.domain.scheduler.HydrationSchedulerImpl
import com.gowtham.hydrate.domain.usecase.CalculateHistorySummaryUseCase
import com.gowtham.hydrate.domain.usecase.CalculateTodaySummaryUseCase
import com.gowtham.hydrate.domain.usecase.GenerateScheduleUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRepository(impl: HydrationRepositoryImpl): HydrationRepository

    @Binds
    @Singleton
    abstract fun bindScheduler(impl: HydrationSchedulerImpl): HydrationScheduler
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("hydrate_preferences") },
        )

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HydrateDatabase =
        Room.databaseBuilder(context, HydrateDatabase::class.java, "hydrate.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWaterLogDao(database: HydrateDatabase): WaterLogDao = database.waterLogDao()

    @Provides
    fun provideDailyStatsDao(database: HydrateDatabase): DailyStatsDao = database.dailyStatsDao()

    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext context: Context): HydrationNotificationManager =
        HydrationNotificationManager(context)

    @Provides
    @Singleton
    fun provideGenerateScheduleUseCase(): GenerateScheduleUseCase = GenerateScheduleUseCase()

    @Provides
    @Singleton
    fun provideCalculateTodaySummaryUseCase(): CalculateTodaySummaryUseCase = CalculateTodaySummaryUseCase()

    @Provides
    @Singleton
    fun provideCalculateHistorySummaryUseCase(): CalculateHistorySummaryUseCase = CalculateHistorySummaryUseCase()
}
