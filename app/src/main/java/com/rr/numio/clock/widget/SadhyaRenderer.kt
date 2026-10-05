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
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Sadhya poster style: a Kerala sadhya on a banana leaf, drawn as a sticker,
 * with the time served on the rice.
 *
 * Everything is drawn on an 800 x 400 board and scaled to the widget,
 * so the layout stays the same at any size.
 */
object SadhyaRenderer {

    private const val NAVY = 0xFF1E2B5C.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    /** Banana leaf: narrow tip on the left, broad cut end on the right. */
    private fun leafPath() = Path().apply {
        moveTo(24f, 222f); cubicTo(52f, 128f, 150f, 70f, 280f, 64f); lineTo(746f, 58f)
        cubicTo(776f, 58f, 788f, 76f, 788f, 104f); lineTo(788f, 318f)
        cubicTo(788f, 344f, 770f, 352f, 744f, 350f); lineTo(560f, 346f)
        cubicTo(530f, 356f, 500f, 342f, 470f, 350f); lineTo(290f, 350f)
        cubicTo(150f, 344f, 56f, 304f, 24f, 222f); close()
    }

    private class Curry(
        val cx: Float, val cy: Float, val rx: Float, val ry: Float, val fill: Int,
        val deco: (Canvas, Curry, Random) -> Unit = { _, _, _ -> }
    )

    // ── paints ──────────────────────────────────────────────────────────────
    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun line(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = width
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }

    /** Fill a shape, then outline it in navy. */
    private fun shape(canvas: Canvas, path: Path, color: Int, outline: Float) {
        canvas.drawPath(path, fill(color))
        canvas.drawPath(path, line(NAVY, outline))
    }

