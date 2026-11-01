package com.rr.numio.clock.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.rr.numio.clock.R
import com.rr.numio.clock.data.WorldCity
import com.rr.numio.clock.data.WorldCityStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * World Sticker — one city per 2×2 tile. Each tile you place keeps its own city and time colour,
 * so two tiles side by side can show two different cities.
 */
class ClockWidgetWorldSticker : ClockWidget() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WorldSticker.forget(context, it) }
    }
}

object WorldSticker {
    private const val PREFS = "numio_prefs"
    private fun cityKey(id: Int) = "world_sticker_city_$id"
    private fun colorKey(id: Int) = "world_sticker_color_$id"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Your world clock cities, in World clock order. */
    fun allCities(context: Context): List<WorldCity> =
        runCatching { runBlocking { WorldCityStore.getCities(context).first() } }.getOrDefault(emptyList())

    /** The city this tile shows: the one picked for it, or the first world clock city. */
    fun city(context: Context, id: Int, all: List<WorldCity> = allCities(context)): WorldCity? {
        val name = prefs(context).getString(cityKey(id), null)
        return all.firstOrNull { it.name == name } ?: all.firstOrNull()
    }

    fun cityName(context: Context, id: Int): String? = prefs(context).getString(cityKey(id), null)

    fun color(context: Context, id: Int): String =
        prefs(context).getString(colorKey(id), null) ?: WorldStickerRenderer.COLOR_CREAM

    fun save(context: Context, id: Int, city: String, color: String) {
        prefs(context).edit().putString(cityKey(id), city).putString(colorKey(id), color).apply()
    }

    fun forget(context: Context, id: Int) {
        prefs(context).edit().remove(cityKey(id)).remove(colorKey(id)).apply()
    }

    /** Draw the tile as a picture; it's redrawn every minute with the poster tick. */
    fun build(context: Context, id: Int, options: Bundle, all: List<WorldCity>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_poster)
        val density = context.resources.displayMetrics.density
        val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160).takeIf { it > 0 } ?: 160
        val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180).takeIf { it > 0 } ?: 180
        val scaleDown = minOf(1f, 700f / (wDp * density), 800f / (hDp * density))
        val wPx = (wDp * density * scaleDown).toInt().coerceAtLeast(1)
        val hPx = (hDp * density * scaleDown).toInt().coerceAtLeast(1)
        val bmp = android.graphics.Bitmap.createBitmap(wPx, hPx, android.graphics.Bitmap.Config.ARGB_8888)
        val city = city(context, id, all)
        WorldStickerRenderer.draw(context, android.graphics.Canvas(bmp), wPx, hPx, city, color(context, id))
        views.setImageViewBitmap(R.id.poster_image, bmp)
        views.setContentDescription(R.id.poster_image, "World clock: " + (city?.name ?: "no city"))
        return views
    }
}
