package com.rr.numio.clock.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.clock.data.AlarmModel
import com.rr.numio.clock.data.AlarmRepository
import com.rr.numio.clock.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun AlarmScreen() {
    val context = LocalContext.current
    val repository = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()
    val alarms by repository.alarms.collectAsState(initial = emptyList())

    var showPicker by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmModel?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "ALARM · NUMIO",
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            color = AppColor.accent.value,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Active alarms",
            fontSize = 11.sp,
            color = NumioTextMuted,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(alarms) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggle = { id ->
                        scope.launch { repository.toggleAlarm(id) }
                    },
                    onDelete = { id ->
                        scope.launch { repository.deleteAlarm(id) }
                    },
                    onEdit = { updatedAlarm ->
                        editingAlarm = updatedAlarm
                        showPicker = true
                    },
                    onDayToggle = { updatedAlarm ->
                        scope.launch { repository.addAlarm(updatedAlarm) }
                    }
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            editingAlarm = null
                            showPicker = true
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1A1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 18.sp, color = NumioTextMuted)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "New alarm",
                            fontSize = 13.sp,
                            color = NumioTextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }

    // Numio time picker dialog
    if (showPicker) {
        val calendar = Calendar.getInstance()
        val alarmToEdit = editingAlarm // capture stable reference
        NumioTimePicker(
            initialHour = alarmToEdit?.hour ?: calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = alarmToEdit?.minute ?: calendar.get(Calendar.MINUTE),
            onConfirm = { hour, minute ->
                scope.launch {
                    if (alarmToEdit != null) {
                        repository.addAlarm(alarmToEdit.copy(hour = hour, minute = minute))
                    } else {
                        val newAlarm = AlarmModel(
                            id = System.currentTimeMillis().toInt(),
                            hour = hour,
                            minute = minute,
                            label = "Alarm",
                            days = listOf(false, true, true, true, true, true, false),
                            isEnabled = true
                        )
                        repository.addAlarm(newAlarm)
                    }
                }
            },
            onDismiss = {
                showPicker = false
                editingAlarm = null
            }
        )
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmModel,
    onToggle: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onEdit: (AlarmModel) -> Unit,
    onDayToggle: (AlarmModel) -> Unit
) {
    val isActive = alarm.isEnabled
    val h = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
    val m = alarm.minute.toString().padStart(2, '0')
    val ap = if (alarm.hour < 12) "AM" else "PM"
    val days = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) Color(0xFF1A1610) else Color(0xFF161616))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.clickable { onEdit(alarm) }
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$h:$m",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.W300,
                        color = if (isActive) AppColor.accent.value else NumioTextSecondary,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ap,
                        fontSize = 16.sp,
                        color = if (isActive) Color(0x80F5C427) else NumioTextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = alarm.label,
                    fontSize = 12.sp,
                    color = NumioTextMuted,
                    letterSpacing = 0.5.sp
                )
                if (isActive) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = getTimeUntilAlarm(alarm.hour, alarm.minute),
                        fontSize = 11.sp,
                        color = AppColor.accent.value.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                NumioToggle(isOn = isActive, onToggle = { onToggle(alarm.id) })
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "delete",
                    fontSize = 10.sp,
                    color = Color(0xFF3a3a3a),
                    modifier = Modifier.clickable { onDelete(alarm.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEachIndexed { index, day ->
                val dayOn = alarm.days[index]
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (dayOn) Color(0x20F5C427) else Color(0xFF1F1F1F))
                        .clickable {
                            val newDays = alarm.days.toMutableList()
                            newDays[index] = !newDays[index]
                            onDayToggle(alarm.copy(days = newDays))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        fontSize = 10.sp,
                        color = if (dayOn) AppColor.accent.value else Color(0xFF444444),
                        fontWeight = if (dayOn) FontWeight.W500 else FontWeight.W400
                    )
                }
            }
        }
    }
}

@Composable
fun NumioToggle(isOn: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (isOn) AppColor.accent.value else Color(0xFF2A2A2A))
            .clickable { onToggle() }
            .padding(3.dp),
        contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isOn) Color(0xFF0E0E0E) else Color(0xFF888888))
        )
    }
}

fun getTimeUntilAlarm(hour: Int, minute: Int): String {
    val now = Calendar.getInstance()
    val alarm = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }
    if (alarm.timeInMillis <= now.timeInMillis) {
        alarm.add(Calendar.DAY_OF_YEAR, 1)
    }
    val diffMs = alarm.timeInMillis - now.timeInMillis
    val diffMins = (diffMs / 60000).toInt()
    val hours = diffMins / 60
    val mins = diffMins % 60
    return when {
        hours == 0 -> "Rings in $mins min"
        mins == 0  -> "Rings in $hours hr"
        else       -> "Rings in ${hours}h ${mins}m"
    }
}