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
        List(260) {
            val t = rnd.nextFloat()
            val spread = (rnd.nextFloat() - 0.5f) * 0.16f * (1f + rnd.nextFloat())
            Star(t, 0.62f - t * 0.55f + spread, 0.4f + rnd.nextFloat() * 0.7f, 0.08f + rnd.nextFloat() * 0.22f, false)
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

        band.forEachIndexed { i, s ->
            drawCircle(if (i % 3 == 0) Haze else Cream, s.r * dp, Offset(s.x * w, s.y * h), alpha = s.a)
        }
        stars.forEach { s ->
            drawCircle(Cream, s.r * dp, Offset(s.x * w, s.y * h), alpha = if (s.twinkles) s.a * twinkle else s.a)
        }

        moon(Offset(w * 0.5f, h * 0.24f), 58f * dp)
        voyager(Offset(w * 0.2f, h * 0.12f), dp)
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
        Cream.copy(alpha = 0.3f), at + Offset(30 * dp, 18 * dp), at + Offset(130 * dp, 34 * dp),
        strokeWidth = 0.8f * dp, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * dp, 6 * dp)), cap = StrokeCap.Round
    )
    translate(at.x, at.y) {
        rotate(-20f, pivot = Offset.Zero) {
            // high-gain dish (seen at an angle) and its feed
            drawOval(c.copy(alpha = 0.18f), Offset(-17 * dp, -6 * dp), Size(34 * dp, 12 * dp))
            drawOval(c, Offset(-17 * dp, -6 * dp), Size(34 * dp, 12 * dp), style = Stroke(line))
            drawLine(c, Offset(0f, 0f), Offset(0f, -9 * dp), line)
            drawCircle(c, 1.2f * dp, Offset(0f, -9.5f * dp))
            // ten-sided body under the dish
            val body = Path().apply {
                for (i in 0 until 10) {
                    val a = Math.toRadians(i * 36.0)
                    val p = Offset((cos(a) * 6.5 * dp).toFloat(), (6 * dp + sin(a) * 3 * dp).toFloat())
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(body, c.copy(alpha = 0.3f))
            drawPath(body, c, style = Stroke(line))
            // magnetometer boom: very long and thin, with lattice ticks
            drawLine(c.copy(alpha = 0.7f), Offset(5 * dp, 7 * dp), Offset(58 * dp, 26 * dp), 0.7f * dp)
            for (i in 1..6) {
                val p = Offset(5 * dp + i * 8.8f * dp, 7 * dp + i * 3.2f * dp)
                drawLine(c.copy(alpha = 0.5f), p + Offset(-1 * dp, 2 * dp), p + Offset(1 * dp, -2 * dp), 0.6f * dp)
            }
            // power boom with three RTGs
            drawLine(c, Offset(-5 * dp, 7 * dp), Offset(-26 * dp, 16 * dp), line)
            for (i in 0..2) {
                val p = Offset(-17 * dp - i * 4.2f * dp, 12.2f * dp + i * 1.8f * dp)
                drawCircle(c, 1.9f * dp, p)
            }
            // science boom with the camera platform
            drawLine(c, Offset(1 * dp, 9 * dp), Offset(4 * dp, 22 * dp), line)
            drawRect(c, Offset(1.5f * dp, 21 * dp), Size(5 * dp, 3.5f * dp))
        }
    }
}
