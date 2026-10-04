package com.rr.numio.clock.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.rr.numio.clock.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Bubble poster style: glossy balloon time (Baloo 2, drawn with outline,
 * fill, shine and shade in code so it follows the accent colour),
 * one date line underneath, and a few pen doodles.
 * Bubble Clear: no card; everything gets a sticker-style outline so it
 * stands out on any wallpaper.
 */
object BubbleRenderer {

    private const val CARD = 0xFFFDFBF6.toInt()
    private const val NAVY = 0xFF1E2B5C.toInt()
    private const val PINK = 0xFFE9739A.toInt()
    private const val SHINE = 0xB3FFFFFF.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    private fun font(context: Context): Typeface =
        ResourcesCompat.getFont(context, R.font.baloo2_extrabold) ?: Typeface.DEFAULT_BOLD

    private fun textPaint(font: Typeface, size: Float, color: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font; textSize = size; this.color = color
        }

    fun draw(
        context: Context, canvas: Canvas, w: Int, h: Int, accent: Int,
        time: String, date: String,
        boxed: Boolean = true
    ) {
        val f = font(context)
        val stroke = (h * 0.012f).coerceAtLeast(2f)
        val rnd = Random(7)

        val card = RectF(stroke * 2.5f, stroke * 2.5f, w - stroke * 2.5f, h - stroke * 2.5f)
        val cw = card.width()
        val ch = card.height()
        val radius = ch * 0.11f

        val navyLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = NAVY; style = Paint.Style.STROKE; strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val pinkLine = Paint(navyLine).apply { color = PINK; strokeWidth = stroke * 0.7f }
        val navyFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = NAVY }
        // On a wallpaper, navy ink turns white and everything gets a navy edge
        val inkColor = if (boxed) NAVY else WHITE

