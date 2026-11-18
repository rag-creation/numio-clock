package com.rr.numio.clock.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rr.numio.clock.data.Bedtime
import com.rr.numio.clock.data.BedtimeSettings
import com.rr.numio.clock.ui.alarm.NumioToggle
import com.rr.numio.clock.ui.theme.*
import kotlinx.coroutines.launch

/** Settings → Bedtime: on/off, sleep goal and when to be reminded. */
@Composable
fun BedtimeSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by Bedtime.settings(context).collectAsState(initial = BedtimeSettings())
    fun save(s: BedtimeSettings) { scope.launch { Bedtime.save(context, s) } }

    // The reminder is a notification, so ask for that the first time it's turned on
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Spacer(modifier = Modifier.height(8.dp))
    SettingsSectionTitle("Bedtime")
    Spacer(modifier = Modifier.height(12.dp))
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Bedtime reminder", fontSize = 14.sp, color = NumioTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "A gentle note before your next morning alarm, so you get enough sleep",
                    fontSize = 12.sp,
                    color = NumioTextMuted,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            NumioToggle(isOn = settings.enabled, onToggle = {
                val on = !settings.enabled
                if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                save(settings.copy(enabled = on))
            })
        }
        if (settings.enabled) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Sleep goal", fontSize = 12.sp, color = NumioTextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            ChoiceRow(
                labels = listOf("7 h", "7½ h", "8 h", "9 h"),
                values = listOf(420, 450, 480, 540),
                selected = settings.goalMinutes,
                onPick = { save(settings.copy(goalMinutes = it)) }
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("Remind me", fontSize = 12.sp, color = NumioTextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            ChoiceRow(
                labels = listOf("At bedtime", "15 min before", "30 min before"),
                values = listOf(0, 15, 30),
                selected = settings.leadMinutes,
                onPick = { save(settings.copy(leadMinutes = it)) }
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun ChoiceRow(labels: List<String>, values: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, label ->
            val on = values[i] == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Color(0x20F5C427) else Color(0xFF1F1F1F))
                    .clickable { onPick(values[i]) }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    color = if (on) AppColor.accent.value else NumioTextMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
