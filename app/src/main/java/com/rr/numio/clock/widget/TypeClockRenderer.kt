package com.rr.numio.clock.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.core.content.res.ResourcesCompat
import com.rr.numio.clock.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/** Type clock settings: layout, font, second colour, outline. Saved for the 2×2 widget. */
object TypeClock {
    private const val PREFS = "numio_prefs"
    private const val KEY_LAYOUT = "type_layout"
    private const val KEY_FONT = "type_font"
    private const val KEY_SECOND = "type_second"
    private const val KEY_OUTLINE = "type_outline"
    private const val KEY_CARD = "type_card"

    val layouts = listOf("stacked" to "Stacked", "side" to "Side date", "badge" to "Badge")

    /** key → (label, font) */
    val fonts = listOf(
        Triple("serif", "Serif", R.font.dmserif_display),
        Triple("joti", "Joti", R.font.joti_one),
        Triple("moirai", "Moirai", R.font.moirai_one),
        Triple("pirata", "Pirata", R.font.pirata_one),
        Triple("tall", "Tall", R.font.anton),
        Triple("mono", "Mono", R.font.rubik_mono_one),
        Triple("block", "Block", R.font.bungee)
    )

    val secondColours = listOf(
        0xFFFFF3DC.toInt(), // cream
        0xFFF2B6C8.toInt(), // pink
        0xFF6FD3D6.toInt(), // teal
        0xFFB9A3F0.toInt(), // lavender
        0xFF9BE29B.toInt()  // mint
    )

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun layout(c: Context) = prefs(c).getString(KEY_LAYOUT, "stacked")
        ?.takeIf { k -> layouts.any { it.first == k } } ?: "stacked"
    fun font(c: Context) = prefs(c).getString(KEY_FONT, "joti")
        ?.takeIf { k -> fonts.any { it.first == k } } ?: "joti"
    fun second(c: Context) = prefs(c).getInt(KEY_SECOND, secondColours[0])
    fun outline(c: Context) = prefs(c).getBoolean(KEY_OUTLINE, false)
    /** Dark card behind the clock; off = transparent, straight on the wallpaper. */
    fun card(c: Context) = prefs(c).getBoolean(KEY_CARD, true)

    fun saveLayout(c: Context, v: String) { prefs(c).edit().putString(KEY_LAYOUT, v).apply(); ClockWidget.updateAll(c) }
    fun saveFont(c: Context, v: String) { prefs(c).edit().putString(KEY_FONT, v).apply(); ClockWidget.updateAll(c) }
    fun saveSecond(c: Context, v: Int) { prefs(c).edit().putInt(KEY_SECOND, v).apply(); ClockWidget.updateAll(c) }
    fun saveOutline(c: Context, v: Boolean) { prefs(c).edit().putBoolean(KEY_OUTLINE, v).apply(); ClockWidget.updateAll(c) }
    fun saveCard(c: Context, v: Boolean) { prefs(c).edit().putBoolean(KEY_CARD, v).apply(); ClockWidget.updateAll(c) }

    fun fontRes(key: String) = fonts.firstOrNull { it.first == key }?.third ?: R.font.joti_one
}

/**
 * Draws the 2×2 type clock: big two-colour digits plus the date,
 * in one of three layouts. Drawn on a 300 × 300 board and scaled to the widget.
 */
object TypeClockRenderer {

    private const val CARD = 0xFF1D1D25.toInt()
    private const val INK_ON_ACCENT = 0xFF1D1D25.toInt()

    // On the wallpaper (no card) everything gets a soft shadow so it stays readable.
    // Set per render; render is synchronized because Settings draws previews in parallel.
    private var shadow = false
    private fun Paint.shadowed() = apply { if (shadow) setShadowLayer(4f, 1f, 2f, 0x99000000.toInt()) }

    @Synchronized
    fun render(
        context: Context, width: Int, height: Int,
        layout: String = TypeClock.layout(context),
        fontKey: String = TypeClock.font(context)
    ): Bitmap {
        val w = width.coerceAtLeast(60); val h = height.coerceAtLeast(60)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = min(w / 300f, h / 300f)
        canvas.translate((w - 300f * s) / 2f, (h - 300f * s) / 2f)
        canvas.scale(s, s)

        shadow = !TypeClock.card(context)
        if (!shadow) {
            canvas.drawRoundRect(RectF(2f, 2f, 298f, 298f), 34f, 34f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD })
        }

        val a = PosterStyles.accent(context)
        val b = TypeClock.second(context)
        val outline = TypeClock.outline(context)
        val font = font(context, TypeClock.fontRes(fontKey))
        // Moirai is a hairline font: small text uses Joti so it stays readable
        val small = if (fontKey == "moirai") font(context, R.font.joti_one) else font

