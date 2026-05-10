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
        const val CHANNEL_ID_SOUND      = "hydrate_reminders_sound"
        const val CHANNEL_ID_VIBRATION  = "hydrate_reminders_vibration"
        const val CHANNEL_ID_RINGTONE   = "hydrate_reminders_ringtone"
        const val CHANNEL_ID_SUMMARY    = "hydrate_lock_screen_summary"
        const val NOTIFICATION_ID       = 7001
        const val SUMMARY_NOTIFICATION_ID = 7300
        const val ACTION_I_DRANK  = "com.gowtham.hydrate.action.I_DRANK"
        const val ACTION_SNOOZE   = "com.gowtham.hydrate.action.SNOOZE"
        const val ACTION_SKIP     = "com.gowtham.hydrate.action.SKIP"
        const val EXTRA_AMOUNT_ML        = "extra_amount_ml"
        const val EXTRA_REQUEST_CODE     = "extra_request_code"
        const val EXTRA_TIMESTAMP_MILLIS = "extra_timestamp_millis"

        // ── Lock-screen widget helpers ──────────────────────────────────────
        private const val BAR_LENGTH = 10   // number of blocks in progress bar

        private fun buildProgressBar(percent: Int): String {
            val filled = (percent.coerceIn(0, 100) * BAR_LENGTH / 100)
            return "▓".repeat(filled) + "░".repeat(BAR_LENGTH - filled)
        }

        private fun milestoneEmoji(percent: Int) = when {
            percent == 0   -> "🫗"
            percent < 25   -> "💧"
            percent < 50   -> "🥤"
            percent < 75   -> "💦"
            percent < 100  -> "🌊"
            else           -> "🎉"
        }

        private fun milestoneLabel(percent: Int) = when {
            percent == 0   -> "Not started yet"
            percent < 25   -> "Just beginning…"
            percent < 50   -> "Keep sipping!"
            percent < 75   -> "Halfway there!"
            percent < 100  -> "Almost there!"
            else           -> "Goal crushed! 🏆"
        }
    }

    // ── Channel setup ───────────────────────────────────────────────────────

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val notifAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val ringtoneAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // (A) Gentle notification sound – default notification tone
        val soundChannel = NotificationChannel(
            CHANNEL_ID_SOUND,
            "Hydration reminders (sound)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Hydration reminders with gentle notification sound"
            enableVibration(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                notifAudio,
            )
        }

        // (B) Vibration only – silent
        val vibrationChannel = NotificationChannel(
            CHANNEL_ID_VIBRATION,
            "Hydration reminders (vibration)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Hydration reminders with vibration only"
            enableVibration(true)
            setSound(null, null)
        }

        // (C) Phone notification sound – uses the device ringtone URI so it
        //     sounds like an incoming notification from a messaging / calling app
        val ringtoneChannel = NotificationChannel(
            CHANNEL_ID_RINGTONE,
            "Hydration reminders (phone notification)",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Hydration reminders with phone notification sound"
            enableVibration(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                ringtoneAudio,
            )
        }

        // (D) Lock-screen widget summary – no sound, no vibration, persistent
        val summaryChannel = NotificationChannel(
            CHANNEL_ID_SUMMARY,
            "Hydration lock-screen summary",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Widget-style progress shown on lock screen without opening the app"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannels(
            listOf(soundChannel, vibrationChannel, ringtoneChannel, summaryChannel),
        )
    }

    // ── Reminder notification ───────────────────────────────────────────────

    fun showReminder(
        amountMl: Int,
        requestCode: Int,
        slotTimestampMillis: Long,
        vibrationOnly: Boolean,
        phoneRingtone: Boolean = false,
    ) {
        ensureChannels()

        val reminderChannelId = when {
            vibrationOnly  -> CHANNEL_ID_VIBRATION
            phoneRingtone  -> CHANNEL_ID_RINGTONE
            else           -> CHANNEL_ID_SOUND
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("route", "today")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, requestCode, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val drankIntent = Intent(context, com.gowtham.hydrate.receivers.HydrationActionReceiver::class.java).apply {
            action = ACTION_I_DRANK
            putExtra(EXTRA_AMOUNT_ML, amountMl)
        }
        val drankPendingIntent = PendingIntent.getBroadcast(
            context, requestCode + 10_000, drankIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, com.gowtham.hydrate.receivers.HydrationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_AMOUNT_ML, amountMl)
            putExtra(EXTRA_REQUEST_CODE, requestCode)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, requestCode + 20_000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val skipIntent = Intent(context, com.gowtham.hydrate.receivers.HydrationActionReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra(EXTRA_REQUEST_CODE, requestCode)
            putExtra(EXTRA_TIMESTAMP_MILLIS, slotTimestampMillis)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context, requestCode + 30_000, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, reminderChannelId)
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle("💧 Time to Hydrate")
            .setContentText("Drink $amountMl ml of water.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "✅ I Drank", drankPendingIntent)
            .addAction(0, "⏰ Snooze 15 Min", snoozePendingIntent)
            .addAction(0, "⏭ Skip", skipPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + requestCode, notification)
    }

    fun cancel(requestCode: Int) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID + requestCode)
    }

    // ── Lock-screen widget summary (Feature 13) ─────────────────────────────
    //
    //  Shown as a persistent, always-visible notification on the lock screen
    //  so the user can see today's % without unlocking / opening the app.
    //  Uses VISIBILITY_PUBLIC so Android renders it in full on the lock screen.

    fun showLockScreenSummary(percent: Int, totalMl: Int, goalMl: Int) {
        ensureChannels()

        val bar   = buildProgressBar(percent)
        val emoji = milestoneEmoji(percent)
        val label = milestoneLabel(percent)

        val remainingMl = (goalMl - totalMl).coerceAtLeast(0)
        val shortTitle  = "$emoji $percent% hydrated today"
        val shortBody   = "$bar  $totalMl / $goalMl ml"
        val expandedText = buildString {
            appendLine("$emoji  $label")
            appendLine()
            appendLine("Progress  $bar  $percent%")
            appendLine("Consumed  $totalMl ml")
            appendLine("Goal      $goalMl ml")
            if (remainingMl > 0) append("Remaining $remainingMl ml")
            else append("You've hit your daily goal! 🎉")
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("route", "today")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, SUMMARY_NOTIFICATION_ID, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val summary = NotificationCompat.Builder(context, CHANNEL_ID_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle(shortTitle)
            .setContentText(shortBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setBigContentTitle(shortTitle),
            )
            // ── Widget-style lock-screen behaviour ──────────────────────────
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)   // show full content on lock screen
            .setOngoing(true)           // cannot be dismissed by swipe
            .setOnlyAlertOnce(true)     // no sound/vibration on update
            .setShowWhen(false)         // no timestamp clutter
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(false)
            .build()

        NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, summary)
    }
}
