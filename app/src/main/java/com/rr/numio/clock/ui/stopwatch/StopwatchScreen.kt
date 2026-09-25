package com.rr.numio.clock.ui.stopwatch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.R
import com.rr.numio.clock.data.WorldCityStore
import com.rr.numio.clock.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StopwatchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var laps by remember { mutableStateOf(listOf<Long>()) }
    var loaded by remember { mutableStateOf(false) }

    // Load saved state on first composition
    LaunchedEffect(Unit) {
        WorldCityStore.getStopwatchState(context).collect { (savedMs, savedLaps) ->
            if (!loaded) {
                elapsedMs = savedMs
                laps = savedLaps
                loaded = true
            }
        }
    }

    // Timer tick
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(10)
            elapsedMs += 10
        }
    }

    // Save state whenever elapsedMs or laps change (but not too often — every 500ms)
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(500)
            WorldCityStore.saveStopwatchState(context, elapsedMs, laps)
        }
    }

    val minutes = (elapsedMs / 60000).toInt()
    val seconds = ((elapsedMs % 60000) / 1000).toInt()
    val millis  = ((elapsedMs % 1000) / 10).toInt()

    val lapTimes = laps.mapIndexed { i, t ->
        if (i == 0) t else t - laps[i - 1]
    }
    val bestIdx  = if (lapTimes.size > 2) lapTimes.indexOf(lapTimes.min()) else -1
    val worstIdx = if (lapTimes.size > 2) lapTimes.indexOf(lapTimes.max()) else -1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "STOPWATCH · NUMIO",
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            color = AppColor.accent.value,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Time display
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${minutes.toString().padStart(2,'0')}:${seconds.toString().padStart(2,'0')}",
                fontSize = 64.sp,
                fontWeight = FontWeight.W100,
                color = NumioTextPrimary,
                letterSpacing = (-2).sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = ".${millis.toString().padStart(2,'0')}",
                fontSize = 28.sp,
                fontWeight = FontWeight.W300,
                color = AppColor.accent.value,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Canvas(modifier = Modifier.width(50.dp).height(2.dp)) {
            drawRect(color = AppColor.accent.value)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lap
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161616))
                    .clickable {
                        if (isRunning) {
                            laps = laps + elapsedMs
                            scope.launch {
                                WorldCityStore.saveStopwatchState(context, elapsedMs, laps)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_flag),
                    contentDescription = "Lap",
                    tint = if (isRunning) NumioTextSecondary else NumioTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Play/Pause
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(AppColor.accent.value)
                    .clickable { isRunning = !isRunning },
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

            // Reset
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161616))
                    .clickable {
                        isRunning = false
                        elapsedMs = 0L
                        laps = listOf()
                        scope.launch {
                            WorldCityStore.clearStopwatchState(context)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Stop",
                    tint = NumioTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (laps.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("LAP", fontSize = 10.sp, color = NumioTextMuted, letterSpacing = 1.5.sp)
                Text("TIME", fontSize = 10.sp, color = NumioTextMuted, letterSpacing = 1.5.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(laps.reversed()) { reversedIndex, lapMs ->
                    val actualIndex = laps.size - 1 - reversedIndex
                    val lapTime = if (actualIndex == 0) lapMs
                    else lapMs - laps[actualIndex - 1]

                    val lapColor = when (actualIndex) {
                        bestIdx  -> AppColor.accent.value
                        worstIdx -> NumioRed
                        else     -> NumioTextSecondary
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lap ${actualIndex + 1}",
                            fontSize = 12.sp,
                            color = NumioTextMuted
                        )
                        Text(
                            text = formatLapTime(lapTime),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W300,
                            color = lapColor
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                        drawRect(color = Color(0xFF1A1A1A))
                    }
                }
            }
        }
    }
}

fun formatLapTime(ms: Long): String {
    val minutes = (ms / 60000).toInt()
    val seconds = ((ms % 60000) / 1000).toInt()
    val millis  = ((ms % 1000) / 10).toInt()
    return "${minutes.toString().padStart(2,'0')}:${seconds.toString().padStart(2,'0')}.${millis.toString().padStart(2,'0')}"
}