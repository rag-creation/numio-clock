package com.rr.numio.clock.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.rr.numio.clock.R
import com.rr.numio.clock.data.WorldCity
import com.rr.numio.clock.ui.clock.cityTimeZone
import java.text.Normalizer
import java.time.Instant
import kotlin.math.abs
import kotlin.math.min

/**
 * World Sticker widget: one city per 2×2 tile, so two tiles fit side by side in a row.
 *   time (Fraunces) · sun/moon · the city drawing as a die-cut sticker · NAME label · UTC offset
 * The sticker is the city's wallpaper drawing on a dark plate, with a white edge that follows
 * the drawing's outline and a soft shadow. A city without a drawing gets a round sun/moon sticker.
 */
object WorldStickerRenderer {

    /** Time colours you can pick per widget. "gold" follows the app's accent colour. */
    const val COLOR_CREAM = "cream"
    const val COLOR_GOLD = "gold"
    const val COLOR_WHITE = "white"
    val COLORS = listOf(COLOR_CREAM, COLOR_GOLD, COLOR_WHITE)

    private const val CREAM = 0xFFF3E9D2.toInt()
    private const val PLATE = 0xFF1B2430.toInt()
    private const val EDGE = 0xFFFFFDF7.toInt()
    private const val LABEL_TEXT = 0xFF1B2430.toInt()

    /** Time fonts you can pick per widget: key → name shown in the setup screen. */
    const val FONT_FRAUNCES = "fraunces"
    const val FONT_SKETCH = "sketch"
    val FONTS = listOf(
        FONT_FRAUNCES to "Fraunces",
        "cormorant" to "Cormorant",
        "dmserif" to "DM Serif",
        "outfit" to "Outfit",
        "anton" to "Anton",
        "bungee" to "Bungee",
        "rubikmono" to "Rubik Mono",
        FONT_SKETCH to "Handwriting",
    )

    private fun typeface(context: Context, key: String): Typeface {
        val id = when (key) {
            "cormorant" -> R.font.cormorant_bold
            "dmserif" -> R.font.dmserif_display
            "outfit" -> R.font.outfit_medium
            "anton" -> R.font.anton
            "bungee" -> R.font.bungee
            "rubikmono" -> R.font.rubik_mono_one
            else -> R.font.fraunces_bold
        }
        return runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: Typeface.SERIF
    }

    fun timeColor(context: Context, key: String): Int = when (key) {
        COLOR_GOLD -> PosterStyles.accent(context)
        COLOR_WHITE -> Color.WHITE
        else -> CREAM
    }

    private val stickers = HashMap<String, Bitmap>()

