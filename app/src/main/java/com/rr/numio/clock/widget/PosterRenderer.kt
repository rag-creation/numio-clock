package com.rr.numio.clock.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.core.content.res.ResourcesCompat
import com.rr.numio.clock.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Draws the Poster widget as an image, so it can use any font
 * (launchers don't let widgets load fonts bundled in the app).
 *
 * Layout (Numio style):
 *   01   ← amber hours
 *   ━━   ← amber underline (like the calculator display)
 *   41   ← white minutes          SATURDAY
 *                                 3 OCTOBER
 *                                 ● ● ●  NUMIO
 */
object PosterRenderer {

    private const val AMBER = 0xFFF5C427.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val MUTED = 0xCCB0B0B0.toInt()
    private const val SHADOW = 0x99000000.toInt()

    fun render(context: Context, width: Int, height: Int): Bitmap {
        val w = width.coerceAtLeast(100)
        val h = height.coerceAtLeast(60)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val digitFont = ResourcesCompat.getFont(context, R.font.permanent_marker) ?: Typeface.DEFAULT_BOLD
        val labelFont = ResourcesCompat.getFont(context, R.font.bangers) ?: Typeface.DEFAULT_BOLD

        val now = Calendar.getInstance()
        val is24 = DateFormat.is24HourFormat(context)
        val hour = SimpleDateFormat(if (is24) "HH" else "hh", Locale.getDefault()).format(now.time)
        val minute = SimpleDateFormat("mm", Locale.getDefault()).format(now.time)
        val day = SimpleDateFormat("EEEE", Locale.getDefault()).format(now.time).uppercase()
        val date = SimpleDateFormat("d MMMM", Locale.getDefault()).format(now.time).uppercase()

        fun paint(font: Typeface, color: Int, size: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            this.color = color
            textSize = size
            setShadowLayer(size * 0.06f, 0f, size * 0.02f, SHADOW)
        }

        // Start from height-based sizes, then shrink if it's too wide
        var digit = h * 0.40f
        val pad = h * 0.08f

        fun neededWidth(d: Float): Float {
            val dp = paint(digitFont, AMBER, d)
            val digitsW = maxOf(dp.measureText(hour), dp.measureText(minute))
            val lp = paint(labelFont, WHITE, d * 0.34f)
            val datep = paint(labelFont, AMBER, d * 0.28f)
            val rightW = maxOf(lp.measureText(day), datep.measureText(date), d * 0.9f)
            return digitsW + d * 0.35f + rightW
        }

        val available = w - pad * 2
        val needed = neededWidth(digit)
        if (needed > available) digit *= available / needed

        val digitPaint = paint(digitFont, AMBER, digit)
        val minutePaint = paint(digitFont, WHITE, digit)
        val dayPaint = paint(labelFont, WHITE, digit * 0.34f).apply { letterSpacing = 0.04f }
        val datePaint = paint(labelFont, AMBER, digit * 0.28f).apply { letterSpacing = 0.04f }
        val brandPaint = paint(labelFont, MUTED, digit * 0.16f).apply { letterSpacing = 0.25f }

        val digitsW = maxOf(digitPaint.measureText(hour), digitPaint.measureText(minute))
        val blockH = digit * 2.05f
        val top = (h - blockH) / 2f
        val x = pad

        // Hours
        val hoursBaseline = top + digit * 0.88f
        canvas.drawText(hour, x, hoursBaseline, digitPaint)

        // Amber underline between hours and minutes
        val lineY = hoursBaseline + digit * 0.10f
        val lineH = (digit * 0.06f).coerceAtLeast(3f)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AMBER }
        canvas.drawRoundRect(RectF(x, lineY, x + digitsW * 0.75f, lineY + lineH), lineH, lineH, linePaint)

        // Minutes
        val minutesBaseline = lineY + lineH + digit * 0.92f
        canvas.drawText(minute, x, minutesBaseline, minutePaint)

        // Right column, vertically centred on the digit block
        val rx = x + digitsW + digit * 0.35f
        val midY = top + blockH / 2f
        canvas.drawText(day, rx, midY - digit * 0.08f, dayPaint)
        canvas.drawText(date, rx, midY + digit * 0.30f, datePaint)

        // Three amber dots + NUMIO
        val dotR = digit * 0.045f
        val dotsY = midY + digit * 0.62f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AMBER }
        for (i in 0 until 3) canvas.drawCircle(rx + dotR + i * dotR * 3.2f, dotsY - dotR, dotR, dotPaint)
        canvas.drawText("NUMIO", rx + dotR * 10.5f, dotsY, brandPaint)

        return bmp
    }

    /** For accessibility (TalkBack reads this). */
    fun description(context: Context): String {
        val now = Calendar.getInstance()
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm, EEEE d MMMM" else "h:mm a, EEEE d MMMM"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(now.time)
    }
}
