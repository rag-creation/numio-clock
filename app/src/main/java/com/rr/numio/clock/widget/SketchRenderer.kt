package com.rr.numio.clock.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import com.rr.numio.clock.R
import kotlin.random.Random

/**
 * Sketch poster styles, with hand-drawn digits and letters (hand-drawn by RR):
 *  - Sketch: cream paper card with a wobbly ink border
 *  - Sketch Clear: no card, white ink with a soft shadow, sits on any wallpaper
 *
 * Glyphs are white PNGs with alpha in drawable-nodpi and are tinted here,
 * so the time follows the widget accent colour.
 */
object SketchRenderer {

    private const val PAPER = 0xFFFBF3E2.toInt()
    private const val INK = 0xFF1C1A17.toInt()
    private const val FIBER = 0x33A08A5C
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val SHADOW = 0x8C000000.toInt()

    // Listed by id (not looked up by name) so resource shrinking keeps them.
    private val DIGITS = intArrayOf(
        R.drawable.sketch_digit_0, R.drawable.sketch_digit_1, R.drawable.sketch_digit_2,
        R.drawable.sketch_digit_3, R.drawable.sketch_digit_4, R.drawable.sketch_digit_5,
        R.drawable.sketch_digit_6, R.drawable.sketch_digit_7, R.drawable.sketch_digit_8,
        R.drawable.sketch_digit_9
    )
    private val LETTERS = intArrayOf(
        R.drawable.sketch_letter_a, R.drawable.sketch_letter_b, R.drawable.sketch_letter_c,
        R.drawable.sketch_letter_d, R.drawable.sketch_letter_e, R.drawable.sketch_letter_f,
        R.drawable.sketch_letter_g, R.drawable.sketch_letter_h, R.drawable.sketch_letter_i,
        R.drawable.sketch_letter_j, R.drawable.sketch_letter_k, R.drawable.sketch_letter_l,
        R.drawable.sketch_letter_m, R.drawable.sketch_letter_n, R.drawable.sketch_letter_o,
        R.drawable.sketch_letter_p, R.drawable.sketch_letter_q, R.drawable.sketch_letter_r,
        R.drawable.sketch_letter_s, R.drawable.sketch_letter_t, R.drawable.sketch_letter_u,
        R.drawable.sketch_letter_v, R.drawable.sketch_letter_w, R.drawable.sketch_letter_x,
        R.drawable.sketch_letter_y, R.drawable.sketch_letter_z
    )

    private val cache = HashMap<Int, Bitmap>()

