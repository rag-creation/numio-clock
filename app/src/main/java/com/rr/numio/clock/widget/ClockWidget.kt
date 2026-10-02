package com.rr.numio.clock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import com.rr.numio.clock.MainActivity
import com.rr.numio.clock.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Home screen widgets: time + date + next alarm.
 * ClockWidget       → dark card
 * ClockWidgetClear  → transparent, just the clock
 * The time and date are TextClocks, so they tick by themselves — no background work.
 */
open class ClockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAll(context)
    }

    // Called when the user resizes the widget
    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle
    ) {
        updateAll(context)
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
        /** Refresh every placed widget of both styles. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val variants = listOf(
                ClockWidget::class.java to R.layout.widget_clock,
                ClockWidgetClear::class.java to R.layout.widget_clock_clear
            )
            for ((cls, layout) in variants) {
                manager.getAppWidgetIds(ComponentName(context, cls)).forEach { id ->
                    val options = manager.getAppWidgetOptions(id)
                    manager.updateAppWidget(id, buildViews(context, layout, options))
                }
            }
        }

        private fun buildViews(context: Context, layout: Int, options: Bundle): RemoteViews {
            val views = RemoteViews(context.packageName, layout)
            views.setTextViewText(R.id.widget_next_alarm, nextAlarmText(context))

            // Scale the text to the widget's size.
            // Base design = 250 × 110 dp. Portrait width = MIN_WIDTH, portrait height = MAX_HEIGHT.
            val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250).takeIf { it > 0 } ?: 250
            val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110).takeIf { it > 0 } ?: 110
            val scale = minOf(w / 250f, h / 110f).coerceIn(0.7f, 2.4f)

            fun size(id: Int, sp: Float) =
                views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sp * scale)
            size(R.id.widget_time, 52f)
            size(R.id.widget_ampm, 16f)
            size(R.id.widget_date, 13f)
            size(R.id.widget_next_alarm, 12f)

            // Tap the widget to open the app
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
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

/** Transparent variant — same behaviour, no background card. */
class ClockWidgetClear : ClockWidget()
