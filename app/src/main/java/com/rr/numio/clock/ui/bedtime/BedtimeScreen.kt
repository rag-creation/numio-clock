package com.rr.numio.clock.ui.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.data.AlarmRepository
import com.rr.numio.clock.data.Bedtime
import com.rr.numio.clock.data.BedtimeSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private val Amber = Color(0xFFFFC21A)
private val Cream = Color(0xFFF1EEE6)
private val Grey = Color(0xFFA09C94)

/** The calm page the bedtime reminder opens: when to sleep, the countdown, and Voyager 1. */
@Composable
fun BedtimeScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alarms by remember { AlarmRepository(context).alarms }.collectAsState(initial = emptyList())
    val settings by Bedtime.settings(context).collectAsState(initial = BedtimeSettings())

    // Tick every 30 s so the countdown stays right while the page is open
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); now = System.currentTimeMillis() }
    }

    val plan = Bedtime.tonight(alarms, settings, now)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        NightSky(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text("BEDTIME · NUMIO", fontSize = 13.sp, letterSpacing = 4.sp, color = Amber)
            Spacer(modifier = Modifier.height(28.dp))

            if (plan == null) {
                Text("No morning alarm", fontSize = 34.sp, fontWeight = FontWeight.Light, color = Cream)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Nothing rings tomorrow morning. Sleep in.", fontSize = 15.sp, color = Grey, textAlign = TextAlign.Center)
            } else {
                val ring = time.format(Date(plan.ring))
                val left = ((plan.ring - now) / 60_000L).toInt().coerceAtLeast(0)
                if (now < plan.bedtime) {
                    Text("Sleep by", fontSize = 18.sp, color = Grey)
                    BigTime(time.format(Date(plan.bedtime)))
                    Text("${Bedtime.hoursText(settings.goalMinutes)} before your $ring alarm", fontSize = 15.sp, color = Grey)
                } else {
                    Text("It's past bedtime", fontSize = 18.sp, color = Grey)
                    Text("Sleep now", fontSize = 54.sp, fontWeight = FontWeight.Light, color = Cream)
                    Text("Your alarm is at $ring", fontSize = 15.sp, color = Grey)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1F1D18))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text("${left / 60}h ${left % 60}m to alarm", fontSize = 13.sp, color = Amber)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                "Voyager 1 is about ${Voyager.billionKm(now)} billion km away tonight,\nstill travelling since 1977. You can rest.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF8A8680),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Amber)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Text("Good night", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1400)) }

            if (plan != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallButton("In 15 min", Modifier.weight(1f)) {
                        Bedtime.later(context)
                        onClose()
                    }
                    SmallButton("Skip ${time.format(Date(plan.ring))} alarm", Modifier.weight(1f)) {
                        scope.launch {
                            Bedtime.skipComingAlarm(context)
                            onClose()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BigTime(text: String) {
    // "11:12 PM" → big "11:12", small "PM"
    val parts = text.split(' ', ' ', ' ')
    Row(verticalAlignment = Alignment.Bottom) {
        Text(parts.first(), fontSize = 64.sp, fontWeight = FontWeight.Light, color = Cream)
        if (parts.size > 1) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(parts.drop(1).joinToString(" "), fontSize = 22.sp, color = Grey, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun SmallButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F1F1F))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 14.sp, color = Color(0xFFCFCFCF), maxLines = 1) }
}
