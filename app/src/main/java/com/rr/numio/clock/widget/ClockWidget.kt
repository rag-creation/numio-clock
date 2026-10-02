package com.rr.numio.clock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.rr.numio.clock.MainActivity
import com.rr.numio.clock.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Home screen widget: time + date + next alarm.
 * The time and date are TextClocks, so they tick by themselves — no background work.
 * The next-alarm line refreshes whenever Android says the next alarm changed.
 */
class ClockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> updateAll(context)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidget::class.java))
            ids.forEach { update(context, manager, it) }
        }

        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            views.setTextViewText(R.id.widget_next_alarm, nextAlarmText(context))

            // Tap the widget to open the app
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)

            manager.updateAppWidget(id, views)
        }

        private fun nextAlarmText(context: Context): String {
            val next = context.getSystemService(AlarmManager::class.java).nextAlarmClock
                ?: return "No alarms set"

            val alarm = Calendar.getInstance().apply { timeInMillis = next.triggerTime }
            val today = Calendar.getInstance()
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

            fun sameDay(a: Calendar, b: Calendar) =
                a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

            val day = when {
                sameDay(alarm, today) -> "Today"
                sameDay(alarm, tomorrow) -> "Tomorrow"
                else -> SimpleDateFormat("EEE", Locale.getDefault()).format(alarm.time)
            }
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(alarm.time)
            return "Alarm · $day $time"
        }
    }
}
