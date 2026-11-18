package com.rr.numio.clock.ui.alarm

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.core.content.IntentCompat
import com.rr.numio.clock.R
import com.rr.numio.clock.data.AlarmTimes
import java.text.SimpleDateFormat
import java.util.Locale
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

    // Choosing a sound: which alarm, and the two pickers (phone's alarm tones, a music file)
    var soundAlarmId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSoundDialog by rememberSaveable { mutableStateOf(false) }
    fun saveSound(sound: String, name: String) {
        val alarm = alarms.find { it.id == soundAlarmId } ?: return
        scope.launch { repository.addAlarm(alarm.copy(sound = sound, soundName = name)) }
    }
    val tonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uri = IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        if (uri == null || RingtoneManager.isDefault(uri)) {
            saveSound("", "")
        } else {
            val name = try { RingtoneManager.getRingtone(context, uri)?.getTitle(context) } catch (e: Exception) { null }
            saveSound(uri.toString(), name ?: "Alarm tone")
        }
    }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Keep the right to read this file after a restart, or the alarm couldn't play it
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) { }
        saveSound(uri.toString(), musicName(context, uri))
    }
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
                        },
                        onSound = {
                            soundAlarmId = alarm.id
                            showSoundDialog = true
                        },
                        onVibrate = {
                            scope.launch { repository.addAlarm(alarm.copy(vibrate = !alarm.vibrate)) }
                        },
                        onSkip = { skipping ->
                            scope.launch {
                                if (skipping) repository.cancelSkip(alarm.id) else repository.skipNext(alarm.id)
                            }
                        }
                    )
                }

                // Hint — shown until the user has deleted an alarm once
                if (alarms.isNotEmpty() && !hintSeen) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            NumioHint("Hold an alarm to delete it")
                        }
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

    // Alarm sound
    if (showSoundDialog) {
        val alarm = alarms.find { it.id == soundAlarmId }
        if (alarm == null) {
            showSoundDialog = false
        } else {
            SoundDialog(
                alarm = alarm,
                onDismiss = { showSoundDialog = false },
                onDefault = {
                    showSoundDialog = false
                    saveSound("", "")
                },
                onTones = {
                    showSoundDialog = false
                    tonePicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            if (alarm.sound.isBlank()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            else Uri.parse(alarm.sound)
                        )
                    })
                },
                onMusic = {
                    showSoundDialog = false
                    musicPicker.launch(arrayOf("audio/*"))
                }
            )
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
                        // A new time means a new next ring, so any skip is dropped
                        repository.addAlarm(alarmToEdit.copy(hour = hour, minute = minute, skipAt = 0L))
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
    onDayToggle: (AlarmModel) -> Unit,
    onSound: () -> Unit = {},
    onVibrate: () -> Unit = {},
    onSkip: (skipping: Boolean) -> Unit = {}
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
                        text = getTimeUntilAlarm(alarm),
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
                            onDayToggle(alarm.copy(days = newDays, skipAt = 0L))
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

        Spacer(modifier = Modifier.height(10.dp))

        // Sound · Vibrate · Skip next
        val skipping = AlarmTimes.isSkipping(alarm, System.currentTimeMillis())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OptionChip(
                icon = R.drawable.ic_bell,
                text = alarm.soundName.ifBlank { "Default" },
                on = alarm.sound.isNotBlank(),
                description = "Alarm sound: ${alarm.soundName.ifBlank { "phone's alarm sound" }}",
                onClick = onSound,
                modifier = Modifier.weight(1f, fill = false)
            )
            OptionChip(
                icon = R.drawable.ic_vibrate,
                text = if (alarm.vibrate) "Vibrate" else "No vibrate",
                on = alarm.vibrate,
                description = if (alarm.vibrate) "Vibrate is on" else "Vibrate is off",
                onClick = onVibrate
            )
            if (isActive && alarm.repeats) {
                OptionChip(
                    icon = R.drawable.ic_skip,
                    text = if (skipping) "Skipped ${skipDay(alarm.skipAt)} · Undo" else "Skip next",
                    on = skipping,
                    description = if (skipping) "Next ring skipped. Tap to undo" else "Skip the next ring only",
                    onClick = { onSkip(skipping) }
                )
            }
        }
    }
}

/** Small rounded button on the alarm card: icon + word, amber when on. */
@Composable
private fun OptionChip(
    icon: Int,
    text: String,
    on: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colour = if (on) AppColor.accent.value else NumioTextMuted
    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (on) Color(0x20F5C427) else Color(0xFF1F1F1F))
            .clickable(onClickLabel = description, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = colour,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = colour,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/** "Thu" for the skipped ring's day. */
private fun skipDay(millis: Long): String =
    SimpleDateFormat("EEE", Locale.getDefault()).format(java.util.Date(millis))

/** A music file's name without its extension, for the alarm card. */
private fun musicName(context: Context, uri: Uri): String {
    val name = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) { null }
    return name?.substringBeforeLast('.')?.ifBlank { null } ?: "Music"
}

@Composable
private fun SoundDialog(
    alarm: AlarmModel,
    onDismiss: () -> Unit,
    onDefault: () -> Unit,
    onTones: () -> Unit,
    onMusic: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161616))
                .padding(24.dp)
        ) {
            Text("Alarm sound", fontSize = 20.sp, fontWeight = FontWeight.W400, color = NumioTextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            SoundRow("Phone's alarm sound", "What the phone uses for alarms", alarm.sound.isBlank(), onDefault)
            SoundRow("Alarm tones", "Pick from the phone's tones", false, onTones)
            SoundRow("Music on this phone", "Any song or audio file", false, onMusic)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "If the chosen sound is ever deleted, the alarm rings with the phone's alarm sound instead.",
                fontSize = 12.sp,
                color = NumioTextMuted,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun SoundRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = if (selected) AppColor.accent.value else NumioTextPrimary)
            Text(subtitle, fontSize = 12.sp, color = NumioTextMuted)
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(AppColor.accent.value)
            )
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
 * Time until the next ring, respecting the selected days and a skipped ring.
 * Uses the same maths that books the alarm (AlarmTimes).
 */
fun getTimeUntilAlarm(alarm: AlarmModel): String {
    val now = System.currentTimeMillis()
    val next = AlarmTimes.next(alarm, now)
    val diffMins = ((next - now) / 60000).toInt()
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
