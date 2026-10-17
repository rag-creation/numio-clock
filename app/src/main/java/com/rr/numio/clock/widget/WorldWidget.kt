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
 * World clock widget: the first 2, 3 or 4 cities from the World clock tab. Each time is a TextClock set to
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
    private const val KEY_COUNT = "world_widget_count"

    /** How many cities the widget shows: 2, 3 or 4 (chosen in Settings → Widgets). */
    fun count(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_COUNT, 3).coerceIn(2, 4)

    fun saveCount(context: Context, n: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_COUNT, n.coerceIn(2, 4)).apply()
        ClockWidget.updateAll(context)
    }

    fun build(context: Context, clear: Boolean = false): RemoteViews {
        val views = RemoteViews(context.packageName, if (clear) R.layout.widget_world_clear else R.layout.widget_world)
        val cities = runCatching { runBlocking { WorldCityStore.getCities(context).first() } }.getOrDefault(emptyList())
            .take(count(context))
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