    private fun oval(cx: Float, cy: Float, rx: Float, ry: Float) =
        Path().apply { addOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), Path.Direction.CW) }

    private fun circle(cx: Float, cy: Float, r: Float) = oval(cx, cy, r, r)

    // ── entry point ─────────────────────────────────────────────────────────
    fun draw(context: Context, canvas: Canvas, w: Int, h: Int, time: String, date: String) {
        val s = min(w / 800f, h / 400f)
        canvas.save()
        canvas.translate((w - 800f * s) / 2f, (h - 400f * s) / 2f)
        canvas.scale(s, s)

        val rnd = Random(11)
        drawLeaf(canvas)
        drawSambarBowl(canvas)
        drawChips(canvas)
        drawCurries(canvas, rnd)
        drawPapadamAndBanana(canvas)
        drawRice(canvas)
        drawTime(context, canvas, time)
        drawPayasam(canvas)
        drawLabels(context, canvas, date)

        canvas.restore()
    }

    // ── leaf ────────────────────────────────────────────────────────────────
    private fun drawLeaf(canvas: Canvas) {
        val leaf = leafPath()
        canvas.drawPath(leaf, line(WHITE, 22f))          // sticker edge
        canvas.drawPath(leaf, fill(0xFF3FAE49.toInt()))

        canvas.save()
        canvas.clipPath(leaf)
        // fine veins running out from the centre fold
        val vein = line(0xD95FC066.toInt(), 1.6f)
        var x = 40f
        while (x < 800f) {
            canvas.drawLine(x, 212f, x + 22f, 52f, vein)
            canvas.drawLine(x, 212f, x + 22f, 372f, vein)
            x += 12f
        }
        // centre fold: light crease + the vein line
        canvas.drawPath(Path().apply {
            moveTo(34f, 220f); cubicTo(120f, 212f, 220f, 208f, 320f, 207f); lineTo(786f, 205f)
        }, line(0xBF8FDC8A.toInt(), 9f))
        canvas.drawPath(Path().apply {
            moveTo(30f, 222f); cubicTo(120f, 214f, 220f, 210f, 320f, 209f); lineTo(786f, 207f)
        }, line(0xFF2D8A36.toInt(), 4f))
        canvas.restore()

        canvas.drawPath(leaf, line(NAVY, 5f))
    }

    // ── clay sambar bowl on the top-right corner ────────────────────────────
    private fun drawSambarBowl(canvas: Canvas) {
        canvas.save()
        canvas.translate(736f, 82f); canvas.scale(0.72f, 0.72f); canvas.translate(-736f, -70f)
        canvas.drawPath(oval(736f, 70f, 66f, 50f), fill(WHITE).apply {
            style = Paint.Style.FILL_AND_STROKE; strokeWidth = 18f
        })
        shape(canvas, oval(736f, 70f, 62f, 46f), 0xFFA0522D.toInt(), 4f)
        shape(canvas, oval(736f, 62f, 52f, 34f), 0xFFD9712A.toInt(), 4f)
        veg(canvas, 723f, 55f, 22f, 7f, -20f, 0xFF6AA83A.toInt())
        veg(canvas, 751f, 73f, 18f, 6f, 30f, 0xFF6AA83A.toInt())
        shape(canvas, circle(752f, 58f, 7f), 0xFFF2C94C.toInt(), 2.5f)
        shape(canvas, circle(728f, 72f, 5f), 0xFFE74C3C.toInt(), 2.5f)
        canvas.drawLine(770f, 40f, 800f, 8f, line(0xFFC9C9C9.toInt(), 8f))  // spoon
        canvas.drawLine(770f, 40f, 800f, 8f, line(NAVY, 2f))
        canvas.restore()
    }

    /** A little rounded stick of vegetable, rotated around its centre. */
    private fun veg(canvas: Canvas, cx: Float, cy: Float, len: Float, thick: Float, deg: Float, color: Int, outline: Float = 2.5f) {
        canvas.save()
        canvas.rotate(deg, cx, cy)
        val r = RectF(cx - len / 2f, cy - thick / 2f, cx + len / 2f, cy + thick / 2f)
        val p = Path().apply { addRoundRect(r, thick / 2f, thick / 2f, Path.Direction.CW) }
        shape(canvas, p, color, outline)
        canvas.restore()
    }

    // ── banana chips at the leaf tip ────────────────────────────────────────
    private fun drawChips(canvas: Canvas) {
        listOf(112f to 168f, 132f to 156f, 128f to 180f, 152f to 170f).forEach { (x, y) ->
            shape(canvas, circle(x, y, 10f), 0xFFF7CF3C.toInt(), 2.5f)
            canvas.drawCircle(x, y, 4f, line(0xFFE0B21E.toInt(), 1.5f))
        }
    }

    // ── curries in two staggered rows ───────────────────────────────────────
    private val dotsDeco = { color: Int, n: Int, r: Float ->
        { c: Canvas, k: Curry, rnd: Random ->
            repeat(n) {
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val t = sqrt(rnd.nextFloat()) * 0.7f
                c.drawCircle(k.cx + cos(a) * k.rx * t, k.cy + sin(a) * k.ry * t, r, fill(color))
            }
        }
    }

    private val row1 = listOf(
        Curry(232f, 104f, 16f, 12f, 0xFFFFFFFF.toInt()),                                   // salt
        Curry(272f, 100f, 20f, 14f, 0xFFD93B1F.toInt(), dotsDeco(0xFF8F1D0C.toInt(), 4, 2.6f)), // mango pickle
        Curry(318f, 98f, 22f, 15f, 0xFFB5C23E.toInt(), dotsDeco(0xFF5C6A12.toInt(), 3, 3f)),    // lime pickle
        Curry(368f, 97f, 26f, 17f, 0xFFE05A8C.toInt()),                                     // beetroot pachadi
        Curry(424f, 96f, 28f, 18f, 0xFFF3E6B8.toInt()) { c, k, rnd ->                       // avial
            val colors = intArrayOf(0xFFE8892A.toInt(), 0xFF7BB33A.toInt(), 0xFFA0522D.toInt(), 0xFFF1D36B.toInt())
            repeat(6) {
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val t = sqrt(rnd.nextFloat()) * 0.6f
                veg(c, k.cx + cos(a) * k.rx * t, k.cy + sin(a) * k.ry * t, 18f, 5f,
                    rnd.nextFloat() * 120f - 60f, colors[rnd.nextInt(colors.size)], 1f)
            }
        },
        Curry(484f, 95f, 26f, 17f, 0xFFFBF7EA.toInt(), dotsDeco(0xFFC9A14A.toInt(), 3, 3.4f)), // olan
        Curry(542f, 94f, 28f, 18f, 0xFF9CCC3A.toInt(), dotsDeco(WHITE, 7, 2.2f)),              // thoran
        Curry(602f, 93f, 26f, 17f, 0xFF7A5A2A.toInt(), dotsDeco(0xFFD7AA66.toInt(), 5, 3f)),    // kootu curry
    )
    private val row2 = listOf(
        Curry(258f, 140f, 20f, 14f, 0xFF5E2412.toInt()),                                    // inji puli
        Curry(304f, 142f, 24f, 15f, 0xFF8A4B1F.toInt(), dotsDeco(0xFFC27A3A.toInt(), 4, 2.4f)), // sharkara upperi
        Curry(356f, 141f, 26f, 16f, 0xFFF5D64E.toInt()) { c, _, _ ->                         // pineapple pachadi
            shape(c, circle(348f, 141f, 4.5f), 0xFF6A1B4D.toInt(), 1.5f)
            shape(c, circle(364f, 138f, 4.5f), 0xFF6A1B4D.toInt(), 1.5f)
        },
        Curry(414f, 142f, 28f, 16f, 0xFFE88A2A.toInt(), dotsDeco(0xFF7A2E0E.toInt(), 4, 2.6f)), // erissery
        Curry(474f, 141f, 26f, 16f, 0xFF3F7D2A.toInt(), dotsDeco(0xFFA7D36A.toInt(), 6, 2.2f)), // beans mezhukkupuratti
        Curry(532f, 140f, 26f, 16f, 0xFFD6A64A.toInt(), dotsDeco(0xFF7A4A12.toInt(), 5, 2f)),   // chena
        Curry(590f, 140f, 26f, 16f, 0xFFC0392B.toInt(), dotsDeco(0xFFF3C26B.toInt(), 4, 2.6f)), // pulissery-red / stew
    )

    private fun drawCurries(canvas: Canvas, rnd: Random) {
        fun row(list: List<Curry>, dx: Float, dy: Float) {
            canvas.save()
            canvas.translate(dx, dy); canvas.scale(1.18f, 1.22f)
            list.forEach { k ->
                shape(canvas, heap(k, rnd), k.fill, 3f)
                k.deco(canvas, k, rnd)
                // soft highlight
                canvas.drawPath(oval(k.cx - k.rx * 0.25f, k.cy - k.ry * 0.35f, k.rx * 0.35f, k.ry * 0.18f),
                    fill(0x73FFFFFF))
            }
            canvas.restore()
        }
        row(row1, -40f, 16f)
        row(row2, -52f, 22f)
    }

    /** Irregular rounded heap, like a scoop of curry. */
    private fun heap(k: Curry, rnd: Random, n: Int = 9, wob: Float = 0.18f): Path {
        val xs = FloatArray(n); val ys = FloatArray(n)
        for (i in 0 until n) {
            val a = i.toFloat() / n * 2f * PI.toFloat()
            val f = 1f + (rnd.nextFloat() - 0.5f) * wob * 2f
            xs[i] = k.cx + cos(a) * k.rx * f
            ys[i] = k.cy + sin(a) * k.ry * f
        }
        return Path().apply {
            moveTo((xs[0] + xs[n - 1]) / 2f, (ys[0] + ys[n - 1]) / 2f)
            for (i in 0 until n) {
                val j = (i + 1) % n
                quadTo(xs[i], ys[i], (xs[i] + xs[j]) / 2f, (ys[i] + ys[j]) / 2f)
            }
            close()
        }
    }

    // ── papadam + banana, bottom left ───────────────────────────────────────
    private fun drawPapadamAndBanana(canvas: Canvas) {
        shape(canvas, circle(150f, 268f, 52f), 0xFFF7E7A8.toInt(), 4f)
        val bubble = line(0xFFD9BE5E.toInt(), 2.5f)
        canvas.drawCircle(134f, 252f, 6f, bubble); canvas.drawCircle(168f, 284f, 7f, bubble)
        canvas.drawCircle(146f, 292f, 4f, bubble); canvas.drawCircle(172f, 246f, 4f, bubble)
        canvas.drawCircle(120f, 280f, 4f, bubble)

        shape(canvas, Path().apply {
            moveTo(150f, 300f); cubicTo(176f, 336f, 240f, 342f, 286f, 320f)
            cubicTo(294f, 316f, 292f, 308f, 284f, 308f); cubicTo(246f, 322f, 196f, 318f, 170f, 292f)
            cubicTo(162f, 284f, 146f, 288f, 150f, 300f); close()
        }, 0xFFF7D33D.toInt(), 4f)
        shape(canvas, Path().apply {
            moveTo(284f, 308f); rLineTo(14f, -6f); rLineTo(4f, 10f); rLineTo(-12f, 8f); close()
        }, 0xFF6B4A1F.toInt(), 4f)
    }

    // ── rice heap with parippu + ghee ───────────────────────────────────────
    private fun drawRice(canvas: Canvas) {
        shape(canvas, Path().apply {
            moveTo(300f, 322f); cubicTo(282f, 262f, 340f, 222f, 470f, 220f)
            cubicTo(600f, 220f, 668f, 258f, 656f, 312f); cubicTo(648f, 350f, 312f, 356f, 300f, 322f); close()
        }, 0xFFFFFDF4.toInt(), 5f)
        val grain = line(0xFFE7E1CC.toInt(), 3f)
        listOf(350f to 300f, 372f to 262f, 420f to 236f, 520f to 232f, 590f to 262f,
            600f to 310f, 560f to 326f, 400f to 322f, 470f to 330f).forEach { (x, y) ->
            canvas.drawPath(Path().apply { moveTo(x, y); rQuadTo(5f, -4f, 9f, 0f) }, grain)
        }
        shape(canvas, Path().apply {
            moveTo(606f, 262f); cubicTo(612f, 244f, 650f, 246f, 654f, 264f)
            cubicTo(658f, 280f, 638f, 288f, 622f, 284f); cubicTo(608f, 282f, 600f, 276f, 606f, 262f); close()
        }, 0xFFF2C230.toInt(), 3.5f)
        shape(canvas, oval(630f, 262f, 7f, 4.5f), 0xFFFFF4B8.toInt(), 2f)
    }

    // ── time: flat navy poster digits, centred on the rice ─────────────────
    private fun drawTime(context: Context, canvas: Canvas, time: String) {
        val font = ResourcesCompat.getFont(context, R.font.bangers) ?: Typeface.DEFAULT_BOLD
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font; color = NAVY; textSize = 104f
            letterSpacing = 0.03f; textAlign = Paint.Align.CENTER
        }
        // keep it inside the rice even for wide times
        while (p.measureText(time) > 300f && p.textSize > 40f) p.textSize *= 0.95f
        canvas.drawText(time, 470f, 324f, p)
    }

    // ── payasam glasses, bottom right ───────────────────────────────────────
    private fun drawPayasam(canvas: Canvas) {
        fun glass(l: Float, top: Float, fillTop: Float, liquid: Int, surface: Int) {
            val r = l + 40f
            shape(canvas, Path().apply {
                moveTo(l, top); lineTo(r, top); lineTo(r - 4f, top + 64f)
                cubicTo(r - 5f, top + 70f, l + 5f, top + 70f, l + 4f, top + 64f); close()
            }, 0xFFF1E6D0.toInt(), 4f)
            shape(canvas, Path().apply {
                moveTo(l + 2f, fillTop); lineTo(r - 2f, fillTop); lineTo(r - 4f, top + 64f)
                cubicTo(r - 5f, top + 70f, l + 5f, top + 70f, l + 4f, top + 64f); close()
            }, liquid, 4f)
            shape(canvas, oval(l + 20f, top + 1f, 20f, 6f), surface, 4f)
        }
        glass(664f, 282f, 300f, 0xFFE9C9A8.toInt(), 0xFFF6EFE2.toInt())  // palada
        glass(712f, 296f, 312f, 0xFF9A5A26.toInt(), 0xFFB06A2C.toInt())  // pradhaman
        // cashews on top
        listOf(676f to 282f, 724f to 296f, 736f to 298f).forEach { (x, y) ->
            shape(canvas, Path().apply { moveTo(x, y); rQuadTo(4f, -6f, 8f, 0f) }, 0xFFF3D9A4.toInt(), 2f)
        }
    }

    // ── date sticker strip + brand ──────────────────────────────────────────
    private fun drawLabels(context: Context, canvas: Canvas, date: String) {
        val font = ResourcesCompat.getFont(context, R.font.baloo2_extrabold) ?: Typeface.DEFAULT_BOLD
        fun sticker(text: String, x: Float, y: Float, size: Float, edge: Float, spacing: Float, align: Paint.Align) {
            val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = font; textSize = size; letterSpacing = spacing; textAlign = align
            }
            canvas.drawText(text, x, y, Paint(base).apply {
                color = NAVY; style = Paint.Style.STROKE; strokeWidth = edge; strokeJoin = Paint.Join.ROUND
            })
            canvas.drawText(text, x, y, Paint(base).apply { color = WHITE })
        }
        var size = 26f
        val probe = Paint().apply { typeface = font; letterSpacing = 0.04f }
        while (probe.apply { textSize = size }.measureText(date) > 440f && size > 12f) size *= 0.95f
        sticker(date, 470f, 386f, size, 7f, 0.04f, Paint.Align.CENTER)
        sticker("NUMIO", 46f, 386f, 16f, 5f, 0.25f, Paint.Align.LEFT)
    }
}
