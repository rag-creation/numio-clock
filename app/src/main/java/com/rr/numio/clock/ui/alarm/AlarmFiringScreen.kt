package com.rr.numio.clock.ui.alarm

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.R
import com.rr.numio.clock.data.AlarmModel
import com.rr.numio.clock.data.AlarmReceiver
import com.rr.numio.clock.data.AlarmScheduler
import com.rr.numio.clock.data.AlarmService
import com.rr.numio.clock.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar

enum class AlarmResult { NONE, SNOOZED, DISMISSED }

@Composable
fun AlarmFiringScreen(
    hour: Int = 7,
    minute: Int = 0,
    label: String = "Wake up",
    alarmId: Int = 0,
    snoozeMinutes: Int = 10,
    onSnooze: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var result by remember { mutableStateOf(AlarmResult.NONE) }
    var secondsLeft by remember { mutableStateOf(20) }

    var snoozeProgress by remember { mutableStateOf(0f) }
    var dismissProgress by remember { mutableStateOf(0f) }
    var holdingSnooze by remember { mutableStateOf(false) }
    var holdingDismiss by remember { mutableStateOf(false) }

    val snoozeDuration = 3000L
    val dismissDuration = 5000L

    // FIX #1 — Live current time instead of alarm set time
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

    LaunchedEffect(result) {
        if (result != AlarmResult.NONE) {
            AlarmReceiver.stopAlarm()
            AlarmService.stop(context)
            val manager = context.getSystemService(
                android.app.NotificationManager::class.java
            )
            manager.cancelAll()
        }
    }

    // Auto-snooze after 20 seconds
    LaunchedEffect(Unit) {
        for (i in 0 until 20) {
            delay(1000)
            if (result != AlarmResult.NONE) return@LaunchedEffect
            secondsLeft--
        }
        if (result == AlarmResult.NONE) {
            AlarmScheduler.snooze(
                context,
                AlarmModel(
                    id = alarmId,
                    hour = hour,
                    minute = minute,
                    label = label,
                    days = List(7) { false },
                    isEnabled = true
                ),
                snoozeMinutes
            )
            AlarmReceiver.stopAlarm()
            AlarmService.stop(context)
            result = AlarmResult.SNOOZED
            onSnooze()
        }
    }

    LaunchedEffect(holdingSnooze) {
        if (holdingSnooze) {
            val steps = 50
            val stepDelay = snoozeDuration / steps
            repeat(steps) {
                if (!holdingSnooze) return@repeat
                delay(stepDelay)
                snoozeProgress += 1f / steps
            }
            if (holdingSnooze && snoozeProgress >= 0.99f) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                )
                AlarmScheduler.snooze(
                    context,
                    AlarmModel(
                        id = alarmId,
                        hour = hour,
                        minute = minute,
                        label = label,
                        days = List(7) { false },
                        isEnabled = true
                    ),
                    snoozeMinutes
                )
                result = AlarmResult.SNOOZED
                onSnooze()
            }
        } else {
            while (snoozeProgress > 0f) {
                delay(16)
                snoozeProgress = (snoozeProgress - 0.03f).coerceAtLeast(0f)
            }
        }
    }

    LaunchedEffect(holdingDismiss) {
        if (holdingDismiss) {
            val steps = 50
            val stepDelay = dismissDuration / steps
            repeat(steps) {
                if (!holdingDismiss) return@repeat
                delay(stepDelay)
                dismissProgress += 1f / steps
            }
            if (holdingDismiss && dismissProgress >= 0.99f) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                )
                result = AlarmResult.DISMISSED
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
    ) {
        if (result == AlarmResult.NONE) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "$h:$m",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.W100,
                        color = NumioTextPrimary,
                        letterSpacing = (-3).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ap,
                        fontSize = 28.sp,
                        color = AppColor.accent.value,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }

                Text(
                    text = label.uppercase(),
                    fontSize = 11.sp,
                    color = NumioTextMuted,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Auto snooze in $secondsLeft sec",
                    fontSize = 11.sp,
                    color = Color(0xFF333333),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Snooze button
                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF161616))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    holdingSnooze = true
                                    tryAwaitRelease()
                                    holdingSnooze = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(snoozeProgress)
                            .align(Alignment.CenterStart)
                            .background(Color(0x30F5C427))
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_moon),
                            contentDescription = "Snooze",
                            tint = AppColor.accent.value,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Snooze",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W300,
                                color = AppColor.accent.value
                            )
                            Text(
                                text = if (holdingSnooze) "keep holding..." else "hold 3 seconds",
                                fontSize = 11.sp,
                                color = Color(0xFF555555),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dismiss button
                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(24.dp))
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
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(dismissProgress)
                            .align(Alignment.CenterStart)
                            .background(Color(0x25E04040))
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bell_off),
                            contentDescription = "Dismiss",
                            tint = NumioRed,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Dismiss",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W300,
                                color = NumioRed
                            )
                            Text(
                                text = if (holdingDismiss) "keep holding..." else "hold 5 seconds",
                                fontSize = 11.sp,
                                color = Color(0xFF444444),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Result overlay
        if (result != AlarmResult.NONE) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(
                            id = if (result == AlarmResult.SNOOZED)
                                R.drawable.ic_moon else R.drawable.ic_bell_off
                        ),
                        contentDescription = null,
                        tint = if (result == AlarmResult.SNOOZED)
                            AppColor.accent.value else NumioRed,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (result == AlarmResult.SNOOZED) "Snoozed" else "Alarm off",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.W200,
                        color = NumioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (result == AlarmResult.SNOOZED)
                            "Ringing again in $snoozeMinutes minutes"
                        else
                            "See you tomorrow! 💛",
                        fontSize = 14.sp,
                        color = NumioTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}