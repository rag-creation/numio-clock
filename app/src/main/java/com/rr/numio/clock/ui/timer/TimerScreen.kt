package com.rr.numio.clock.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.R
import com.rr.numio.clock.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TimerScreen() {
    var selectedMinutes by remember { mutableStateOf(25) }
    var selectedSeconds by remember { mutableStateOf(0) }
    var totalSeconds by remember { mutableStateOf(25 * 60) }
    var secondsLeft by remember { mutableStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinInput by remember { mutableStateOf("") }
    var customSecInput by remember { mutableStateOf("") }

    val presets = listOf(5, 25, 45, 60)

    LaunchedEffect(isRunning) {
        while (isRunning && secondsLeft > 0) {
            delay(1000)
            secondsLeft--
            if (secondsLeft == 0) {
                isRunning = false
                isFinished = true
            }
        }
    }

    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds.toFloat() else 0f
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TIMER · NUMIO",
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            color = AppColor.accent.value,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Ring
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                val inset = strokeWidth / 2
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = Color(0xFF1A1A1A),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                if (progress > 0f) {
                    drawArc(
                        color = if (isFinished) NumioRed else AppColor.accent.value,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isFinished) "Done!"
                    else "${minutes.toString().padStart(2,'0')}:${seconds.toString().padStart(2,'0')}",
                    fontSize = if (isFinished) 28.sp else 42.sp,
                    fontWeight = FontWeight.W200,
                    color = if (isFinished) NumioRed else NumioTextPrimary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (isFinished) "Timer complete"
                    else if (isRunning) "focus" else "ready",
                    fontSize = 11.sp,
                    color = NumioTextMuted,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preset pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { min ->
                val isSelected = selectedMinutes == min && selectedSeconds == 0 && !isRunning
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) Color(0x20F5C427) else Color(0xFF161616)
                        )
                        .clickable {
                            if (!isRunning) {
                                selectedMinutes = min
                                selectedSeconds = 0
                                totalSeconds = min * 60
                                secondsLeft = min * 60
                                isFinished = false
                                showCustomInput = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (min == 60) "1 hr" else "$min min",
                        fontSize = 12.sp,
                        color = if (isSelected) AppColor.accent.value else NumioTextMuted
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (showCustomInput) Color(0x20F5C427) else Color(0xFF161616)
                    )
                    .clickable {
                        if (!isRunning) showCustomInput = !showCustomInput
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "custom",
                    fontSize = 12.sp,
                    color = if (showCustomInput) AppColor.accent.value else NumioTextMuted
                )
            }
        }

        // Custom input
        if (showCustomInput && !isRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = customMinInput,
                    onValueChange = {
                        if (it.length <= 2) customMinInput = it.filter { c -> c.isDigit() }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        color = NumioTextPrimary,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    ),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1F1F1F))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customMinInput.isEmpty()) {
                                Text(
                                    "mm", fontSize = 20.sp,
                                    color = NumioTextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            inner()
                        }
                    }
                )
                Text(":", fontSize = 24.sp, color = NumioTextMuted)
                BasicTextField(
                    value = customSecInput,
                    onValueChange = {
                        if (it.length <= 2) customSecInput = it.filter { c -> c.isDigit() }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        color = NumioTextPrimary,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    ),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1F1F1F))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customSecInput.isEmpty()) {
                                Text(
                                    "ss", fontSize = 20.sp,
                                    color = NumioTextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            inner()
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColor.accent.value)
                        .clickable {
                            val mins = customMinInput.toIntOrNull() ?: 0
                            val secs = customSecInput.toIntOrNull() ?: 0
                            val total = mins * 60 + secs
                            if (total > 0) {
                                selectedMinutes = mins
                                selectedSeconds = secs
                                totalSeconds = total
                                secondsLeft = total
                                isFinished = false
                                showCustomInput = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Set", fontSize = 14.sp,
                        color = NumioDark,
                        fontWeight = FontWeight.W500
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161616))
                    .clickable {
                        isRunning = false
                        secondsLeft = totalSeconds
                        isFinished = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_reset),
                    contentDescription = "Reset",
                    tint = NumioTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Play/Pause
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(AppColor.accent.value)
                    .clickable {
                        if (isFinished) {
                            secondsLeft = totalSeconds
                            isFinished = false
                            isRunning = true
                        } else {
                            isRunning = !isRunning
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isRunning) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isRunning) "Pause" else "Play",
                    tint = NumioDark,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Bell
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161616))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bell),
                    contentDescription = "Sound",
                    tint = NumioTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}