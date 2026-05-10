package com.gowtham.hydrate.domain.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gowtham.hydrate.MainActivity
import com.gowtham.hydrate.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrationNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        const val CHANNEL_ID_SOUND = "hydrate_reminders_sound"
        const val CHANNEL_ID_VIBRATION = "hydrate_reminders_vibration"
        const val CHANNEL_ID_SUMMARY = "hydrate_lock_screen_summary"
        const val NOTIFICATION_ID = 7001
        const val SUMMARY_NOTIFICATION_ID = 7300
        const val ACTION_I_DRANK = "com.gowtham.hydrate.action.I_DRANK"
        const val ACTION_SNOOZE = "com.gowtham.hydrate.action.SNOOZE"
        const val ACTION_SKIP = "com.gowtham.hydrate.action.SKIP"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
        const val EXTRA_TIMESTAMP_MILLIS = "extra_timestamp_millis"
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val soundChannel = NotificationChannel(
            CHANNEL_ID_SOUND,
            "Hydration reminders (sound)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Hydration reminders with gentle sound"
            enableVibration(true)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
        }

        val vibrationOnlyChannel = NotificationChannel(
            CHANNEL_ID_VIBRATION,
            "Hydration reminders (vibration)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Hydration reminders with vibration only"
            enableVibration(true)
            setSound(null, null)
        }

        val summaryChannel = NotificationChannel(
            CHANNEL_ID_SUMMARY,
            "Hydration lock screen summary",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows today's hydration progress on lock screen"
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannels(listOf(soundChannel, vibrationOnlyChannel, summaryChannel))
    }

    fun showReminder(amountMl: Int, requestCode: Int, slotTimestampMillis: Long, vibrationOnly: Boolean) {
        ensureChannels()
        val reminderChannelId = if (vibrationOnly) CHANNEL_ID_VIBRATION else CHANNEL_ID_SOUND
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("route", "today")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val drankIntent = Intent(context, com.gowtham.hydrate.receivers.HydrationActionReceiver::class.java).apply {
            action = ACTION_I_DRANK
            putExtra(EXTRA_AMOUNT_ML, amountMl)
        }
        val drankPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode + 10_000,
            drankIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeIntent = Intent(context, com.gowtham.hydrate.receivers.HydrationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_AMOUNT_ML, amountMl)
            putExtra(EXTRA_REQUEST_CODE, requestCode)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode + 20_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val skipIntent = Intent(context, com.gowtham.hydrate.receivers.HydrationActionReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra(EXTRA_REQUEST_CODE, requestCode)
            putExtra(EXTRA_TIMESTAMP_MILLIS, slotTimestampMillis)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode + 30_000,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, reminderChannelId)
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle("Time to Hydrate")
            .setContentText("Drink $amountMl ml of water.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "I Drank", drankPendingIntent)
            .addAction(0, "Snooze 15 Min", snoozePendingIntent)
            .addAction(0, "Skip", skipPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + requestCode, notification)
    }

    fun cancel(requestCode: Int) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID + requestCode)
    }

    fun showLockScreenSummary(percent: Int, totalMl: Int, goalMl: Int) {
        ensureChannels()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("route", "today")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            SUMMARY_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val summary = NotificationCompat.Builder(context, CHANNEL_ID_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle("Hydrate Today")
            .setContentText("$percent% complete  ($totalMl/$goalMl ml)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Today: $percent% complete ($totalMl/$goalMl ml). Keep sipping through the day."))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(false)
            .build()

        NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, summary)
    }
}
