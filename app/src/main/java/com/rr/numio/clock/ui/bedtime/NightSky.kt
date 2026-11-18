package com.rr.numio.clock.ui.bedtime

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Cream = Color(0xFFF1EEE6)
private val Amber = Color(0xFFFFC21A)
private val Haze = Color(0xFF8E7CC3)

private class Star(val x: Float, val y: Float, val r: Float, val a: Float, val twinkles: Boolean)

/**
 * The bedtime sky, drawn in code so it's crisp on every screen: stars, a faint
 * Milky Way band, an amber crescent moon with a soft glow, and a small
 * Voyager 1 drifting top-left with a dotted trail.
 */
@Composable
fun NightSky(modifier: Modifier = Modifier) {
    val stars = remember {
        val rnd = Random(1977)
        List(110) { Star(rnd.nextFloat(), rnd.nextFloat() * 0.62f, 0.6f + rnd.nextFloat() * 1.2f, 0.25f + rnd.nextFloat() * 0.6f, rnd.nextInt(8) == 0) }
    }
    // The Milky Way: many tiny dim dots along a diagonal band
    val band = remember {
        val rnd = Random(1990)
        List(520) {
            val t = rnd.nextFloat()
            // denser in the middle of the band, thinning out at its edges
            val spread = (rnd.nextFloat() + rnd.nextFloat() - 1f) * 0.12f
            Star(t, 0.66f - t * 0.6f + spread, 0.4f + rnd.nextFloat() * 0.8f, 0.14f + rnd.nextFloat() * 0.3f, false)
        }
    }
    val twinkle by rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dp = density

        // a soft glow along the band, then its dots
        drawLine(
            Brush.linearGradient(
                listOf(Color.Transparent, Haze.copy(alpha = 0.07f), Cream.copy(alpha = 0.05f), Haze.copy(alpha = 0.07f), Color.Transparent),
                start = Offset(0f, 0.66f * h), end = Offset(w, 0.06f * h)
            ),
            Offset(0f, 0.66f * h), Offset(w, 0.06f * h), strokeWidth = 0.16f * h, cap = StrokeCap.Round
        )
        band.forEachIndexed { i, s ->
            drawCircle(if (i % 3 == 0) Haze else Cream, s.r * dp, Offset(s.x * w, s.y * h), alpha = s.a)
        }
        stars.forEach { s ->
            drawCircle(Cream, s.r * dp, Offset(s.x * w, s.y * h), alpha = if (s.twinkles) s.a * twinkle else s.a)
        }

        moon(Offset(w * 0.5f, h * 0.31f), 58f * dp)
        voyager(Offset(w * 0.24f, h * 0.13f), dp * 1.9f)
    }
}

/** Amber crescent with a soft glow behind it. */
private fun DrawScope.moon(c: Offset, r: Float) {
    drawCircle(
        Brush.radialGradient(listOf(Amber.copy(alpha = 0.22f), Color.Transparent), center = c, radius = r * 2.1f),
        radius = r * 2.1f, center = c
    )
    val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
    val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + r * 0.46f, c.y - r * 0.34f), r * 0.9f)) }
    val crescent = Path().apply { op(disc, bite, PathOperation.Difference) }
    drawPath(crescent, Amber)
}

/**
 * Voyager 1, simplified but true to its shape: the big dish facing Earth, the
 * ten-sided body under it, the long magnetometer boom, the power boom with
 * its three RTGs, and the science boom with the camera platform.
 */
private fun DrawScope.voyager(at: Offset, dp: Float) {
    val line = 1.2f * dp
    val c = Cream.copy(alpha = 0.9f)
    // trail it has left behind
    drawLine(
        Cream.copy(alpha = 0.3f), at + Offset(40 * dp, 22 * dp), at + Offset(95 * dp, 34 * dp),
        strokeWidth = 0.8f * dp, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * dp, 6 * dp)), cap = StrokeCap.Round
    )
    translate(at.x, at.y) {
        rotate(-16f, pivot = Offset.Zero) {
            // high-gain dish, seen at an angle: rim, inner ring and the feed on its struts
            drawOval(c.copy(alpha = 0.16f), Offset(-18 * dp, -7 * dp), Size(36 * dp, 13 * dp))
            drawOval(c, Offset(-18 * dp, -7 * dp), Size(36 * dp, 13 * dp), style = Stroke(line))
            drawOval(c.copy(alpha = 0.45f), Offset(-9 * dp, -3.2f * dp), Size(18 * dp, 6.5f * dp), style = Stroke(0.7f * dp))
            drawLine(c, Offset(-8 * dp, -1 * dp), Offset(0f, -11 * dp), 0.7f * dp)
            drawLine(c, Offset(8 * dp, -1 * dp), Offset(0f, -11 * dp), 0.7f * dp)
            drawCircle(c, 1.3f * dp, Offset(0f, -11.5f * dp))
            // ten-sided body under the dish
            val body = Path().apply {
                for (i in 0 until 10) {
                    val a = Math.toRadians(i * 36.0)
                    val p = Offset((cos(a) * 7 * dp).toFloat(), (8 * dp + sin(a) * 3.2 * dp).toFloat())
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(body, c.copy(alpha = 0.35f))
            drawPath(body, c, style = Stroke(line))
            // magnetometer boom: very long, very thin, a small sensor at its tip
            drawLine(c.copy(alpha = 0.55f), Offset(6 * dp, 8 * dp), Offset(62 * dp, 25 * dp), 0.6f * dp)
            drawCircle(c.copy(alpha = 0.8f), 1.1f * dp, Offset(62 * dp, 25 * dp))
            // power boom with its three RTGs: short cylinders end to end along the boom
            drawLine(c, Offset(-6 * dp, 9 * dp), Offset(-16 * dp, 14 * dp), line)
            val ux = -0.894f
            val uy = 0.447f
            for (i in 0..2) {
                val along = (2.8f + i * 5.2f) * dp
                val centre = Offset(-16 * dp + ux * along, 14 * dp + uy * along)
                rotate(-26.6f, pivot = centre) {
                    drawRoundRect(
                        c, Offset(centre.x - 2.3f * dp, centre.y - 1.8f * dp), Size(4.6f * dp, 3.6f * dp),
                        androidx.compose.ui.geometry.CornerRadius(0.8f * dp)
                    )
                }
            }
            // science boom with the camera platform at its end
            drawLine(c, Offset(2 * dp, 11 * dp), Offset(9 * dp, 25 * dp), line)
            drawRect(c, Offset(7 * dp, 24 * dp), Size(6 * dp, 4 * dp))
            drawLine(c, Offset(13 * dp, 26 * dp), Offset(16 * dp, 26 * dp), 0.8f * dp)
        }
    }
}
