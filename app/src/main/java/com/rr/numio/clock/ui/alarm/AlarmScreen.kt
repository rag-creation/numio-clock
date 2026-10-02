package com.rr.numio.clock.ui.alarm

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rr.numio.clock.data.AlarmModel
import com.rr.numio.clock.data.AlarmRepository
import com.rr.numio.clock.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private const val PREFS = "numio_prefs"
private const val KEY_DELETE_HINT_SEEN = "delete_hint_seen"

@Composable
fun AlarmScreen() {
    val context = LocalContext.current
    val repository = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()
    val alarms by repository.alarms.collectAsState(initial = emptyList())

    var showPicker by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmModel?>(null) }

    // Delete flow: long-press → confirm → delete → undo bar
    var alarmToDelete by remember { mutableStateOf<AlarmModel?>(null) }
    var recentlyDeleted by remember { mutableStateOf<AlarmModel?>(null) }

    // One-time hint "Hold an alarm to delete it"
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var hintSeen by remember { mutableStateOf(prefs.getBoolean(KEY_DELETE_HINT_SEEN, false)) }

    // Hide the undo bar after 4 seconds
    LaunchedEffect(recentlyDeleted) {
        if (recentlyDeleted != null) {
            delay(4000)
            recentlyDeleted = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { id ->
                            scope.launch { repository.toggleAlarm(id) }
                        },
                        onLongPress = { alarmToDelete = it },
                        onEdit = { updatedAlarm ->
                            editingAlarm = updatedAlarm
                            showPicker = true
                        },
                        onDayToggle = { updatedAlarm ->
                            scope.launch { repository.addAlarm(updatedAlarm) }
                        }
                    )
                }

                // Hint — shown until the user has deleted an alarm once
                if (alarms.isNotEmpty() && !hintSeen) {
                    item {
                        Text(
                            text = "Hold an alarm to delete it",
                            fontSize = 11.sp,
                            color = Color(0xFF444444),
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
                    }
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

                // Room so the undo bar never covers the last card
                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }

        // Undo bar
        AnimatedVisibility(
            visible = recentlyDeleted != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1F1F1F))
                    .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Alarm deleted", fontSize = 13.sp, color = NumioTextSecondary)
                Text(
                    text = "UNDO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W600,
                    letterSpacing = 1.sp,
                    color = AppColor.accent.value,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            recentlyDeleted?.let { restored ->
                                scope.launch { repository.addAlarm(restored) }
                            }
                            recentlyDeleted = null
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }

    // Confirm delete
    alarmToDelete?.let { alarm ->
        DeleteAlarmDialog(
            alarm = alarm,
            onCancel = { alarmToDelete = null },
            onConfirm = {
                scope.launch { repository.deleteAlarm(alarm.id) }
                recentlyDeleted = alarm
                alarmToDelete = null
                if (!hintSeen) {
                    hintSeen = true
                    prefs.edit().putBoolean(KEY_DELETE_HINT_SEEN, true).apply()
                }
            }
        )
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
private fun DeleteAlarmDialog(
    alarm: AlarmModel,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val h = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
    val m = alarm.minute.toString().padStart(2, '0')
    val ap = if (alarm.hour < 12) "AM" else "PM"

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161616))
                .padding(24.dp)
        ) {
            Text(
                text = "Delete alarm?",
                fontSize = 20.sp,
                fontWeight = FontWeight.W400,
                color = NumioTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The $h:$m $ap alarm will be removed.",
                fontSize = 14.sp,
                color = NumioTextMuted
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F1F1F))
                        .clickable { onCancel() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", fontSize = 14.sp, color = NumioTextSecondary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x25E04040))
                        .clickable { onConfirm() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Delete",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        color = NumioRed
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmCard(
    alarm: AlarmModel,
    onToggle: (Int) -> Unit,
    onLongPress: (AlarmModel) -> Unit,
    onEdit: (AlarmModel) -> Unit,
    onDayToggle: (AlarmModel) -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
            // Tap = edit time, hold = delete
            .combinedClickable(
                onClick = { onEdit(alarm) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress(alarm)
                }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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
                        color = if (isActive) AppColor.accent.value.copy(alpha = 0.5f) else NumioTextMuted,
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
                        text = getTimeUntilAlarm(alarm.hour, alarm.minute, alarm.days),
                        fontSize = 11.sp,
                        color = AppColor.accent.value.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            NumioToggle(isOn = isActive, onToggle = { onToggle(alarm.id) })
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

/**
 * Time until the next ring, respecting the selected days.
 * days[0] = Sunday … days[6] = Saturday (same order as the S M T W T F S row).
 * No days selected = one-time alarm (today, or tomorrow if the time has passed).
 */
fun getTimeUntilAlarm(hour: Int, minute: Int, days: List<Boolean> = emptyList()): String {
    val now = Calendar.getInstance()
    val anyDay = days.any { it }

    var next: Calendar? = null
    for (offset in 0..7) {
        val candidate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, offset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.timeInMillis <= now.timeInMillis) continue
        val dayIndex = candidate.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0
        if (!anyDay || days.getOrElse(dayIndex) { false }) {
            next = candidate
            break
        }
    }
    if (next == null) return ""

    val diffMins = ((next.timeInMillis - now.timeInMillis) / 60000).toInt()
    val d = diffMins / (60 * 24)
    val hrs = (diffMins / 60) % 24
    val mins = diffMins % 60
    return when {
        d > 0 && hrs == 0 -> "Rings in $d ${if (d == 1) "day" else "days"}"
        d > 0             -> "Rings in ${d}d ${hrs}h"
        hrs == 0          -> if (mins == 0) "Rings in under a minute" else "Rings in $mins min"
        mins == 0         -> "Rings in $hrs hr"
        else              -> "Rings in ${hrs}h ${mins}m"
    }
}