package com.rr.numio.clock.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.rr.numio.clock.R
import com.rr.numio.clock.data.WorldCity
import com.rr.numio.clock.ui.clock.cityTimeZone
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * World Sketch widget: your world clock cities side by side, each with its time on a brush stroke,
 * a hand-drawn drawing of the city, a sun or moon for its sky, and its name and UTC offset —
 * all in RR's hand-drawn letters (the same glyphs as the Sketch poster).
 *  - boxed: cream paper card
 *  - clear: straight on the wallpaper, light ink with a soft shadow
 */
object WorldSketchRenderer {

    /** City drawings, listed by id so resource shrinking keeps them. Key = city name, simplified. */
    private val DRAWINGS = mapOf(
        "kochi" to R.drawable.city_kochi, "paris" to R.drawable.city_paris, "saopaulo" to R.drawable.city_saopaulo,
        "riyadh" to R.drawable.city_riyadh, "toronto" to R.drawable.city_toronto, "berlin" to R.drawable.city_berlin,
        "moscow" to R.drawable.city_moscow, "istanbul" to R.drawable.city_istanbul, "karachi" to R.drawable.city_karachi,
        "pathanamthitta" to R.drawable.city_pathanamthitta, "hongkong" to R.drawable.city_hongkong,
        "seoul" to R.drawable.city_seoul, "beijing" to R.drawable.city_beijing, "newyork" to R.drawable.city_newyork,
        "london" to R.drawable.city_london, "sydney" to R.drawable.city_sydney, "dubai" to R.drawable.city_dubai,
        "tokyo" to R.drawable.city_tokyo, "bangkok" to R.drawable.city_bangkok, "losangeles" to R.drawable.city_losangeles,
        "sanfrancisco" to R.drawable.city_sanfrancisco, "singapore" to R.drawable.city_singapore,
        "chicago" to R.drawable.city_chicago, "mumbai" to R.drawable.city_mumbai, "dhaka" to R.drawable.city_dhaka,
        "auckland" to R.drawable.city_auckland, "johannesburg" to R.drawable.city_johannesburg,
        "cairo" to R.drawable.city_cairo, "nairobi" to R.drawable.city_nairobi,
    )

    /** Drawings that have their own version for wallpapers (light lines). */
    private val LIGHT = mapOf("pathanamthitta" to R.drawable.city_pathanamthitta_light)

    private val cache = HashMap<String, Bitmap>()

    /** "São Paulo" → "saopaulo" */
    private fun key(name: String) =
        Normalizer.normalize(name, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase().filter { it.isLetter() }

    /** "São Paulo" → "SAO PAULO", only letters, digits and spaces (what the hand-drawn glyphs have). */
    private fun label(name: String) =
        Normalizer.normalize(name, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").uppercase()
            .filter { it in 'A'..'Z' || it in '0'..'9' || it == ' ' }

    /** The city drawing; for the clear style the dark ink lines are turned light so they show on a dark wallpaper. */
    private fun drawing(context: Context, city: String, light: Boolean): Bitmap? {
        val k = key(city)
        val own = if (light) LIGHT[k] else null
        val id = own ?: DRAWINGS[k] ?: return null
        val cacheKey = "$id/$light"
        return synchronized(cache) {
            cache.getOrPut(cacheKey) {
                val src = BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })
                if (!light || own != null) src else {
                    val px = IntArray(src.width * src.height)
                    src.getPixels(px, 0, src.width, 0, 0, src.width, src.height)
                    for (i in px.indices) {
                        val c = px[i]
                        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
                        val lum = (r * 299 + g * 587 + b * 114) / 1000
                        val sat = maxOf(r, g, b) - minOf(r, g, b)
                        // Grey-ish pixels are ink lines or paper: lines become white, the paper fades out
                        // (so there's no cream halo). Colours (sea, gold, green, red) stay as drawn.
                        if (sat < 60) {
                            val keep = ((215 - lum) / 140f).coerceIn(0f, 1f)
                            px[i] = Color.argb((Color.alpha(c) * keep).toInt(), 246, 242, 234)
                        }
                    }
                    Bitmap.createBitmap(px, src.width, src.height, Bitmap.Config.ARGB_8888)
                }
            }
        }
    }

    fun draw(context: Context, canvas: Canvas, w: Int, h: Int, accent: Int, cities: List<WorldCity>, boxed: Boolean) {
        val S = SketchRenderer
        val stroke = (h * 0.012f).coerceAtLeast(2f)
        val inset = stroke * 2.5f
        val card = RectF(inset, inset, w - inset, h - inset)
        val cw = card.width(); val ch = card.height()
        val radius = ch * 0.08f
        val rnd = Random(7)
        val inkColor = if (boxed) S.INK else S.WHITE
        val shadow = !boxed
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor; style = Paint.Style.STROKE; strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            if (shadow) setShadowLayer(stroke * 1.2f, stroke * 0.4f, stroke * 0.8f, S.SHADOW)
        }