        val hh = fmt(if (DateFormat.is24HourFormat(context)) "HH" else "hh")
        val mm = fmt("mm")
        when (layout) {
            "side" -> drawSide(canvas, font, small, hh, mm, a, b, outline)
            "badge" -> drawBadge(canvas, font, small, hh, mm, a, b, outline)
            else -> drawStacked(canvas, font, small, hh, mm, a, b, outline)
        }
        return bmp
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private fun font(context: Context, id: Int): Typeface =
        ResourcesCompat.getFont(context, id) ?: Typeface.DEFAULT_BOLD

    private fun fmt(p: String) = SimpleDateFormat(p, Locale.ENGLISH).format(Calendar.getInstance().time)

    private fun digits(font: Typeface, color: Int, size: Float, outline: Boolean) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font; this.color = color; textSize = size; textAlign = Paint.Align.CENTER
            if (outline) { style = Paint.Style.STROKE; strokeWidth = 3f; strokeJoin = Paint.Join.ROUND }
        }.shadowed()

    private fun label(font: Typeface, color: Int, size: Float, spacing: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font; this.color = color; textSize = size; letterSpacing = spacing
            textAlign = Paint.Align.CENTER
        }.shadowed()

    /** Shrink [p] until [text] fits in [maxW] (and [maxH] for digit height). */
    private fun fit(p: Paint, text: String, maxW: Float, maxH: Float = Float.MAX_VALUE) {
        val r = android.graphics.Rect()
        while (p.textSize > 10f) {
            p.getTextBounds(text, 0, text.length, r)
            if (p.measureText(text) <= maxW && r.height() <= maxH) break
            p.textSize *= 0.95f
        }
    }

    /** Baseline that centres the digits' real ink height on [centreY]. */
    private fun baseline(p: Paint, text: String, centreY: Float): Float {
        val r = android.graphics.Rect()
        p.getTextBounds(text, 0, text.length, r)
        return centreY - r.exactCenterY()
    }

    // ── A · stacked, date underneath ────────────────────────────────────────
    private fun drawStacked(c: Canvas, f: Typeface, small: Typeface, hh: String, mm: String, a: Int, b: Int, outline: Boolean) {
        val top = digits(f, a, 110f, outline); fit(top, "88", 220f, 84f)
        val bottom = digits(f, b, top.textSize, outline)
        c.drawText(hh, 150f, baseline(top, hh, 82f), top)
        c.drawText(mm, 150f, baseline(bottom, mm, 182f), bottom)
        c.drawRoundRect(RectF(110f, 236f, 190f, 240f), 2f, 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = a }.shadowed())
        val date = fmt("EEE").uppercase(Locale.ENGLISH) + " · " + fmt("d MMM").uppercase(Locale.ENGLISH)
        val d = label(small, b, 24f, .12f); fit(d, date, 240f)
        c.drawText(date, 150f, 274f, d)
    }

    // ── B · stacked on the left, date running down the right edge ─────────
    private fun drawSide(c: Canvas, f: Typeface, small: Typeface, hh: String, mm: String, a: Int, b: Int, outline: Boolean) {
        val top = digits(f, a, 116f, outline); fit(top, "88", 180f, 96f)
        val bottom = digits(f, b, top.textSize, outline)
        c.drawText(hh, 118f, baseline(top, hh, 92f), top)
        c.drawText(mm, 118f, baseline(bottom, mm, 208f), bottom)
        c.drawLine(236f, 40f, 236f, 260f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = b; alpha = 90; strokeWidth = 2f
        })
        val date = fmt("EEEE").uppercase(Locale.ENGLISH) + " " + fmt("d MMM").uppercase(Locale.ENGLISH)
        val d = label(small, a, 26f, .15f); fit(d, date, 230f)
        c.save()
        c.rotate(90f, 264f, 150f)
        c.drawText(date, 264f, 150f - (d.descent() + d.ascent()) / 2f, d)
        c.restore()
    }

    // ── C · accent date badge on top, time underneath ──────────────────────
    private fun drawBadge(c: Canvas, f: Typeface, small: Typeface, hh: String, mm: String, a: Int, b: Int, outline: Boolean) {
        c.drawRoundRect(RectF(28f, 26f, 272f, 112f), 24f, 24f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = a }.shadowed())
        val day = label(small, INK_ON_ACCENT, 60f, 0f); fit(day, "88", 100f)
        c.drawText(fmt("dd"), 88f, baseline(day, fmt("dd"), 69f), day)
        val wd = label(small, INK_ON_ACCENT, 26f, .12f); fit(wd, "WED", 120f)
        c.drawText(fmt("EEE").uppercase(Locale.ENGLISH), 200f, 64f, wd)
        val month = fmt("MMMM").uppercase(Locale.ENGLISH)
        val mo = label(small, INK_ON_ACCENT, 22f, .08f); fit(mo, month, 128f)
        c.drawText(month, 200f, 96f, mo)

        val time = "$hh:$mm"
        val t = digits(f, b, 96f, outline); fit(t, time, 250f, 110f)
        c.drawText(time, 150f, baseline(t, time, 206f), t)
    }

    fun description(context: Context): String =
        SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm, EEEE d MMMM" else "h:mm a, EEEE d MMMM",
            Locale.getDefault()).format(Calendar.getInstance().time)
}
