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

/** Poster style + widget accent colour, saved for the widgets to read. */
object PosterStyles {
    private const val PREFS = "numio_prefs"
    private const val KEY_STYLE = "poster_style"
    private const val KEY_ACCENT = "widget_accent"
    const val DEFAULT_ACCENT = 0xFFF5C427.toInt()

    /** key to label shown in Settings */
    val options = listOf(
        "marker" to "Marker",
        "bold" to "Bold",
        "sketch" to "Sketch",
        "sketch_clear" to "Sketch Clear",
        "bubble" to "Bubble",
        "bubble_clear" to "Bubble Clear",
        "sadhya" to "Sadhya",
        "bloom" to "Bloom",
        "bloom_clear" to "Bloom Clear"
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Saved style; anything no longer offered (e.g. the old "script") falls back to Marker. */
    fun current(context: Context): String {
        val saved = prefs(context).getString(KEY_STYLE, "marker") ?: "marker"
        return if (options.any { it.first == saved }) saved else "marker"
    }

    fun save(context: Context, key: String) {
        prefs(context).edit().putString(KEY_STYLE, key).apply()
        ClockWidget.updateAll(context)
    }

    fun accent(context: Context): Int = prefs(context).getInt(KEY_ACCENT, DEFAULT_ACCENT)

    /** Call when the user applies a new accent colour in Settings. */
    fun saveAccent(context: Context, argb: Int) {
        prefs(context).edit().putInt(KEY_ACCENT, argb).apply()
        ClockWidget.updateAll(context)
    }
}

/**
 * Draws the Poster widget as an image, so it can use any font
 * (launchers don't let widgets load fonts bundled in the app).
 */
object PosterRenderer {

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val DARK = 0xFF111111.toInt()
    private const val MUTED = 0xCCB0B0B0.toInt()
    private const val SHADOW = 0x99000000.toInt()

    fun render(
        context: Context,
        width: Int,
        height: Int,
        style: String = PosterStyles.current(context)
    ): Bitmap {
        val w = width.coerceAtLeast(100)
        val h = height.coerceAtLeast(60)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val accent = PosterStyles.accent(context)
        when (style) {
            "bold" -> drawBold(context, canvas, w, h, accent)
            "sketch", "sketch_clear" -> SketchRenderer.draw(
                context, canvas, w, h, accent,
                // English + Latin digits: the sketch glyphs only cover A-Z and 0-9
                time = fmtEn(if (is24(context)) "HH:mm" else "hh:mm"),
                day = fmtEn("EEEE").uppercase(Locale.ENGLISH),
                date = fmtEn("MMMM dd").uppercase(Locale.ENGLISH),
                boxed = style == "sketch"
            )
            "bubble", "bubble_clear" -> BubbleRenderer.draw(
                context, canvas, w, h, accent,
                time = fmtEn(if (is24(context)) "HH:mm" else "hh:mm"),
                date = fmtEn("EEEE").uppercase(Locale.ENGLISH) + " · " +
                    fmtEn("d MMMM").uppercase(Locale.ENGLISH),
                boxed = style == "bubble"
            )
            "sadhya" -> SadhyaRenderer.draw(
                context, canvas, w, h,
                time = fmtEn(if (is24(context)) "HH:mm" else "hh:mm"),
                date = fmtEn("EEEE").uppercase(Locale.ENGLISH) + " · " +
                    fmtEn("d MMMM").uppercase(Locale.ENGLISH)
            )
            "bloom", "bloom_clear" -> BloomRenderer.draw(
                context, canvas, w, h, accent,
                time = fmtEn(if (is24(context)) "HH:mm" else "hh:mm"),
                date = fmtEn("EEEE").uppercase(Locale.ENGLISH) + " · " +
                    fmtEn("d MMMM").uppercase(Locale.ENGLISH),
                boxed = style == "bloom"
            )
            else -> drawMarker(context, canvas, w, h, accent)
        }
        return bmp
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private fun font(context: Context, id: Int) =
        ResourcesCompat.getFont(context, id) ?: Typeface.DEFAULT_BOLD

    private fun paint(font: Typeface, color: Int, size: Float, shadow: Boolean = true) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            this.color = color
            textSize = size
            if (shadow) setShadowLayer(size * 0.06f, 0f, size * 0.02f, SHADOW)
        }

    private fun fmt(pattern: String) =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Calendar.getInstance().time)

    private fun fmtEn(pattern: String) =
        SimpleDateFormat(pattern, Locale.ENGLISH).format(Calendar.getInstance().time)

    private fun is24(context: Context) = DateFormat.is24HourFormat(context)