        val gold = if (boxed) S.darken(accent, 0.12f) else accent
        val list = cities.take(4).ifEmpty { return }
        val pad = ch * 0.06f
        val colW = (cw - pad * 1.2f) / list.size
        val now = Instant.now()
        val is24 = android.text.format.DateFormat.is24HourFormat(context)

        val times = list.map { city ->
            val t = now.atZone(cityTimeZone(city).toZoneId())
            val hh = if (is24) t.hour else (t.hour % 12).let { if (it == 0) 12 else it }
            Triple(t, (if (is24) hh.toString().padStart(2, '0') else hh.toString()) + ":" + t.minute.toString().padStart(2, '0'),
                if (is24) "" else if (t.hour < 12) "AM" else "PM")
        }

        // One block per city, all the same size and centred on the widget:
        //   time · sun/moon · drawing · NAME · UTC offset
        val hAvail = ch - pad * 2
        var th = hAvail * 0.17f
        fun timeW(i: Int) = S.measure(context, times[i].second, th) +
            if (times[i].third.isEmpty()) 0f else th * 0.1f + S.measure(context, times[i].third, th * 0.3f)
        while (list.indices.any { timeW(it) > colW * 0.84f } && th > 8f) th *= 0.95f
        val sunD = minOf(hAvail * 0.075f, th * 0.55f)
        val gap = hAvail * 0.035f