        if (boxed) {
            // Card + sketchy double border
            canvas.drawRoundRect(card, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD })
            canvas.drawPath(wobblyRect(card, radius, stroke * 0.9f, rnd), navyLine)
            navyLine.strokeWidth = stroke * 0.55f
            canvas.drawPath(wobblyRect(RectF(card).apply { inset(stroke * 1.3f, stroke * 1.1f) }, radius * 0.95f, stroke, rnd), navyLine)
        }

        // ── Layout: time, date and NUMIO stacked as one centred block ─────
        val parts = date.split(" · ")
        val dotGap = { sz: Float -> sz * 0.9f }
        fun datePaint(sz: Float) = textPaint(f, sz, inkColor).apply { letterSpacing = 0.06f }
        fun dateW(sz: Float): Float {
            val p = datePaint(sz)
            return parts.sumOf { p.measureText(it).toDouble() }.toFloat() + dotGap(sz) * (parts.size - 1)
        }
        fun capH(p: Paint, text: String): Float {
            val r = android.graphics.Rect()
            p.getTextBounds(text, 0, text.length, r)
            return r.height().toFloat()
        }

        var dSize = ch * 0.12f
        while (dateW(dSize) > cw * 0.80f && dSize > 6f) dSize *= 0.94f
        val dp = datePaint(dSize)
        val dateCap = capH(dp, "SUNDAY")
        val bSize = dSize * 0.55f
        val bp = textPaint(f, bSize, inkColor).apply { letterSpacing = 0.3f; if (boxed) alpha = 0xB3 }
        val brandCap = capH(bp, "NUMIO")
        val gapTimeDate = dSize * 0.75f
        val gapDateBrand = dSize * 0.6f
        val belowTime = gapTimeDate + dateCap + gapDateBrand + brandCap

        // Time as big as fits: 82% of the width and whatever height is left
        val maxBlockH = ch * 0.80f
        var tSize = ch
        val timePaint = textPaint(f, tSize, accent)
        while (tSize > 10f &&
            (timePaint.measureText(time) > cw * 0.82f || capH(timePaint, time) + belowTime > maxBlockH)
        ) { tSize *= 0.96f; timePaint.textSize = tSize }
        val timeBounds = android.graphics.Rect()
        timePaint.getTextBounds(time, 0, time.length, timeBounds)

        val blockH = timeBounds.height() + belowTime
        val blockTop = card.centerY() - blockH / 2f
        val tBase = blockTop - timeBounds.top
        val dateBase = tBase + timeBounds.bottom + gapTimeDate + dateCap
        val brandBase = dateBase + gapDateBrand + brandCap

        // ── Big bubble time ───────────────────────────────────────────────
        val tX = card.centerX() - timePaint.measureText(time) / 2f
        drawBubble(canvas, timePaint, time, tX, tBase, tSize, accent, halo = !boxed)

        // ── Date line: "SUNDAY · 4 OCTOBER" (dot in accent) ──────────────
        val dpEdge = Paint(dp).apply {
            color = NAVY; style = Paint.Style.STROKE; strokeWidth = dSize * 0.18f
            strokeJoin = Paint.Join.ROUND
        }
        var dx = card.centerX() - dateW(dSize) / 2f
        val accentFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        parts.forEachIndexed { i, part ->
            if (!boxed) canvas.drawText(part, dx, dateBase, dpEdge)
            canvas.drawText(part, dx, dateBase, dp)
            dx += dp.measureText(part)
            if (i < parts.lastIndex) {
                val r = dSize * 0.14f
                val cx = dx + dotGap(dSize) / 2f
                val cy = dateBase - dateCap / 2f
                canvas.drawCircle(cx, cy, r * (if (boxed) 1.35f else 1.7f), navyFill)
                canvas.drawCircle(cx, cy, r, accentFill)
                dx += dotGap(dSize)
            }
        }

        // ── Brand, small and centred under the date ──────────────────────
        val bx = card.centerX() - bp.measureText("NUMIO") / 2f
        if (!boxed) canvas.drawText("NUMIO", bx, brandBase, Paint(bp).apply {
            color = NAVY; style = Paint.Style.STROKE; strokeWidth = bSize * 0.2f; strokeJoin = Paint.Join.ROUND
        })
        canvas.drawText("NUMIO", bx, brandBase, bp)

        // ── Doodles ──────────────────────────────────────────────────────
        val s = ch * 0.045f
        navyLine.strokeWidth = stroke * 0.7f
        if (!boxed) navyLine.color = WHITE
        val edge = Paint(navyLine).apply { color = NAVY; strokeWidth = stroke * 2.0f }
        fun doodle(p: Path, paint: Paint) {
            if (!boxed) canvas.drawPath(p, edge)
            canvas.drawPath(p, paint)
        }
        fun speck(x: Float, y: Float, r: Float) {
            if (!boxed) canvas.drawCircle(x, y, r * 1.8f, navyFill)
            canvas.drawCircle(x, y, r, if (boxed) navyFill else Paint(navyFill).apply { color = WHITE })
        }
        doodle(star(card.left + cw * 0.06f, card.top + ch * 0.20f, s), pinkLine)
        doodle(star(card.left + cw * 0.045f, card.top + ch * 0.55f, s * 0.8f), navyLine)
        doodle(star(card.right - cw * 0.06f, card.top + ch * 0.22f, s * 1.1f), navyLine)
        speck(card.left + cw * 0.09f, card.top + ch * 0.36f, s * 0.2f)
        speck(card.right - cw * 0.05f, card.top + ch * 0.50f, s * 0.2f)
        doodle(squiggle(card.right - cw * 0.085f, card.top + ch * 0.36f, s), pinkLine)
        // little sun, bottom right
        val sunX = card.right - cw * 0.065f
        val sunY = card.bottom - ch * 0.12f
        val sun = Path().apply {
            addCircle(sunX, sunY, s * 0.45f, Path.Direction.CW)
            for (i in 0 until 7) {
                val a = (i / 7f) * 2f * PI.toFloat() - PI.toFloat() / 2f
                moveTo(sunX + cos(a) * s * 0.75f, sunY + sin(a) * s * 0.75f)
                lineTo(sunX + cos(a) * s * 1.15f, sunY + sin(a) * s * 1.15f)
            }
        }
        doodle(sun, pinkLine)
    }

    /** Glossy balloon text: navy outline, accent fill, darker bottom shade, white top shine. */
    private fun drawBubble(
        canvas: Canvas, base: Paint, text: String, x: Float, y: Float, size: Float, accent: Int,
        halo: Boolean = false
    ) {
        val path = Path()
        base.getTextPath(text, 0, text.length, x, y, path)

        // Sticker edge: white ring outside the navy outline (only without the card)
        if (halo) canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WHITE; style = Paint.Style.STROKE; strokeWidth = size * 0.13f
            strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
        })

        // Outline (drawn wide, the fill covers the inner half)
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = NAVY; style = Paint.Style.STROKE; strokeWidth = size * 0.07f
            strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
        })
        // Fill
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })

        // Shade: inner bottom-right rim
        val d = size * 0.032f
        val shade = Path(path).apply { op(shifted(path, -d, -d), Path.Op.DIFFERENCE) }
        canvas.drawPath(shade, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mix(accent, NAVY, 0.22f) })

        // Shine: thin inner top-left rim, then pulled in a little from the edge
        val e = size * 0.03f
        val rim = Path(path).apply { op(shifted(path, e, e), Path.Op.DIFFERENCE) }
        val shine = Path(shifted(rim, e * 0.9f, e * 0.9f)).apply { op(path, Path.Op.INTERSECT) }
        canvas.drawPath(shine, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SHINE })

        // Thin navy line on top so edges stay crisp
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = NAVY; style = Paint.Style.STROKE; strokeWidth = size * 0.012f
        })
    }

    private fun shifted(p: Path, dx: Float, dy: Float) =
        Path(p).apply { transform(Matrix().apply { setTranslate(dx, dy) }) }

    private fun mix(a: Int, b: Int, t: Float) = Color.argb(
        255,
        (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt()
    )

    // ── doodle shapes ───────────────────────────────────────────────────────
    private fun star(cx: Float, cy: Float, r: Float) = Path().apply {
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) r else r * 0.45f
            val a = i * PI.toFloat() / 5f - PI.toFloat() / 2f
            val px = cx + cos(a) * rr
            val py = cy + sin(a) * rr
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    private fun squiggle(x: Float, y: Float, s: Float) = Path().apply {
        moveTo(x, y)
        quadTo(x + s * 0.4f, y + s * 0.3f, x, y + s * 0.6f)
        quadTo(x - s * 0.4f, y + s * 0.9f, x, y + s * 1.2f)
    }

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
            if (i == 0) out.moveTo(pos[0] + drift, pos[1] + drift * 0.6f)
            else out.lineTo(pos[0] + drift, pos[1] + drift * 0.6f)
        }
        return out
    }
}
