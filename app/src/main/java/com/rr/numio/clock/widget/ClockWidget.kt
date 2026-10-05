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
        if (isPosterTick(intent)) updateAll(context)
    }

    // Last widget of this type removed — stop the poster tick if no posters are left
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        schedulePosterTick(context)
    }

    companion object {
        /** Refresh every placed widget of both styles. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val (cardLayout, clearLayout) = WidgetFonts.layouts(WidgetFonts.current(context))
            val variants = listOf(
                ClockWidget::class.java to cardLayout,
                ClockWidgetClear::class.java to clearLayout
            )
            for ((cls, layout) in variants) {
                manager.getAppWidgetIds(ComponentName(context, cls)).forEach { id ->
                    val options = manager.getAppWidgetOptions(id)
                    manager.updateAppWidget(id, buildViews(context, layout, options))
                }
            }
            // Poster widget has its own layout and fonts
            manager.getAppWidgetIds(ComponentName(context, ClockWidgetPoster::class.java)).forEach { id ->
                manager.updateAppWidget(id, buildPosterViews(context, manager.getAppWidgetOptions(id)))
            }
            // Type clock (2×2) is a picture too
            manager.getAppWidgetIds(ComponentName(context, ClockWidgetType::class.java)).forEach { id ->
                manager.updateAppWidget(id, buildTypeViews(context, manager.getAppWidgetOptions(id)))
            }
            schedulePosterTick(context)
        }

        private fun buildPosterViews(context: Context, options: Bundle): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_poster)

            // Widget size in pixels (portrait: MIN_WIDTH × MAX_HEIGHT)
            val density = context.resources.displayMetrics.density
            val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260).takeIf { it > 0 } ?: 260
            val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 140).takeIf { it > 0 } ?: 140
            // Cap the size so the bitmap stays small in memory
            val scaleDown = minOf(1f, 1100f / (wDp * density), 700f / (hDp * density))
            val wPx = (wDp * density * scaleDown).toInt()
            val hPx = (hDp * density * scaleDown).toInt()

            views.setImageViewBitmap(R.id.poster_image, PosterRenderer.render(context, wPx, hPx))
            views.setContentDescription(R.id.poster_image, PosterRenderer.description(context))
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            return views
        }

        private fun buildTypeViews(context: Context, options: Bundle): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_poster)
            val density = context.resources.displayMetrics.density
            val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140).takeIf { it > 0 } ?: 140
            val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 140).takeIf { it > 0 } ?: 140
            // Square board: draw at the smaller side, capped to keep the bitmap small
            val side = (minOf(wDp, hDp) * density).coerceAtMost(700f).toInt()
            views.setImageViewBitmap(R.id.poster_image, TypeClockRenderer.render(context, side, side))
            views.setContentDescription(R.id.poster_image, TypeClockRenderer.description(context))
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            return views
        }

        // ── Poster minute tick ───────────────────────────────────────────────
        // The poster is a picture, so it needs a redraw every minute.
        // RTC (not RTC_WAKEUP): never wakes the phone; if the screen is off,
        // Android delivers it when the phone next wakes. Almost no battery.
        private const val ACTION_POSTER_TICK = "com.rr.numio.clock.POSTER_TICK"

        private fun tickIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 4242,
                Intent(context, ClockWidgetPoster::class.java).setAction(ACTION_POSTER_TICK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        fun schedulePosterTick(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val hasPoster = manager.getAppWidgetIds(ComponentName(context, ClockWidgetPoster::class.java)).isNotEmpty() ||
                manager.getAppWidgetIds(ComponentName(context, ClockWidgetType::class.java)).isNotEmpty()
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (!hasPoster) {
                alarmManager.cancel(tickIntent(context))
                return
            }
            val nextMinute = (System.currentTimeMillis() / 60_000 + 1) * 60_000
            try {
                alarmManager.setExact(AlarmManager.RTC, nextMinute, tickIntent(context))
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC, nextMinute, tickIntent(context))
            }
        }

        internal fun isPosterTick(intent: Intent) = intent.action == ACTION_POSTER_TICK

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
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

/** Poster variant — hand-lettered collage style. */
class ClockWidgetPoster : ClockWidget()

/** Type clock — 2×2 two-colour digits with the date. */
class ClockWidgetType : ClockWidget()

/**
 * Widget fonts. Widgets can't change fonts from code, so each font has its own
 * layout file (generated from widget_clock.xml / widget_clock_clear.xml).
 */
object WidgetFonts {
    const val PREFS = "numio_prefs"
    const val KEY = "widget_font"

    /** key to label shown in Settings */
    val options = listOf(
        "light" to "Light",
        "thin" to "Thin",
        "bold" to "Bold",
        "condensed" to "Condensed",
        "mono" to "Mono",
        "serif" to "Serif"
    )

    /** Android system font family name for each key */
    fun family(key: String): String = when (key) {
        "thin" -> "sans-serif-thin"
        "bold" -> "sans-serif-black"
        "condensed" -> "sans-serif-condensed-light"
        "mono" -> "monospace"
        "serif" -> "serif"
        else -> "sans-serif-light"
    }

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "light") ?: "light"

    fun save(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, key).apply()
        ClockWidget.updateAll(context)
    }

    /** (card layout, clear layout) for a font key */
    fun layouts(key: String): Pair<Int, Int> = when (key) {
        "thin" -> R.layout.widget_clock_thin to R.layout.widget_clock_clear_thin
        "bold" -> R.layout.widget_clock_bold to R.layout.widget_clock_clear_bold
        "condensed" -> R.layout.widget_clock_condensed to R.layout.widget_clock_clear_condensed
        "mono" -> R.layout.widget_clock_mono to R.layout.widget_clock_clear_mono
        "serif" -> R.layout.widget_clock_serif to R.layout.widget_clock_clear_serif
        else -> R.layout.widget_clock to R.layout.widget_clock_clear
    }
}
