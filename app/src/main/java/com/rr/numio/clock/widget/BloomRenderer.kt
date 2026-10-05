package com.rr.numio.clock.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.rr.numio.clock.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Bloom poster style: a bouquet (lavender, daisies, tulip, lotus, dandelion
 * and RR's spider lilies) growing inside a soft blob in the accent colour,
 * with the time and date beside it.
 *
 * Drawn on an 800 x 400 board and scaled to the widget, like Sadhya.
 * Bloom Clear: no blob; stems turn white and everything gets a soft shadow,
 * so it reads on any wallpaper.
 */
object BloomRenderer {

    private const val NAVY = 0xFF1E2B5C.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val LAVENDER = 0xFF9B86D6.toInt()
    private const val DAISY_CENTRE = 0xFFF7B500.toInt()

    private const val SHADOW = 0x8C000000.toInt()

    // Set at the start of each draw (draw is synchronized, previews render in parallel)
    private var clear = false
    /** Stems, leaves and seeds: navy on the blob, white on the wallpaper. */
    private val ink get() = if (clear) WHITE else NAVY

    private fun deg(d: Float) = d * PI.toFloat() / 180f

    private fun Paint.shadowed() = apply { if (clear) setShadowLayer(3f, 1f, 2f, SHADOW) }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }.shadowed()

    private fun line(width: Float, color: Int = ink) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = width
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }.shadowed()

    /** White petal with a navy outline. */
    private fun white(canvas: Canvas, p: Path, outline: Float = 2.4f) {
        canvas.drawPath(p, fill(WHITE))
        canvas.drawPath(p, line(outline, NAVY))   // petals keep their navy edge
    }

    /** A petal or leaf from (cx,cy) outwards at angle [d] degrees. */
    private fun petal(cx: Float, cy: Float, length: Float, width: Float, d: Float, pointed: Boolean): Path {
        val a = deg(d)
        val tx = cx + cos(a) * length; val ty = cy + sin(a) * length
        val nx = -sin(a); val ny = cos(a)
        val mx = cx + cos(a) * length * .55f; val my = cy + sin(a) * length * .55f
        val k = if (pointed) width * .9f else width
        val p1x = mx + nx * k; val p1y = my + ny * k
        val p2x = mx - nx * k; val p2y = my - ny * k
        return Path().apply {
            moveTo(cx, cy)
            if (pointed) {
                quadTo(p1x, p1y, tx, ty); quadTo(p2x, p2y, cx, cy)
            } else {
                cubicTo(p1x, p1y, tx + nx * width * .55f, ty + ny * width * .55f, tx, ty)
                cubicTo(tx - nx * width * .55f, ty - ny * width * .55f, p2x, p2y, cx, cy)
            }
            close()
        }
    }

    // ── entry point ─────────────────────────────────────────────────────────
    fun draw(
        context: Context, canvas: Canvas, w: Int, h: Int, accent: Int, time: String, date: String,
        boxed: Boolean = true
    ) = synchronized(this) {
        clear = !boxed
        val s = min(w / 800f, h / 400f)
        canvas.save()
        canvas.translate((w - 800f * s) / 2f, (h - 400f * s) / 2f)
        canvas.scale(s, s)

        if (boxed) canvas.drawPath(blob(), fill(accent))
        drawBouquet(canvas)
        drawText(context, canvas, time, date, accent)

        canvas.restore()
    }

    private fun blob() = Path().apply {
        moveTo(90f, 74f)
        cubicTo(150f, 30f, 260f, 48f, 360f, 40f); cubicTo(470f, 32f, 560f, 52f, 650f, 36f)
        cubicTo(730f, 24f, 782f, 80f, 772f, 150f); cubicTo(764f, 206f, 790f, 262f, 768f, 316f)
        cubicTo(744f, 374f, 660f, 378f, 570f, 370f); cubicTo(470f, 362f, 380f, 388f, 280f, 376f)
        cubicTo(190f, 366f, 110f, 384f, 62f, 334f); cubicTo(22f, 292f, 46f, 236f, 34f, 180f)
        cubicTo(24f, 126f, 42f, 100f, 90f, 74f); close()
    }

    private fun stem(canvas: Canvas, width: Float, build: Path.() -> Unit) =
        canvas.drawPath(Path().apply(build), line(width))

    private fun drawBouquet(canvas: Canvas) {
        // crossed V stems for the spider lilies, drawn first so they sit behind
        stem(canvas, 2.6f) { moveTo(150f, 112f); cubicTo(176f, 210f, 214f, 300f, 236f, 372f) }
        stem(canvas, 2.6f) { moveTo(404f, 186f); cubicTo(334f, 246f, 250f, 310f, 200f, 372f) }
        // the other stems, rising from the bottom-left
        stem(canvas, 3f) { moveTo(200f, 372f); cubicTo(210f, 300f, 240f, 230f, 278f, 160f); cubicTo(290f, 138f, 300f, 118f, 306f, 104f) }
        stem(canvas, 3f) { moveTo(150f, 372f); cubicTo(164f, 300f, 196f, 220f, 220f, 160f); cubicTo(228f, 140f, 234f, 124f, 236f, 112f) }
        stem(canvas, 3f) { moveTo(176f, 372f); cubicTo(200f, 320f, 230f, 284f, 268f, 262f); cubicTo(290f, 248f, 306f, 240f, 320f, 234f) }
        stem(canvas, 3f) { moveTo(128f, 372f); cubicTo(130f, 320f, 140f, 270f, 150f, 226f) }
        stem(canvas, 3f) { moveTo(214f, 372f); cubicTo(224f, 340f, 236f, 318f, 252f, 300f) }
        stem(canvas, 3f) { moveTo(100f, 372f); cubicTo(96f, 330f, 90f, 300f, 84f, 276f) }
        stem(canvas, 3f) { moveTo(236f, 372f); cubicTo(256f, 356f, 276f, 346f, 300f, 340f) }

        listOf(
            floatArrayOf(160f, 300f, 44f, -150f), floatArrayOf(176f, 262f, 40f, -30f),
            floatArrayOf(214f, 320f, 40f, -120f), floatArrayOf(230f, 232f, 40f, -35f),
            floatArrayOf(118f, 330f, 36f, -160f), floatArrayOf(250f, 350f, 34f, -20f)
        ).forEach { (x, y, l, a) -> canvas.drawPath(petal(x, y, l, l * .22f, a, true), fill(ink)) }

        lavender(canvas, 80f, 372f, 72f, 124f, 9)
        lavender(canvas, 96f, 372f, 108f, 150f, 8)
        lavender(canvas, 60f, 372f, 52f, 196f, 7)
        dandelion(canvas, 312f, 94f, 32f)
        tulip(canvas, 236f, 114f, 50f, -84f)
        lotus(canvas, 322f, 232f, 52f)
        daisy(canvas, 150f, 222f, 30f, 10)
        daisy(canvas, 254f, 296f, 20f, 10)
        daisy(canvas, 84f, 270f, 22f, 9)
        white(canvas, petal(302f, 338f, 24f, 9f, -30f, true))    // bud
        seed(canvas, 372f, 70f, 25f)
        seed(canvas, 350f, 54f, 12f)
        spider(canvas, 150f, 112f, 46f, -4f)
        spider(canvas, 404f, 186f, 40f, 6f)
    }

    private fun daisy(canvas: Canvas, cx: Float, cy: Float, r: Float, n: Int) {
        for (i in 0 until n) white(canvas, petal(cx, cy, r, r * .32f, i * 360f / n, false))
        canvas.drawCircle(cx, cy, r * .3f, fill(DAISY_CENTRE))
        canvas.drawCircle(cx, cy, r * .3f, line(2.2f, NAVY))
    }

    private fun tulip(canvas: Canvas, cx: Float, cy: Float, r: Float, d: Float) {
        white(canvas, petal(cx, cy, r, r * .38f, d, true))
        white(canvas, petal(cx, cy, r * .92f, r * .36f, d - 32f, true))
        white(canvas, petal(cx, cy, r * .92f, r * .36f, d + 32f, true))
    }

    private fun lotus(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        listOf(
            floatArrayOf(-150f, .78f, .30f), floatArrayOf(-30f, .78f, .30f),
            floatArrayOf(-122f, .92f, .30f), floatArrayOf(-58f, .92f, .30f), floatArrayOf(-90f, 1f, .34f)
        ).forEach { (d, l, wd) -> white(canvas, petal(cx, cy, r * l, r * wd, d, true)) }
    }

    private fun lavender(canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, n: Int) {
        stem(canvas, 2.6f) { moveTo(x0, y0); quadTo((x0 + x1) / 2f + 8f, (y0 + y1) / 2f, x1, y1) }
        val bud = fill(LAVENDER); val edge = line(1.6f, NAVY)
        for (i in 0 until n) {
            val t = i / (n - 1f)
            val y = y1 + t * (y0 - y1) * .42f
            val x = x1 + (x0 - x1) * t * .42f
            val r = 7.5f - t * 2.2f
            for (side in intArrayOf(-1, 1)) {
                val ex = x + side * r * .75f
                canvas.save()
                canvas.rotate(side * -30f, ex, y)
                val oval = RectF(ex - r, y - r * .62f, ex + r, y + r * .62f)
                canvas.drawOval(oval, bud); canvas.drawOval(oval, edge)
                canvas.restore()
            }
        }
        val top = RectF(x1 - 5f, y1 - 14f, x1 + 5f, y1 + 2f)
        canvas.drawOval(top, bud); canvas.drawOval(top, edge)
    }

    private fun dandelion(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val ray = line(1.3f); val tip = line(1.1f)
        for (i in 0 until 26) {
            val a = i * 2f * PI.toFloat() / 26f
            val ex = cx + cos(a) * r; val ey = cy + sin(a) * r
            canvas.drawLine(cx, cy, ex, ey, ray)
            for (dd in floatArrayOf(-.35f, 0f, .35f)) {
                canvas.drawLine(ex, ey, ex + cos(a + dd) * 7f, ey + sin(a + dd) * 7f, tip)
            }
        }
        canvas.drawCircle(cx, cy, 5f, fill(ink))
    }

    private fun seed(canvas: Canvas, x: Float, y: Float, d: Float) {
        canvas.save()
        canvas.rotate(d, x, y)
        canvas.drawLine(x, y, x, y + 16f, line(1.3f))
        for (dd in floatArrayOf(-50f, -25f, 0f, 25f, 50f)) {
            val a = deg(dd - 90f)
            canvas.drawLine(x, y, x + cos(a) * 8f, y + sin(a) * 8f, line(1.1f))
        }
        canvas.drawCircle(x, y + 17f, 1.8f, fill(ink))
        canvas.restore()
    }

    /** RR's spider lily: long filaments fanning up and out, curling at the tips. */
    private fun spider(canvas: Canvas, cx: Float, cy: Float, r: Float, tilt: Float) {
        val base = line(1.8f)
        for (d in floatArrayOf(-200f, -160f, 20f, -20f, -180f, 0f)) {
            val a = deg(d + tilt)
            val ex = cx + cos(a) * r * .55f; val ey = cy + sin(a) * r * .55f
            canvas.drawPath(Path().apply {
                moveTo(cx, cy); quadTo(cx + cos(a) * r * .45f, cy + sin(a) * r * .45f + 10f, ex, ey - 4f)
            }, base)
        }
        val filament = line(1.6f); val anther = fill(ink)
        val n = 15
        for (i in 0 until n) {
            val a = deg(-172f + i * (164f / (n - 1)) + tilt)
            val len = r * (1f + .22f * sin(i * 1.7f))
            val tx = cx + cos(a) * len; val ty = cy + sin(a) * len
            val c1x = cx + cos(a) * len * .25f; val c1y = cy + sin(a) * len * .2f - len * .75f
            val curl = if (cos(a) >= 0f) 6f else -6f
            canvas.drawPath(Path().apply {
                moveTo(cx, cy); quadTo(c1x, c1y, tx, ty); rQuadTo(curl, -1f, curl * .6f, 5f)
            }, filament)
            canvas.drawCircle(tx + curl * .6f, ty + 5f, 2.2f, anther)
        }
    }

    // ── time + date ─────────────────────────────────────────────────────────
    private fun drawText(context: Context, canvas: Canvas, time: String, date: String, accent: Int) {
        val timeFont = ResourcesCompat.getFont(context, R.font.fraunces_bold) ?: Typeface.DEFAULT_BOLD
        val dateFont = ResourcesCompat.getFont(context, R.font.cormorant_bold) ?: Typeface.SERIF
        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = timeFont; color = if (clear) accent else NAVY; textSize = 114f; textAlign = Paint.Align.CENTER
            fontFeatureSettings = "'lnum'"
        }.shadowed()
        while (t.measureText(time) > 320f && t.textSize > 40f) t.textSize *= .95f
        canvas.drawText(time, 600f, 226f, t)

        val d = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = dateFont; color = ink; textSize = 24f; textAlign = Paint.Align.CENTER
            letterSpacing = .17f; fontFeatureSettings = "'lnum'"
        }.shadowed()
        while (d.measureText(date) > 340f && d.textSize > 12f) d.textSize *= .95f
        canvas.drawText(date, 600f, 276f, d)
    }
}
