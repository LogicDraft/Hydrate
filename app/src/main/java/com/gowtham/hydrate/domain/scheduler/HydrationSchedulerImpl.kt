package com.gowtham.hydrate.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gowtham.hydrate.data.model.ReminderSlot
import com.gowtham.hydrate.data.model.UserPreferences
import com.gowtham.hydrate.receivers.HydrationAlarmReceiver
import com.gowtham.hydrate.workers.MidnightRescheduleWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrationSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : HydrationScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun scheduleDailyReminders(preferences: UserPreferences, schedule: List<ReminderSlot>) {
        if (!preferences.notificationsEnabled) return
        schedule.filter { it.timestampMillis > System.currentTimeMillis() }.forEachIndexed { index, slot ->
            val intent = Intent(context, HydrationAlarmReceiver::class.java).apply {
                putExtra(HydrationAlarmReceiver.EXTRA_AMOUNT_ML, slot.amountMl)
                putExtra(HydrationAlarmReceiver.EXTRA_TIMESTAMP_MILLIS, slot.timestampMillis)
                putExtra(HydrationAlarmReceiver.EXTRA_REQUEST_CODE, requestCodeFor(slot.timestampMillis))
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeFor(slot.timestampMillis),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, slot.timestampMillis, pendingIntent)
        }
    }

    override fun scheduleSnoozedReminder(amountMl: Int, triggerAtMillis: Long) {
        val intent = Intent(context, HydrationAlarmReceiver::class.java).apply {
            putExtra(HydrationAlarmReceiver.EXTRA_AMOUNT_ML, amountMl)
            putExtra(HydrationAlarmReceiver.EXTRA_TIMESTAMP_MILLIS, triggerAtMillis)
            putExtra(HydrationAlarmReceiver.EXTRA_REQUEST_CODE, triggerAtMillis.hashCode())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(triggerAtMillis),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    override fun cancelSlotReminder(slotTimestampMillis: Long) {
        val intent = Intent(context, HydrationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(slotTimestampMillis),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun scheduleMidnightReschedule() {
        val now = Instant.now()
        val nextMidnight = LocalDateTime.now(ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val delayMillis = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1)
        val request = OneTimeWorkRequestBuilder<MidnightRescheduleWorker>()
            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("reschedule_at" to nextMidnight.toEpochMilli()))
            .addTag(MidnightRescheduleWorker.WORK_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            MidnightRescheduleWorker.WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun cancelAllReminders() {
        repeat(120) { requestCode ->
            val intent = Intent(context, HydrationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
        }
        WorkManager.getInstance(context).cancelUniqueWork(MidnightRescheduleWorker.WORK_NAME)
    }

    private fun requestCodeFor(timestampMillis: Long): Int = (timestampMillis xor (timestampMillis ushr 32)).toInt()
}
