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
        val id = DRAWINGS[key(city)] ?: return null
        val cacheKey = "$id/$light"
        return synchronized(cache) {
            cache.getOrPut(cacheKey) {
                val src = BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })
                if (!light) src else {
                    val px = IntArray(src.width * src.height)
                    src.getPixels(px, 0, src.width, 0, 0, src.width, src.height)
                    for (i in px.indices) {
                        val c = px[i]
                        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
                        val lum = (r * 299 + g * 587 + b * 114) / 1000
                        val sat = maxOf(r, g, b) - minOf(r, g, b)
                        // dark, grey-ish ink → warm white; colours (sea, gold, red) stay
                        if (lum < 120 && sat < 60) px[i] = Color.argb(Color.alpha(c), 246, 240, 226)
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

        if (boxed) {
            canvas.drawRoundRect(card, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = S.PAPER })
            val fiber = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = S.FIBER; strokeWidth = stroke * 0.35f; strokeCap = Paint.Cap.ROUND }
            canvas.save(); canvas.clipRect(card)
            repeat(80) {
                val fx = card.left + rnd.nextFloat() * cw; val fy = card.top + rnd.nextFloat() * ch
                val len = ch * (0.01f + rnd.nextFloat() * 0.03f); val ang = rnd.nextFloat() * Math.PI.toFloat()
                canvas.drawLine(fx, fy, fx + len * cos(ang), fy + len * sin(ang), fiber)
            }
            canvas.restore()
            canvas.drawPath(S.wobblyRect(card, radius, stroke * 0.8f, rnd), ink)
        }

        val gold = if (boxed) S.darken(accent, 0.12f) else accent
        val list = cities.take(4).ifEmpty { return }
        val pad = ch * 0.07f
        val colW = (cw - pad * 2) / list.size
        val now = Instant.now()
        val here = ZoneId.systemDefault()
        val hereOffset = here.rules.getOffset(now).totalSeconds / 60
        val is24 = android.text.format.DateFormat.is24HourFormat(context)

        // One size for every column, so the row looks even: fit the widest time and name.
        val times = list.map { city ->
            val t = now.atZone(cityTimeZone(city).toZoneId())
            val hh = if (is24) t.hour else (t.hour % 12).let { if (it == 0) 12 else it }
            Triple(t, (if (is24) hh.toString().padStart(2, '0') else hh.toString()) + ":" + t.minute.toString().padStart(2, '0'),
                if (is24) "" else if (t.hour < 12) "AM" else "PM")
        }
        var th = ch * 0.21f
        fun timeW(i: Int) = S.measure(context, times[i].second, th) + if (times[i].third.isEmpty()) 0f else th * 0.08f + S.measure(context, times[i].third, th * 0.32f)
        while (list.indices.any { timeW(it) > colW * 0.86f } && th > 8f) th *= 0.95f
        var nh = ch * 0.085f
        while (list.any { S.measure(context, label(it.name), nh) > colW * 0.9f } && nh > 6f) nh *= 0.94f
        val oh = nh * 0.72f

        val timeTop = card.top + pad * 1.1f
        val timeBottom = timeTop + th
        val utcBottom = card.bottom - pad * 0.9f
        val nameBottom = utcBottom - oh * 1.7f
        val artTop = timeBottom + th * 0.35f
        val artBottom = nameBottom - nh * 0.55f

        list.forEachIndexed { i, city ->
            val cx = card.left + pad + colW * i + colW / 2f
            val (t, time, ampm) = times[i]

            // Brush stroke behind the time (on paper); on a wallpaper the time itself is in the accent
            val tw = timeW(i)
            val tx = cx - tw / 2f
            if (boxed) {
                val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold; alpha = 150 }
                val b = RectF(tx - th * 0.25f, timeTop - th * 0.12f, tx + tw + th * 0.25f, timeBottom + th * 0.16f)
                canvas.drawPath(brushPath(b, Random(i * 31 + 5)), brush)
            }
            val digits = if (boxed) S.INK else gold
            val used = S.drawText(context, canvas, time, tx, timeBottom, th, digits, shadow)
            if (ampm.isNotEmpty()) S.drawText(context, canvas, ampm, tx + used + th * 0.08f, timeBottom, th * 0.32f, digits, shadow)

            // The city drawing, fitted into its box
            val box = RectF(cx - colW * 0.45f, artTop, cx + colW * 0.45f, artBottom)
            val art = drawing(context, city.name, light = !boxed)
            if (art != null) {
                val scale = minOf(box.width() / art.width, box.height() / art.height)
                val dw = art.width * scale; val dh = art.height * scale
                val dst = RectF(cx - dw / 2f, box.bottom - dh, cx + dw / 2f, box.bottom)
                val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                if (shadow) { // a soft shadow under the whole drawing
                    p.setShadowLayer(stroke * 1.5f, 0f, stroke, S.SHADOW)
                }
                canvas.drawBitmap(art, null, dst, p)
            } else {
                drawGlobe(canvas, box, ink, gold, stroke)
            }

            // Sun by day, moon by night, in the corner of the sky
            val night = t.hour < 6 || t.hour >= 18
            val sr = minOf(colW, box.height()) * 0.08f
            val sx = box.right - sr * 1.2f; val sy = box.top + sr * 1.3f
            if (night) drawMoon(canvas, sx, sy, sr, gold, shadow) else drawSun(canvas, sx, sy, sr, gold, stroke, shadow)

            // Name, underlined
            val name = label(city.name)
            val nw = S.measure(context, name, nh)
            S.drawText(context, canvas, name, cx - nw / 2f, nameBottom, nh, inkColor, shadow)
            val u = Paint(ink).apply { color = gold; strokeWidth = stroke * 1.1f }
            val uy = nameBottom + nh * 0.35f
            canvas.drawPath(Path().apply {
                moveTo(cx - nw * 0.38f, uy + nh * 0.05f); quadTo(cx, uy - nh * 0.08f, cx + nw * 0.4f, uy)
            }, u)

            // "UTC +5:30" — and "+1 day" in the label when it's already tomorrow there
            val off = zoneOffsetMinutes(city, now)
            val sign = if (off < 0) "-" else "+"
            val oText = (abs(off) / 60).toString() + if (abs(off) % 60 != 0) ":" + (abs(off) % 60).toString().padStart(2, '0') else ""
            val pre = "UTC "
            val preW = S.measure(context, pre, oh)
            val signW = oh * 0.5f
            val numW = S.measure(context, oText, oh)
            val total = preW + signW + numW
            var ox = cx - total / 2f
            val muted = if (boxed) Color.argb(200, 60, 55, 48) else Color.argb(230, 255, 255, 255)
            S.drawText(context, canvas, pre, ox, utcBottom, oh, muted, shadow); ox += preW
            drawSign(canvas, sign, ox + signW * 0.45f, utcBottom - oh * 0.45f, oh * 0.24f, Paint(ink).apply { color = muted; strokeWidth = stroke * 0.9f })
            ox += signW
            S.drawText(context, canvas, oText, ox, utcBottom, oh, muted, shadow)
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

    private fun drawSign(c: Canvas, sign: String, x: Float, y: Float, s: Float, p: Paint) {
        c.drawLine(x - s, y, x + s, y, p)
        if (sign == "+") c.drawLine(x, y - s, x, y + s, p)
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
