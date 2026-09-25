package com.rr.numio.clock.ui.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rr.numio.clock.ui.theme.*

@Composable
fun NumioTimePicker(
    initialHour: Int = 7,
    initialMinute: Int = 0,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember {
        mutableStateOf(initialHour % 12).also { if (it.value == 0) it.value = 12 }
    }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var isAm by remember { mutableStateOf(initialHour < 12) }

    var typingHour by remember { mutableStateOf(false) }
    var typingMinute by remember { mutableStateOf(false) }
    var hourInput by remember { mutableStateOf("") }
    var minuteInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161616))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SET ALARM",
                fontSize = 11.sp,
                color = NumioTextMuted,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.W500
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hour picker
                TimeColumn(
                    value = selectedHour,
                    range = 1..12,
                    isTyping = typingHour,
                    typedInput = hourInput,
                    isScrollingDisabled = typingHour,
                    onTap = {
                        typingHour = true
                        typingMinute = false
                        hourInput = ""
                    },
                    onTyped = { input ->
                        hourInput = input
                        val num = input.toIntOrNull()
                        if (num != null && num in 1..12) {
                            selectedHour = num
                        }
                        if (input.length >= 2) {
                            val n = input.toIntOrNull()
                            if (n != null && n in 1..12) selectedHour = n
                            typingHour = false
                            typingMinute = true
                            minuteInput = ""
                        }
                    },
                    onScroll = { newVal ->
                        if (!typingHour) selectedHour = newVal
                    },
                    accentColor = AppColor.accent.value
                )

                Text(
                    text = ":",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.W100,
                    color = NumioTextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Minute picker
                TimeColumn(
                    value = selectedMinute,
                    range = 0..59,
                    isTyping = typingMinute,
                    typedInput = minuteInput,
                    padStart = true,
                    isScrollingDisabled = typingMinute,
                    onTap = {
                        typingMinute = true
                        typingHour = false
                        minuteInput = ""
                    },
                    onTyped = { input ->
                        minuteInput = input
                        val num = input.toIntOrNull()
                        if (num != null && num in 0..59) {
                            selectedMinute = num
                        }
                        if (input.length >= 2) {
                            val n = input.toIntOrNull()
                            if (n != null && n in 0..59) selectedMinute = n
                            typingMinute = false
                        }
                    },
                    onScroll = { newVal ->
                        if (!typingMinute) selectedMinute = newVal
                    },
                    accentColor = AppColor.accent.value
                )

                Spacer(modifier = Modifier.width(16.dp))

                // AM/PM toggle
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("AM", "PM").forEach { period ->
                        val selected = (period == "AM") == isAm
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) AppColor.accent.value.copy(alpha = 0.15f)
                                    else Color(0xFF1F1F1F)
                                )
                                .clickable { isAm = (period == "AM") }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W500,
                                color = if (selected) AppColor.accent.value else NumioTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap hour or minute to type",
                fontSize = 11.sp,
                color = NumioTextMuted.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F1F1F))
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", fontSize = 14.sp, color = NumioTextMuted)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColor.accent.value.copy(alpha = 0.15f))
                        .clickable {
                            // Finalise any pending typed input
                            val finalHour = if (typingHour && hourInput.isNotEmpty())
                                hourInput.toIntOrNull()?.coerceIn(1, 12) ?: selectedHour
                            else selectedHour

                            val finalMinute = if (typingMinute && minuteInput.isNotEmpty())
                                minuteInput.toIntOrNull()?.coerceIn(0, 59) ?: selectedMinute
                            else selectedMinute

                            val hour24 = when {
                                isAm && finalHour == 12 -> 0
                                !isAm && finalHour != 12 -> finalHour + 12
                                else -> finalHour
                            }
                            onConfirm(hour24, finalMinute)
                            onDismiss()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Set alarm", fontSize = 14.sp, color = AppColor.accent.value)
                }
            }
        }
    }
}

@Composable
fun TimeColumn(
    value: Int,
    range: IntRange,
    isTyping: Boolean,
    typedInput: String,
    padStart: Boolean = false,
    isScrollingDisabled: Boolean = false,
    onTap: () -> Unit,
    onTyped: (String) -> Unit,
    onScroll: (Int) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color
) {
    val itemHeight = 56.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (value - range.first)
            .coerceIn(0, range.last - range.first)
    )
    val snapBehavior = rememberSnapFlingBehavior(listState)

    // Sync scroll when value changes AND we're not typing
    LaunchedEffect(value) {
        if (!isScrollingDisabled) {
            val idx = (value - range.first).coerceIn(0, range.last - range.first)
            if (!listState.isScrollInProgress) {
                listState.animateScrollToItem(idx)
            }
        }
    }

    // Report scroll position changes — only when not typing
    LaunchedEffect(listState.firstVisibleItemIndex, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && !isScrollingDisabled) {
            val newVal = range.first + listState.firstVisibleItemIndex
            if (newVal != value) onScroll(newVal)
        }
    }

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(itemHeight * 3)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        if (isTyping) {
            BasicTextField(
                value = typedInput,
                onValueChange = { if (it.length <= 2) onTyped(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.W200,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Default
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (typedInput.isEmpty()) {
                            Text(
                                text = if (padStart) value.toString().padStart(2, '0')
                                else value.toString(),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.W200,
                                color = accentColor.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
                            )
                        }
                        inner()
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(accentColor.copy(alpha = 0.08f))
            )

            LazyColumn(
                state = listState,
                flingBehavior = snapBehavior,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { Spacer(modifier = Modifier.height(itemHeight)) }

                items(range.last - range.first + 1) { i ->
                    val itemVal = range.first + i
                    val isSelected = itemVal == value
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (padStart) itemVal.toString().padStart(2, '0')
                            else itemVal.toString(),
                            fontSize = if (isSelected) 40.sp else 28.sp,
                            fontWeight = if (isSelected) FontWeight.W200 else FontWeight.W300,
                            color = if (isSelected) accentColor
                            else NumioTextMuted.copy(alpha = 0.4f)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(itemHeight)) }
            }
        }
    }
}