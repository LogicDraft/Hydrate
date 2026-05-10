package com.gowtham.hydrate.di

import com.gowtham.hydrate.data.repository.HydrationRepository
import com.gowtham.hydrate.domain.scheduler.HydrationNotificationManager
import com.gowtham.hydrate.domain.scheduler.HydrationScheduler
import com.gowtham.hydrate.domain.usecase.GenerateScheduleUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HydrateEntryPoint {
    fun repository(): HydrationRepository
    fun scheduler(): HydrationScheduler
    fun notificationManager(): HydrationNotificationManager
    fun generateScheduleUseCase(): GenerateScheduleUseCase
}
