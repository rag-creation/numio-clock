package com.rr.numio.clock.ui.alarm

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun ChasetheMoonScreen(
    hour: Int = 7,
    minute: Int = 0,
    label: String = "Wake up",
    snoozeMinutes: Int = 10,
    onSnooze: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Live current time
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000)
        }
    }
    val h = currentTime.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val m = currentTime.get(Calendar.MINUTE).toString().padStart(2, '0')
    val ap = if (currentTime.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

    // Moon position
    val moonX = remember { Animatable(0f) }
    val moonY = remember { Animatable(0f) }
    var boxWidth by remember { mutableStateOf(0f) }
    var boxHeight by remember { mutableStateOf(0f) }
    var tapCount by remember { mutableStateOf(0) }
    var speed by remember { mutableStateOf(800) } // animation duration ms
    var snoozed by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }

    // Dismiss hold state
    var dismissProgress by remember { mutableStateOf(0f) }
    var holdingDismiss by remember { mutableStateOf(false) }

    val moonSizePx = with(density) { 72.dp.toPx() }

    // Move moon to random position
    fun jumpMoon() {
        if (boxWidth == 0f || boxHeight == 0f) return
        val targetX = Random.nextFloat() * (boxWidth - moonSizePx)
        val targetY = Random.nextFloat() * (boxHeight - moonSizePx - with(density) { 160.dp.toPx() })
        scope.launch {
            moonX.animateTo(targetX, animationSpec = tween(speed))
        }
        scope.launch {
            moonY.animateTo(targetY, animationSpec = tween(speed))
        }
    }

    // Start moon drifting automatically
    LaunchedEffect(boxWidth, boxHeight) {
        if (boxWidth > 0f && boxHeight > 0f) {
            // Place moon at center initially
            moonX.snapTo(boxWidth / 2f - moonSizePx / 2f)
            moonY.snapTo(boxHeight / 3f - moonSizePx / 2f)
            // Start drifting
            while (!snoozed && !dismissed) {
                delay(speed.toLong() + 300)
                if (!snoozed && !dismissed) jumpMoon()
            }
        }
    }

    // Dismiss hold progress
    LaunchedEffect(holdingDismiss) {
        if (holdingDismiss) {
            val steps = 50
            val stepDelay = 5000L / steps
            repeat(steps) {
                if (!holdingDismiss) return@repeat
                delay(stepDelay)
                dismissProgress += 1f / steps
            }
            if (holdingDismiss && dismissProgress >= 0.99f) {
                dismissed = true
                onDismiss()
            }
        } else {
            while (dismissProgress > 0f) {
                delay(16)
                dismissProgress = (dismissProgress - 0.03f).coerceAtLeast(0f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NumioDark)
            .onGloballyPositioned { coords ->
                boxWidth = coords.size.width.toFloat()
                boxHeight = coords.size.height.toFloat()
            }
    ) {
        if (!snoozed && !dismissed) {

            // Time display at top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "$h:$m",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.W100,
                        color = NumioTextPrimary,
                        letterSpacing = (-3).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ap,
                        fontSize = 24.sp,
                        color = AppColor.accent.value,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Text(
                    text = label.uppercase(),
                    fontSize = 11.sp,
                    color = NumioTextMuted,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tap counter
                Text(
                    text = when (tapCount) {
                        0 -> "Tap the moon 3 times to snooze"
                        1 -> "2 more taps... 🌙"
                        2 -> "One more! 🌙🌙"
                        else -> "🌙🌙🌙"
                    },
                    fontSize = 13.sp,
                    color = NumioTextMuted,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tap dots indicator
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < tapCount) AppColor.accent.value
                                    else Color(0xFF2A2A2A)
                                )
                        )
                    }
                }
            }

            // Moon button — floats freely
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            moonX.value.roundToInt(),
                            moonY.value.roundToInt()
                        )
                    }
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A2E))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (tapCount < 2) {
                                tapCount++
                                // Speed up each tap
                                speed = (speed * 0.6f).toInt().coerceAtLeast(150)
                                jumpMoon()
                            } else {
                                // 3rd tap — snooze!
                                tapCount = 3
                                snoozed = true
                                onSnooze()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌙",
                    fontSize = 36.sp
                )
            }

            // Dismiss button at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF111111))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    holdingDismiss = true
                                    tryAwaitRelease()
                                    holdingDismiss = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Progress fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(dismissProgress)
                            .align(Alignment.CenterStart)
                            .background(Color(0x25E04040))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Dismiss",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W300,
                            color = NumioRed
                        )
                        Text(
                            text = if (holdingDismiss) "keep holding..." else "hold 5 seconds",
                            fontSize = 10.sp,
                            color = Color(0xFF444444),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Result overlay
        if (snoozed || dismissed) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (snoozed) "🌙" else "✓",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (snoozed) "Snoozed" else "Alarm off",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.W200,
                        color = NumioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (snoozed) "Ringing again in $snoozeMinutes minutes"
                        else "See you tomorrow! 💛",
                        fontSize = 14.sp,
                        color = NumioTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}