    /** "São Paulo" → "SAO PAULO" */
    private fun label(name: String) =
        Normalizer.normalize(name, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").uppercase()

    fun draw(context: Context, canvas: Canvas, w: Int, h: Int, city: WorldCity?, colorKey: String, fontKey: String = FONT_FRAUNCES) {
        val shadow = SketchRenderer.SHADOW
        if (city == null) {
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = w * 0.07f; textAlign = Paint.Align.CENTER
                setShadowLayer(textSize * 0.3f, 0f, textSize * 0.1f, shadow)
            }
            canvas.drawText("Add a city in", w / 2f, h / 2f - p.textSize * 0.2f, p)
            canvas.drawText("the World clock tab", w / 2f, h / 2f + p.textSize * 1.1f, p)
            return
        }

        val tz = cityTimeZone(city)
        val now = Instant.now()
        val t = now.atZone(tz.toZoneId())
        val is24 = android.text.format.DateFormat.is24HourFormat(context)
        val hh = if (is24) t.hour.toString().padStart(2, '0') else ((t.hour % 12).let { if (it == 0) 12 else it }).toString()
        val time = hh + ":" + t.minute.toString().padStart(2, '0')
        val ampm = if (is24) "" else if (t.hour < 12) "AM" else "PM"
        val night = t.hour < 6 || t.hour >= 18
        val gold = PosterStyles.accent(context)
        val timeColor = timeColor(context, colorKey)

        // Sizes from the tile; a 2×2 tile is roughly square, a taller one gets a bigger sticker.
        val u = min(w.toFloat(), h / 1.4f)
        val pad = u * 0.04f

        val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = typeface(context, fontKey)
            color = timeColor
            textSize = u * 0.27f
            setShadowLayer(textSize * 0.12f, 0f, textSize * 0.05f, shadow)
        }
        val ampmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            color = timeColor
            letterSpacing = 0.08f
            textSize = u * 0.075f
            setShadowLayer(textSize * 0.3f, 0f, textSize * 0.1f, shadow)
        }
        // Handwriting uses RR's hand-drawn digits (the Sketch poster's), sized by digit height
        val sketch = fontKey == FONT_SKETCH
        fun digitsH() = timePaint.textSize * 0.78f
        fun digitsW() = if (sketch) SketchRenderer.measure(context, time, digitsH()) else timePaint.measureText(time)
        fun timeWidth() = digitsW() +
            if (ampm.isEmpty()) 0f else u * 0.02f + ampmPaint.measureText(ampm)
        while (timeWidth() > w * 0.92f && timePaint.textSize > 8f) {
            timePaint.textSize *= 0.95f; ampmPaint.textSize *= 0.95f
        }

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            color = LABEL_TEXT
            letterSpacing = 0.1f
            textSize = u * 0.062f
        }
        val name = label(city.name)
        val pillPadX = u * 0.05f
        while (labelPaint.measureText(name) + pillPadX * 2 > w * 0.94f && labelPaint.textSize > 6f) labelPaint.textSize *= 0.94f
        val utcPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            color = Color.argb(225, 225, 230, 236)
            letterSpacing = 0.08f
            textSize = u * 0.056f
            setShadowLayer(textSize * 0.3f, 0f, textSize * 0.1f, shadow)
        }

        val timeH = digitsH()          // height of the digits
        val sunD = u * 0.09f
        val gap = u * 0.035f
        val pillH = labelPaint.textSize * 1.9f
        val utcH = utcPaint.textSize
        val fixed = timeH + gap + sunD + gap + gap + pillH + gap * 0.7f + utcH

        val sticker = sticker(context, city.name)
        val artMaxW = w * 0.96f
        val artMaxH = (h - pad * 2 - fixed).coerceAtLeast(u * 0.2f)
        val (artW, artH) = if (sticker != null) {
            val s = min(artMaxW / sticker.width, artMaxH / sticker.height)
            sticker.width * s to sticker.height * s
        } else {
            val d = min(artMaxH, u * 0.62f)
            d to d
        }
        val total = fixed + artH
        val cx = w / 2f
        var y = ((h - total) / 2f).coerceAtLeast(pad)

        // Time
        val tw = timeWidth()
        val tx = cx - tw / 2f
        if (sketch) SketchRenderer.drawText(context, canvas, time, tx, y + timeH, timeH, timeColor, true)
        else canvas.drawText(time, tx, y + timeH, timePaint)
        if (ampm.isNotEmpty()) canvas.drawText(ampm, tx + digitsW() + u * 0.02f, y + timeH, ampmPaint)
        y += timeH + gap

        // Sun by day, moon by night
        if (night) WorldSketchRenderer.drawMoon(canvas, cx, y + sunD / 2f, sunD / 2f, gold, true)
        else WorldSketchRenderer.drawSun(canvas, cx, y + sunD / 2f, sunD / 2f, gold, u * 0.012f, true)
        y += sunD + gap

        // The sticker
        if (sticker != null) {
            canvas.drawBitmap(sticker, null, RectF(cx - artW / 2f, y, cx + artW / 2f, y + artH),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            drawRoundSticker(canvas, cx, y + artH / 2f, artH / 2f, night, gold)
        }
        y += artH + gap

        // NAME on a white sticker label
        val nw = labelPaint.measureText(name)
        val pill = RectF(cx - nw / 2f - pillPadX, y, cx + nw / 2f + pillPadX, y + pillH)
        canvas.drawRoundRect(pill, pillH / 2f, pillH / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = EDGE; setShadowLayer(pillH * 0.25f, 0f, pillH * 0.12f, shadow)
        })
        val fm = labelPaint.fontMetrics
        canvas.drawText(name, cx - nw / 2f, pill.centerY() - (fm.ascent + fm.descent) / 2f, labelPaint)
        y += pillH + gap * 0.7f

        // UTC offset
        val off = tz.getOffset(now.toEpochMilli()) / 60000
        val utc = "UTC" + (if (off < 0) "−" else "+") + (abs(off) / 60) +
            (if (abs(off) % 60 != 0) ":" + (abs(off) % 60).toString().padStart(2, '0') else "")
        canvas.drawText(utc, cx - utcPaint.measureText(utc) / 2f, y + utcH * 0.82f, utcPaint)
    }

    /** A round sticker with a big sun or moon, for cities without a drawing. */
    private fun drawRoundSticker(c: Canvas, x: Float, y: Float, r: Float, night: Boolean, gold: Int) {
        val edge = r * 0.12f
        c.drawCircle(x, y + r * 0.05f, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x73000000; maskFilter = BlurMaskFilter(r * 0.12f, BlurMaskFilter.Blur.NORMAL)
        })
        c.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EDGE })
        c.drawCircle(x, y, r - edge, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PLATE })
        val s = (r - edge) * 0.55f
        if (night) WorldSketchRenderer.drawMoon(c, x, y, s, gold, false)
        else WorldSketchRenderer.drawSun(c, x, y, s, gold, r * 0.05f, false)
    }

    /**
     * The city's wallpaper drawing turned into a die-cut sticker: a dark plate following the
     * drawing's shape (holes filled), a white edge around it and a soft shadow under it.
     * Made once per city and kept.
     */
    private fun sticker(context: Context, city: String): Bitmap? {
        val art = WorldSketchRenderer.drawing(context, city, light = true) ?: return null
        return synchronized(stickers) { stickers.getOrPut(city) { makeSticker(art) } }
    }

    private fun makeSticker(art: Bitmap): Bitmap {
        // Work out the shape on a small grid (fast), then draw it smooth at full size.
        val g = 240
        val scale = g.toFloat() / art.width
        val gw = g
        val gh = (art.height * scale).toInt().coerceAtLeast(1)
        val plateR = 0.04f * g          // dark plate around the drawing
        val edgeR = plateR + 0.035f * g  // white edge around the plate
        val shadowR = 0.03f * g
        val p = (edgeR + shadowR * 2).toInt() + 2
        val W = gw + p * 2
        val H = gh + p * 2

        val small = Bitmap.createScaledBitmap(art, gw, gh, true)
        val px = IntArray(gw * gh)
        small.getPixels(px, 0, gw, 0, 0, gw, gh)

        // Distance (in grid pixels) from every pixel to the drawing — two-pass chamfer 3-4
        val big = Int.MAX_VALUE / 4
        val d = IntArray(W * H) { big }
        for (yy in 0 until gh) for (xx in 0 until gw) {
            if (Color.alpha(px[yy * gw + xx]) > 40) d[(yy + p) * W + xx + p] = 0
        }
        fun relax(i: Int, j: Int, cost: Int) { if (d[j] + cost < d[i]) d[i] = d[j] + cost }
        for (yy in 0 until H) for (xx in 0 until W) {
            val i = yy * W + xx
            if (xx > 0) relax(i, i - 1, 3)
            if (yy > 0) {
                relax(i, i - W, 3)
                if (xx > 0) relax(i, i - W - 1, 4)
                if (xx < W - 1) relax(i, i - W + 1, 4)
            }
        }
        for (yy in H - 1 downTo 0) for (xx in W - 1 downTo 0) {
            val i = yy * W + xx
            if (xx < W - 1) relax(i, i + 1, 3)
            if (yy < H - 1) {
                relax(i, i + W, 3)
                if (xx < W - 1) relax(i, i + W + 1, 4)
                if (xx > 0) relax(i, i + W - 1, 4)
            }
        }

        fun mask(r: Float): BooleanArray {
            val lim = (r * 3).toInt()
            val m = BooleanArray(W * H) { d[it] <= lim }
            // Fill holes: anything not reachable from the border is inside
            val out = BooleanArray(W * H)
            val stack = ArrayDeque<Int>()
            fun push(i: Int) { if (!m[i] && !out[i]) { out[i] = true; stack.addLast(i) } }
            for (xx in 0 until W) { push(xx); push((H - 1) * W + xx) }
            for (yy in 0 until H) { push(yy * W); push(yy * W + W - 1) }
            while (stack.isNotEmpty()) {
                val i = stack.removeLast(); val xx = i % W; val yy = i / W
                if (xx > 0) push(i - 1); if (xx < W - 1) push(i + 1)
                if (yy > 0) push(i - W); if (yy < H - 1) push(i + W)
            }
            return BooleanArray(W * H) { !out[it] }
        }

        // Alpha-only bitmaps: drawn in the paint's colour, and the blur works on them
        fun maskBitmap(m: BooleanArray): Bitmap {
            val a = IntArray(W * H) { if (m[it]) Color.WHITE else Color.TRANSPARENT }
            val argb = Bitmap.createBitmap(a, W, H, Bitmap.Config.ARGB_8888)
            return argb.extractAlpha().also { argb.recycle() }
        }

        val plate = maskBitmap(mask(plateR))
        val edge = maskBitmap(mask(edgeR))

        // Full-size sticker
        val outScale = art.width.toFloat() / gw
        val ow = (W * outScale).toInt()
        val oh = (H * outScale).toInt()
        val out = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val dst = RectF(0f, 0f, ow.toFloat(), oh.toFloat())
        val smooth = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Soft shadow, a little below
        val sh = shadowR * outScale
        c.save(); c.translate(0f, sh * 0.6f)
        c.drawBitmap(edge, null, dst, Paint(smooth).apply {
            color = 0x80000000.toInt()
            maskFilter = BlurMaskFilter(sh, BlurMaskFilter.Blur.NORMAL)
        })
        c.restore()
        // White edge, then the dark plate (slightly blurred so the upscaled grid edges look smooth)
        val soften = BlurMaskFilter(outScale * 0.8f, BlurMaskFilter.Blur.NORMAL)
        c.drawBitmap(edge, null, dst, Paint(smooth).apply { color = EDGE; maskFilter = soften })
        c.drawBitmap(plate, null, dst, Paint(smooth).apply { color = PLATE; maskFilter = soften })
        // The drawing on top
        c.drawBitmap(art, p * outScale, p * outScale, smooth)

        plate.recycle(); edge.recycle(); small.takeIf { it !== art }?.recycle()
        return out
    }
}
