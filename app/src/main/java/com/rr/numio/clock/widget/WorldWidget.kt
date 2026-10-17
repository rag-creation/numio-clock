package com.rr.numio.clock.widget

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.rr.numio.clock.R
import com.rr.numio.clock.data.WorldCityStore
import com.rr.numio.clock.ui.clock.cityTimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/**
 * World clock widget: up to 4 cities you choose from the World clock tab. Each time is a TextClock set to
 * the city's time zone, so it ticks by itself. The note under each name ("Tomorrow · +9",
 * "+1:30") and the sun/moon are refreshed with the poster minute tick.
 */
object WorldWidget {
    private val rows = listOf(
        Triple(R.id.world_row_0, R.id.world_name_0, R.id.world_note_0) to R.id.world_time_0,
        Triple(R.id.world_row_1, R.id.world_name_1, R.id.world_note_1) to R.id.world_time_1,
        Triple(R.id.world_row_2, R.id.world_name_2, R.id.world_note_2) to R.id.world_time_2,
        Triple(R.id.world_row_3, R.id.world_name_3, R.id.world_note_3) to R.id.world_time_3,
    )

    private const val PREFS = "numio_prefs"
    private const val KEY_CITIES = "world_widget_cities"
    const val MAX = 4

    /** The cities chosen for the widget (names), or null if none chosen yet. */
    fun chosen(context: Context): List<String>? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CITIES, null)
            ?.split('|')?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }

    fun saveChosen(context: Context, names: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CITIES, names.joinToString("|")).apply()
        ClockWidget.updateAll(context)
    }

    /** The cities to show: the chosen ones (in World clock order), or the first 3 if none are chosen. */
    private fun citiesFor(context: Context): List<com.rr.numio.clock.data.WorldCity> {
        val all = runCatching { runBlocking { WorldCityStore.getCities(context).first() } }.getOrDefault(emptyList())
        val names = chosen(context)
        val picked = if (names == null) all.take(3) else all.filter { it.name in names }
        return picked.ifEmpty { all.take(3) }.take(MAX)
    }

    fun build(context: Context, clear: Boolean = false, options: android.os.Bundle? = null): RemoteViews {
        // Same font as the Card & Clear widgets, and the app's accent colour for the times.
        val (cardLayout, clearLayout) = WidgetFonts.worldLayouts(WidgetFonts.current(context))
        val views = RemoteViews(context.packageName, if (clear) clearLayout else cardLayout)
        val cities = citiesFor(context)
        val accent = PosterStyles.accent(context)

        // Text grows with the widget: each city gets an equal share of the height.
        val wDp = options?.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)?.takeIf { it > 0 } ?: 250
        val hDp = options?.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180)?.takeIf { it > 0 } ?: 180
        val rowDp = (hDp - 20f) / maxOf(1, cities.size)
        val timeSp = minOf(rowDp * 0.42f, wDp * 0.13f).coerceIn(18f, 54f)
        val nameSp = (timeSp * 0.55f).coerceIn(13f, 26f)
        val noteSp = (timeSp * 0.44f).coerceIn(10f, 20f)
        val now = Instant.now()
        val here = ZoneId.systemDefault()
        val hereDate = now.atZone(here).toLocalDate()
        val hereOffset = here.rules.getOffset(now).totalSeconds / 60

        rows.forEachIndexed { i, (ids, timeId) ->
            val (rowId, nameId, noteId) = ids
            val city = cities.getOrNull(i)
            if (city == null) {
                views.setViewVisibility(rowId, View.GONE)
                return@forEachIndexed
            }
            views.setViewVisibility(rowId, View.VISIBLE)
            val tz = cityTimeZone(city)
            val zone = tz.toZoneId()
            val there = now.atZone(zone)
            val night = there.hour < 6 || there.hour >= 18
            views.setTextViewText(nameId, (if (night) "🌙 " else "☀️ ") + city.name)
            views.setString(timeId, "setTimeZone", tz.id)
            views.setTextViewTextSize(timeId, android.util.TypedValue.COMPLEX_UNIT_SP, timeSp)
            // Long names (Pathanamthitta) shrink a little instead of being cut off.
            val fit = if (city.name.length > 9) 9f / city.name.length else 1f
            views.setTextViewTextSize(nameId, android.util.TypedValue.COMPLEX_UNIT_SP, (nameSp * fit).coerceAtLeast(11f))
            views.setTextColor(timeId, accent)
            views.setTextViewTextSize(noteId, android.util.TypedValue.COMPLEX_UNIT_SP, noteSp)

            val diff = zone.rules.getOffset(now).totalSeconds / 60 - hereOffset
            val diffText = when {
                diff == 0 -> "Same time"
                else -> (if (diff > 0) "+" else "−") + (abs(diff) / 60) +
                    (if (abs(diff) % 60 != 0) ":" + (abs(diff) % 60).toString().padStart(2, '0') else "") + " hrs"
            }
            val day = when (there.toLocalDate().compareTo(hereDate)) {
                0 -> "Today"
                else -> if (there.toLocalDate().isAfter(hereDate)) "Tomorrow" else "Yesterday"
            }
            views.setTextViewText(noteId, "$day · $diffText")
        }
        if (cities.isEmpty()) {
            views.setViewVisibility(R.id.world_row_0, View.VISIBLE)
            views.setTextViewText(R.id.world_name_0, "No cities yet")
            views.setTextViewText(R.id.world_note_0, "Add cities in the World clock tab")
        }
        return views
    }
}