        val namePaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            letterSpacing = 0.12f
            color = inkColor
            textSize = hAvail * 0.06f
            if (shadow) setShadowLayer(textSize * 0.25f, 0f, textSize * 0.08f, S.SHADOW)
        }
        while (list.any { namePaint.measureText(label(it.name)) > colW * 0.88f } && namePaint.textSize > 6f) namePaint.textSize *= 0.94f
        val utcPaint = android.text.TextPaint(namePaint).apply {
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            letterSpacing = 0.06f
            textSize = namePaint.textSize * 0.78f
            color = if (boxed) Color.argb(190, 60, 55, 48) else Color.argb(220, 255, 255, 255)
        }
        val nameH = namePaint.textSize
        val utcH = utcPaint.textSize

        // The drawing gets what's left, up to a nice proportion of the column
        val fixed = th + gap + sunD + gap + gap * 1.1f + nameH + gap * 0.6f + utcH
        // The drawings are wider than tall, so the column width usually decides their size. Use the
        // height they really need (the tallest one), so the block stays tight with no gap above them.
        val artW = colW * 0.97f
        val artMax = (hAvail - fixed).coerceAtLeast(hAvail * 0.2f)
        val arts = list.map { drawing(context, it.name, light = !boxed) }
        val artH = arts.maxOf { a -> if (a == null) artMax * 0.7f else minOf(artMax, artW * a.height / a.width) }
        val total = fixed + artH
        val top = card.top + pad + (hAvail - total).coerceAtLeast(0f) / 2f

        if (boxed) {
            // The paper hugs the cities, so a tall widget has no empty paper above and below them
            val paper = RectF(card.left, (top - pad).coerceAtLeast(card.top), card.right, (top + total + pad).coerceAtMost(card.bottom))
            val pr = minOf(radius, paper.height() * 0.08f)
            canvas.drawRoundRect(paper, pr, pr, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = S.PAPER })
            val fiber = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = S.FIBER; strokeWidth = stroke * 0.35f; strokeCap = Paint.Cap.ROUND }
            canvas.save(); canvas.clipRect(paper)
            repeat((80 * paper.height() / ch).toInt().coerceAtLeast(30)) {
                val fx = paper.left + rnd.nextFloat() * paper.width(); val fy = paper.top + rnd.nextFloat() * paper.height()
                val len = ch * (0.01f + rnd.nextFloat() * 0.03f); val ang = rnd.nextFloat() * Math.PI.toFloat()
                canvas.drawLine(fx, fy, fx + len * cos(ang), fy + len * sin(ang), fiber)
            }
            canvas.restore()
            canvas.drawPath(S.wobblyRect(paper, pr, stroke * 0.8f, rnd), ink)
        }


        list.forEachIndexed { i, city ->
            val cx = card.left + pad * 0.6f + colW * i + colW / 2f
            val (t, time, ampm) = times[i]
            var y = top

            // Time (on a brush stroke on paper; in the accent on a wallpaper)
            val tw = timeW(i)
            val tx = cx - tw / 2f
            if (boxed) {
                val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold; alpha = 150 }
                val b = RectF(tx - th * 0.25f, y - th * 0.1f, tx + tw + th * 0.25f, y + th * 1.14f)
                canvas.drawPath(brushPath(b, Random(i * 31 + 5)), brush)
            }
            val digits = if (boxed) S.INK else gold
            val used = S.drawText(context, canvas, time, tx, y + th, th, digits, shadow)
            if (ampm.isNotEmpty()) S.drawText(context, canvas, ampm, tx + used + th * 0.1f, y + th, th * 0.3f, digits, shadow)
            y += th + gap

            // Sun by day, moon by night
            val night = t.hour < 6 || t.hour >= 18
            if (night) drawMoon(canvas, cx, y + sunD / 2f, sunD / 2f, gold, shadow)
            else drawSun(canvas, cx, y + sunD / 2f, sunD / 2f, gold, stroke, shadow)
            y += sunD + gap

            // The drawing, as big as it fits, sitting on the same baseline in every column
            val box = RectF(cx - artW / 2f, y, cx + artW / 2f, y + artH)
            val art = arts[i]
            if (art != null) {
                val scale = minOf(box.width() / art.width, box.height() / art.height)
                val dw = art.width * scale; val dh = art.height * scale
                canvas.drawBitmap(art, null, RectF(cx - dw / 2f, box.bottom - dh, cx + dw / 2f, box.bottom),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } else {
                drawGlobe(canvas, box, ink, gold, stroke)
            }
            y += artH + gap * 1.1f

            // NAME and UTC offset, clean and centred
            val name = label(city.name)
            canvas.drawText(name, cx - namePaint.measureText(name) / 2f, y + nameH * 0.82f, namePaint)
            y += nameH + gap * 0.6f
            val off = zoneOffsetMinutes(city, now)
            val utc = "UTC" + (if (off < 0) "−" else "+") + (abs(off) / 60) +
                (if (abs(off) % 60 != 0) ":" + (abs(off) % 60).toString().padStart(2, '0') else "")
            canvas.drawText(utc, cx - utcPaint.measureText(utc) / 2f, y + utcH * 0.82f, utcPaint)
        }
    }

    private fun zoneOffsetMinutes(city: WorldCity, now: Instant) =
        cityTimeZone(city).getOffset(now.toEpochMilli()) / 60000

    /** A rough, dry-brush stroke shape. */
    private fun brushPath(r: RectF, rnd: Random): Path {
        val p = Path()
        val steps = 14
        val top = FloatArray(steps + 1) { r.top + (rnd.nextFloat() - 0.3f) * r.height() * 0.18f }
        val bot = FloatArray(steps + 1) { r.bottom - (rnd.nextFloat() - 0.3f) * r.height() * 0.18f }
        p.moveTo(r.left + r.width() * 0.03f, top[0] + r.height() * 0.1f)
        for (i in 1..steps) p.lineTo(r.left + r.width() * i / steps, top[i])
        p.quadTo(r.right + r.height() * 0.08f, r.centerY(), r.right - r.width() * 0.02f, bot[steps])
        for (i in steps - 1 downTo 0) p.lineTo(r.left + r.width() * i / steps, bot[i])
        p.quadTo(r.left - r.height() * 0.1f, r.centerY(), r.left + r.width() * 0.03f, top[0] + r.height() * 0.1f)
        p.close()
        return p
    }

    private fun drawSun(c: Canvas, x: Float, y: Float, r: Float, color: Int, stroke: Float, shadow: Boolean) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; if (shadow) setShadowLayer(stroke, 0f, stroke * 0.5f, SketchRenderer.SHADOW) }
        c.drawCircle(x, y, r * 0.55f, fill)
        val ray = Paint(fill).apply { style = Paint.Style.STROKE; strokeWidth = stroke * 0.9f; strokeCap = Paint.Cap.ROUND }
        for (k in 0 until 8) {
            val a = Math.PI * 2 * k / 8 + 0.2
            c.drawLine(x + (r * 0.8f) * cos(a).toFloat(), y + (r * 0.8f) * sin(a).toFloat(),
                x + (r * 1.15f) * cos(a).toFloat(), y + (r * 1.15f) * sin(a).toFloat(), ray)
        }
    }

    private fun drawMoon(c: Canvas, x: Float, y: Float, r: Float, color: Int, shadow: Boolean) {
        val p = Path().apply {
            addCircle(x, y, r * 0.75f, Path.Direction.CW)
            val cut = Path().apply { addCircle(x + r * 0.38f, y - r * 0.28f, r * 0.62f, Path.Direction.CW) }
            op(cut, Path.Op.DIFFERENCE)
        }
        c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; if (shadow) setShadowLayer(r * 0.2f, 0f, r * 0.1f, SketchRenderer.SHADOW) })
    }


    /** For a city without a drawing: a small hand-drawn globe. */
    private fun drawGlobe(c: Canvas, box: RectF, ink: Paint, gold: Int, stroke: Float) {
        val r = minOf(box.width(), box.height()) * 0.38f
        val x = box.centerX(); val y = box.bottom - r * 1.1f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold; alpha = 120 }
        c.drawCircle(x, y, r, fill)
        val p = Paint(ink).apply { strokeWidth = stroke * 1.1f }
        c.drawCircle(x, y, r, p)
        p.strokeWidth = stroke * 0.7f
        c.drawOval(RectF(x - r * 0.45f, y - r, x + r * 0.45f, y + r), p)
        c.drawLine(x - r, y, x + r, y, p)
        c.drawArc(RectF(x - r, y - r * 0.55f, x + r, y + r * 0.2f), 200f, 140f, false, p)
    }
}
