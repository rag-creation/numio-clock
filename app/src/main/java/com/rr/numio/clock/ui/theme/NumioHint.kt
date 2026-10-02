package com.rr.numio.clock.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small glowing pill used for gesture hints ("Hold … to …").
 * Pulses a few times when it appears so the eye catches it, then settles to a soft glow.
 */
@Composable
fun NumioHint(text: String, modifier: Modifier = Modifier) {
    val accent = AppColor.accent.value
    val glow = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        repeat(3) {
            glow.animateTo(1f, tween(700))
            glow.animateTo(0f, tween(700))
        }
        glow.animateTo(0.35f, tween(500))
    }

    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        fontSize = 10.sp,
        letterSpacing = 0.3.sp,
        color = accent.copy(alpha = 0.55f + 0.4f * glow.value),
        modifier = modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.04f + 0.08f * glow.value))
            .border(0.5.dp, accent.copy(alpha = 0.15f + 0.45f * glow.value), shape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}