    // ── 1. MARKER: stacked amber hours / underline / white minutes ──────────
    private fun drawMarker(context: Context, canvas: Canvas, w: Int, h: Int, accent: Int) {
        val digitFont = font(context, R.font.permanent_marker)
        val labelFont = font(context, R.font.bangers)

        val hour = fmt(if (is24(context)) "HH" else "hh")
        val minute = fmt("mm")
        val day = fmt("EEEE").uppercase()
        val date = fmt("d MMMM").uppercase()

        var digit = h * 0.40f
        val pad = h * 0.08f

        fun neededWidth(d: Float): Float {
            val dp = paint(digitFont, accent, d)
            val digitsW = maxOf(dp.measureText(hour), dp.measureText(minute))
            val rightW = maxOf(
                paint(labelFont, WHITE, d * 0.34f).measureText(day),
                paint(labelFont, accent, d * 0.28f).measureText(date),
                d * 0.9f
            )
            return digitsW + d * 0.35f + rightW
        }
        val needed = neededWidth(digit)
        if (needed > w - pad * 2) digit *= (w - pad * 2) / needed

        val hourPaint = paint(digitFont, accent, digit)
        val minutePaint = paint(digitFont, WHITE, digit)
        val dayPaint = paint(labelFont, WHITE, digit * 0.34f).apply { letterSpacing = 0.04f }
        val datePaint = paint(labelFont, accent, digit * 0.28f).apply { letterSpacing = 0.04f }
        val brandPaint = paint(labelFont, MUTED, digit * 0.16f).apply { letterSpacing = 0.25f }

        val digitsW = maxOf(hourPaint.measureText(hour), hourPaint.measureText(minute))
        val blockH = digit * 2.05f
        val top = (h - blockH) / 2f
        val x = pad

        val hoursBaseline = top + digit * 0.88f
        canvas.drawText(hour, x, hoursBaseline, hourPaint)

        val lineY = hoursBaseline + digit * 0.10f
        val lineH = (digit * 0.06f).coerceAtLeast(3f)
        val solid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRoundRect(RectF(x, lineY, x + digitsW * 0.75f, lineY + lineH), lineH, lineH, solid)

        canvas.drawText(minute, x, lineY + lineH + digit * 0.92f, minutePaint)

        val rx = x + digitsW + digit * 0.35f
        val midY = top + blockH / 2f
        canvas.drawText(day, rx, midY - digit * 0.08f, dayPaint)
        canvas.drawText(date, rx, midY + digit * 0.30f, datePaint)

        val dotR = digit * 0.045f
        val dotsY = midY + digit * 0.62f
        for (i in 0 until 3) canvas.drawCircle(rx + dotR + i * dotR * 3.2f, dotsY - dotR, dotR, solid)
        canvas.drawText("NUMIO", rx + dotR * 10.5f, dotsY, brandPaint)
    }

    // ── 3. BOLD: huge comic time, date in an accent pill ─────────────────────
    private fun drawBold(context: Context, canvas: Canvas, w: Int, h: Int, accent: Int) {
        val boldFont = font(context, R.font.bangers)
        val hour = fmt(if (is24(context)) "HH" else "hh")
        val minute = fmt("mm")
        val pillText = (fmt("EEE") + "  ·  " + fmt("d MMM")).uppercase()

        var size = h * 0.66f
        val pad = h * 0.08f

        fun neededWidth(s: Float): Float {
            val p = paint(boldFont, WHITE, s).apply { letterSpacing = 0.02f }
            return p.measureText("$hour:$minute")
        }
        val needed = neededWidth(size)
        if (needed > w - pad * 2) size *= (w - pad * 2) / needed

        val hourPaint = paint(boldFont, accent, size).apply { letterSpacing = 0.02f }
        val restPaint = paint(boldFont, WHITE, size).apply { letterSpacing = 0.02f }
        val pillPaint = paint(boldFont, DARK, size * 0.20f, shadow = false).apply { letterSpacing = 0.08f }

        val pillH = size * 0.34f
        val blockH = size * 0.80f + size * 0.10f + pillH
        val top = (h - blockH) / 2f
        val x = pad
        val baseline = top + size * 0.80f

        canvas.drawText(hour, x, baseline, hourPaint)
        val colonX = x + hourPaint.measureText(hour)
        canvas.drawText(":", colonX, baseline, restPaint)
        canvas.drawText(minute, colonX + restPaint.measureText(":"), baseline, restPaint)

        // Pill
        val pillTop = baseline + size * 0.10f
        val textW = pillPaint.measureText(pillText)
        val pillW = textW + pillH * 1.2f
        val solid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRoundRect(RectF(x, pillTop, x + pillW, pillTop + pillH), pillH / 2f, pillH / 2f, solid)
        val textBaseline = pillTop + pillH / 2f - (pillPaint.descent() + pillPaint.ascent()) / 2f
        canvas.drawText(pillText, x + pillH * 0.6f, textBaseline, pillPaint)
    }

    /** For accessibility (TalkBack reads this). */
    fun description(context: Context): String {
        val pattern = if (is24(context)) "HH:mm, EEEE d MMMM" else "h:mm a, EEEE d MMMM"
        return fmt(pattern)
    }
}
