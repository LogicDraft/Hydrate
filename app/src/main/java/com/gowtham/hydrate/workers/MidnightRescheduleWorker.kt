package com.gowtham.hydrate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gowtham.hydrate.di.HydrateEntryPoint
import dagger.hilt.android.EntryPointAccessors

class MidnightRescheduleWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, HydrateEntryPoint::class.java)
        val repository = entryPoint.repository()
        val scheduler = entryPoint.scheduler()
        val preferences = repository.getPreferencesSnapshot()
        val schedule = entryPoint.generateScheduleUseCase().invoke(preferences, emptyList())
        scheduler.scheduleDailyReminders(preferences, schedule)
        scheduler.scheduleMidnightReschedule()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "midnight_reschedule"
    }
}