    private fun glyph(context: Context, c: Char): Bitmap? {
        val id = when (c) {
            in '0'..'9' -> DIGITS[c - '0']
            in 'A'..'Z' -> LETTERS[c - 'A']
            else -> return null
        }
        return cache.getOrPut(id) {
            BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })
        }
    }

    // ── text made of glyph images ───────────────────────────────────────────
    private fun gap(h: Float) = h * 0.07f
    private fun spaceW(h: Float) = h * 0.38f
    private fun colonW(h: Float) = h * 0.30f

    private fun measure(context: Context, text: String, h: Float): Float {
        var w = 0f
        text.forEachIndexed { i, c ->
            w += when (c) {
                ' ' -> spaceW(h)
                ':' -> colonW(h)
                else -> glyph(context, c)?.let { it.width * h / it.height } ?: 0f
            }
            if (i < text.lastIndex) w += gap(h)
        }
        return w
    }

    /** Draws [text] with its bottom edge on [bottom]. Returns the width used. */
    private fun drawText(
        context: Context, canvas: Canvas, text: String,
        x: Float, bottom: Float, h: Float, color: Int, shadow: Boolean = false
    ): Float {
        if (shadow) {
            // Soft drop shadow: same text, dark, nudged down-right, drawn first
            val o = (h * 0.03f).coerceAtLeast(1.5f)
            drawText(context, canvas, text, x + o * 0.5f, bottom + o, h, SHADOW, false)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        var cx = x
        text.forEachIndexed { i, c ->
            when (c) {
                ' ' -> cx += spaceW(h)
                ':' -> {
                    // two slightly uneven inked dots
                    val r = h * 0.055f
                    val mx = cx + colonW(h) / 2f
                    canvas.drawCircle(mx, bottom - h * 0.66f, r, dot)
                    canvas.drawCircle(mx + r * 0.3f, bottom - h * 0.68f, r * 0.7f, dot)
                    canvas.drawCircle(mx - r * 0.1f, bottom - h * 0.24f, r * 1.05f, dot)
                    canvas.drawCircle(mx + r * 0.35f, bottom - h * 0.22f, r * 0.6f, dot)
                    cx += colonW(h)
                }
                else -> glyph(context, c)?.let { g ->
                    val gw = g.width * h / g.height
                    canvas.drawBitmap(g, null, RectF(cx, bottom - h, cx + gw, bottom), paint)
                    cx += gw
                }
            }
            if (i < text.lastIndex) cx += gap(h)
        }
        return cx - x
    }

    // ── the widget ──────────────────────────────────────────────────────────
    fun draw(
        context: Context, canvas: Canvas, w: Int, h: Int, accent: Int,
        time: String, day: String, date: String,
        boxed: Boolean = true
    ) {
        val stroke = (h * 0.013f).coerceAtLeast(2f)
        val inset = stroke * 2.5f
        val card = RectF(inset, inset, w - inset, h - inset)
        val cw = card.width()
        val ch = card.height()
        val radius = ch * 0.09f
        val rnd = Random(42)

        val inkColor = if (boxed) INK else WHITE
        val shadow = !boxed
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor; style = Paint.Style.STROKE; strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            if (shadow) setShadowLayer(stroke * 1.2f, stroke * 0.4f, stroke * 0.8f, SHADOW)
        }

        if (boxed) {
            // Paper
            canvas.drawRoundRect(card, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PAPER })
            val fiber = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = FIBER; strokeWidth = stroke * 0.35f; strokeCap = Paint.Cap.ROUND
            }
            canvas.save()
            canvas.clipRect(card)
            repeat(70) {
                val fx = card.left + rnd.nextFloat() * cw
                val fy = card.top + rnd.nextFloat() * ch
                val len = ch * (0.01f + rnd.nextFloat() * 0.03f)
                val ang = rnd.nextFloat() * Math.PI.toFloat()
                canvas.drawLine(fx, fy, fx + len * kotlin.math.cos(ang), fy + len * kotlin.math.sin(ang), fiber)
            }
            canvas.restore()

            // Double wobbly ink border
            canvas.drawPath(wobblyRect(card, radius, stroke * 0.8f, rnd), ink)
            ink.strokeWidth = stroke * 0.6f
            val inner = RectF(card).apply { inset(stroke * 1.4f, stroke * 1.2f) }
            canvas.drawPath(wobblyRect(inner, radius * 0.95f, stroke * 1.1f, rnd), ink)
        }

        // On cream paper the accent is darkened a little so it reads; on a wallpaper it's used as-is
        val gold = if (boxed) darken(accent, 0.22f) else accent
        val pad = ch * 0.11f

        // Sizes, shrunk until everything fits
        var th = ch * 0.44f
        var lh = ch * 0.15f
        val dateW: () -> Float = { maxOf(measure(context, day, lh), measure(context, date, lh)) }
        while (dateW() > cw * 0.62f && lh > 6f) lh *= 0.93f
        while (measure(context, time, th) > cw - pad * 2 && th > 10f) th *= 0.95f

        val dateBottom = card.bottom - pad - lh * 0.45f
        val dayBottom = dateBottom - lh * 1.3f
        val dayTop = dayBottom - lh
        val dateLeft = card.right - pad - dateW()
        // If the time would run into the date block, shrink it
        while (th > 10f &&
            card.left + pad + measure(context, time, th) > dateLeft - pad * 0.3f &&
            card.top + pad + th > dayTop - pad * 0.2f
        ) th *= 0.95f

        // Time
        drawText(context, canvas, time, card.left + pad, card.top + pad + th, th, gold, shadow)

        // Day + date, right aligned
        val right = card.right - pad
        drawText(context, canvas, day, right - measure(context, day, lh), dayBottom, lh, inkColor, shadow)
        val dw = measure(context, date, lh)
        drawText(context, canvas, date, right - dw, dateBottom, lh, inkColor, shadow)

        // Hand-drawn underline + little dash
        ink.strokeWidth = stroke * 1.1f
        val uy = dateBottom + lh * 0.30f
        val ux0 = right - dw - lh * 0.15f
        val ux1 = right - lh * 0.9f
        canvas.drawPath(Path().apply {
            moveTo(ux0, uy + lh * 0.04f)
            quadTo((ux0 + ux1) / 2f, uy - lh * 0.06f, ux1, uy)
        }, ink)
        ink.strokeWidth = stroke * 0.8f
        canvas.drawLine(ux1 + lh * 0.2f, uy - lh * 0.02f, right, uy - lh * 0.06f, ink)

        // Brand, bottom left
        val bh = lh * 0.62f
        val bw = drawText(context, canvas, "NUMIO", card.left + pad, card.bottom - pad * 0.95f, bh, inkColor, shadow)

        // Accent doodles: specks, a sparkle and a pin
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            if (shadow) setShadowLayer(stroke, stroke * 0.3f, stroke * 0.6f, SHADOW)
        }
        val r = ch * 0.012f
        canvas.drawCircle(card.left + pad + bw + r * 3f, card.bottom - pad * 0.95f - bh * 0.2f, r, accentPaint)
        canvas.drawCircle(dateLeft - lh * 0.5f, dayTop + lh * 0.2f, r * 1.2f, accentPaint)
        canvas.drawCircle(dateLeft - lh * 0.2f, dayTop - lh * 0.15f, r * 0.8f, accentPaint)
        canvas.drawCircle(right - lh * 0.3f, dateBottom + lh * 0.75f, r, accentPaint)

        ink.strokeWidth = stroke * 0.7f
        val sx = card.right - pad * 0.55f
        val sy = card.bottom - pad * 1.0f
        val s = ch * 0.025f
        canvas.drawLine(sx - s, sy, sx + s, sy, ink)
        canvas.drawLine(sx, sy - s * 1.3f, sx, sy + s * 1.3f, ink)

        if (boxed) {
            ink.strokeWidth = stroke * 0.9f
            canvas.drawCircle(card.right - pad * 0.55f, card.top + pad * 0.6f, ch * 0.018f, ink)
        }
    }

    /** Rounded rectangle drawn as a slightly shaky hand-drawn line. */
    private fun wobblyRect(r: RectF, radius: Float, wobble: Float, rnd: Random): Path {
        val src = Path().apply { addRoundRect(r, radius, radius, Path.Direction.CW) }
        val pm = android.graphics.PathMeasure(src, true)
        val len = pm.length
        val steps = 90
        val pos = FloatArray(2)
        val out = Path()
        var drift = 0f
        for (i in 0..steps) {
            pm.getPosTan(len * i / steps, pos, null)
            drift = (drift + (rnd.nextFloat() - 0.5f) * wobble * 0.6f).coerceIn(-wobble, wobble)
            val x = pos[0] + drift
            val y = pos[1] + drift * 0.6f
            if (i == 0) out.moveTo(x, y) else out.lineTo(x, y)
        }
        return out
    }

    private fun darken(c: Int, amount: Float): Int {
        val k = 1f - amount
        return Color.argb(
            255,
            (Color.red(c) * k).toInt(),
            (Color.green(c) * k).toInt(),
            (Color.blue(c) * k).toInt()
        )
    }
}
