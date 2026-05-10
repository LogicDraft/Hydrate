package com.gowtham.hydrate.receivers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gowtham.hydrate.MainActivity
import com.gowtham.hydrate.R
import com.gowtham.hydrate.data.repository.HydrationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class HydrateWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var repository: HydrationRepository

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val logs = repository.todayLogs.first()
            val preferences = repository.getPreferencesSnapshot()
            val totalMl = logs.sumOf { it.amountMl }
            val goalMl = preferences.dailyGoalMl
            val percent = if (goalMl > 0) (totalMl * 100) / goalMl else 0

            val emoji = when {
                percent == 0 -> "🫗"
                percent < 25 -> "💧"
                percent < 50 -> "🥤"
                percent < 75 -> "💦"
                percent < 100 -> "🌊"
                else -> "🎉"
            }
            val shortTitle = "$emoji Hydration"
            val remainingMl = (goalMl - totalMl).coerceAtLeast(0)
            val details = if (remainingMl > 0) "$totalMl / $goalMl ml ($remainingMl ml left)" else "Goal crushed! 🎉 ($totalMl ml)"

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_notification_summary)
                views.setTextViewText(R.id.widget_title, shortTitle)
                views.setTextViewText(R.id.widget_details, details)
                views.setTextViewText(R.id.widget_percentage, "$percent%")
                views.setProgressBar(R.id.widget_progress, 100, percent.coerceIn(0, 100), false)

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
