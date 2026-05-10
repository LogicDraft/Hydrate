package com.gowtham.hydrate.domain.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
        const val CHANNEL_ID = "hydrate_reminders"
        const val NOTIFICATION_ID = 7001
        const val ACTION_I_DRANK = "com.gowtham.hydrate.action.I_DRANK"
        const val ACTION_SNOOZE = "com.gowtham.hydrate.action.SNOOZE"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hydration reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Exact hydration reminder alerts"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminder(amountMl: Int, requestCode: Int) {
        ensureChannel()
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle("Time to Hydrate")
            .setContentText("Drink $amountMl ml of water.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "I Drank", drankPendingIntent)
            .addAction(0, "Snooze 1 Hour", snoozePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + requestCode, notification)
    }

    fun cancel(requestCode: Int) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID + requestCode)
    }
}
