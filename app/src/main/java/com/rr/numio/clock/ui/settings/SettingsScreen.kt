package com.rr.numio.clock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import com.rr.numio.clock.data.WorldCityStore
import com.rr.numio.clock.ui.setup.PermissionList
import com.rr.numio.clock.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.BreakIterator

@Composable
fun SettingsScreen() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedAlarmStyle by remember { mutableStateOf(1) }
    var selectedSnooze by remember { mutableStateOf(1) }

    // Load saved snooze duration
    val savedSnooze by WorldCityStore.getSnoozeDuration(context)
        .collectAsState(initial = 10)
    LaunchedEffect(savedSnooze) {
        selectedSnooze = when (savedSnooze) {
            5 -> 0
            10 -> 1
            15 -> 2
            20 -> 3
            else -> 1
        }
    }
    // Easter egg: tap "Version 1.0.0" 7 times
    var easterEggCount by remember { mutableStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    var lastToast by remember { mutableStateOf<Toast?>(null) }
    var hexInput by remember { mutableStateOf("") }
    var customColor by remember { mutableStateOf(AppColor.accent.value) }

    // Load saved alarm style
    val savedStyle by WorldCityStore.getAlarmStyle(context)
        .collectAsState(initial = "hold")
    LaunchedEffect(savedStyle) {
        selectedAlarmStyle = if (savedStyle == "chase") 1 else 0
    }

    val alarmStyles = listOf("Hold to Unlock", "Chase the Moon")
    val snoozeDurations = listOf("5 min", "10 min", "15 min", "20 min")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SETTINGS · NUMIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                color = AppColor.accent.value,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Appearance
        item {
            SettingsSectionTitle("Appearance")
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                Text("Accent color", fontSize = 14.sp, color = NumioTextPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Preview dot
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                runCatching {
                                    Color(
                                        android.graphics.Color.parseColor(
                                            if (hexInput.startsWith("#")) hexInput
                                            else "#$hexInput"
                                        )
                                    )
                                }.getOrElse { AppColor.accent.value }
                            )
                    )

                    BasicTextField(
                        value = hexInput,
                        onValueChange = { value ->
                            if (value.length <= 7) hexInput = value.uppercase()
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = NumioTextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1F1F1F))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                if (hexInput.isEmpty()) {
                                    Text(
                                        "#F5C427",
                                        fontSize = 14.sp,
                                        color = NumioTextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Apply color button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x20F5C427))
                        .clickable {
                            runCatching {
                                val parsed = Color(
                                    android.graphics.Color.parseColor(
                                        if (hexInput.startsWith("#")) hexInput
                                        else "#$hexInput"
                                    )
                                )
                                customColor = parsed
                                AppColor.update(parsed)
                                // Save to DataStore so it persists after restart
                                scope.launch {
                                    WorldCityStore.saveAccentColor(
                                        context,
                                        parsed.toArgb().toLong()
                                    )
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Apply color",
                        fontSize = 13.sp,
                        color = AppColor.accent.value
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reset to default
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1F1F1F))
                        .clickable {
                            hexInput = ""
                            AppColor.update(NumioAmber)
                            scope.launch {
                                WorldCityStore.saveAccentColor(
                                    context,
                                    NumioAmber.toArgb().toLong()
                                )
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Reset to default",
                        fontSize = 13.sp,
                        color = NumioTextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Alarm style
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionTitle("Alarm style")
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                Text(
                    "Choose how you snooze",
                    fontSize = 14.sp,
                    color = NumioTextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                alarmStyles.forEachIndexed { index, style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedAlarmStyle == index)
                                    Color(0x20F5C427) else Color.Transparent
                            )
                            .clickable {
                                selectedAlarmStyle = index
                                scope.launch {
                                    WorldCityStore.saveAlarmStyle(
                                        context,
                                        if (index == 0) "hold" else "chase"
                                    )
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                style,
                                fontSize = 13.sp,
                                color = if (selectedAlarmStyle == index)
                                    AppColor.accent.value else NumioTextSecondary
                            )
                            // Show HARD badge for Chase the Moon
                            if (index == 1) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x30E04040))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "HARD",
                                        fontSize = 9.sp,
                                        color = Color(0xFFE04040),
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.W600
                                    )
                                }
                            }
                        }
                        if (selectedAlarmStyle == index) {
                            Text("✓", fontSize = 13.sp, color = AppColor.accent.value)
                        }
                    }
                    if (index < alarmStyles.size - 1) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Snooze duration
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionTitle("Snooze duration")
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    snoozeDurations.forEachIndexed { index, dur ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selectedSnooze == index)
                                        Color(0x20F5C427) else Color(0xFF1F1F1F)
                                )
                                .clickable {
                                    selectedSnooze = index
                                    scope.launch {
                                        val minutes = when (index) {
                                            0 -> 5
                                            1 -> 10
                                            2 -> 15
                                            3 -> 20
                                            else -> 10
                                        }
                                        WorldCityStore.saveSnoozeDuration(context, minutes)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dur,
                                fontSize = 11.sp,
                                color = if (selectedSnooze == index)
                                    AppColor.accent.value else NumioTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Permissions
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionTitle("Permissions")
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard {
                PermissionList()
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // About
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionTitle("About")
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                SettingsRow(
                    label = "Version",
                    value = "1.1.1",
                    onSecretTap = {
                        easterEggCount++
                        val left = 7 - easterEggCount
                        lastToast?.cancel()
                        when {
                            left <= 0 -> {
                                easterEggCount = 0
                                lastToast = null
                                showEasterEgg = true
                            }
                            left <= 3 -> {
                                lastToast = Toast.makeText(
                                    context,
                                    if (left == 1) "1 more tap…" else "$left more taps…",
                                    Toast.LENGTH_SHORT
                                ).also { it.show() }
                            }
                        }
                    }
                )
                SettingsDivider()
                SettingsRow(
                    label = "Source code",
                    value = "GitHub →",
                    onClick = {
                        uriHandler.openUri("https://github.com/rag-creation/numio-clock")
                    }
                )
                SettingsDivider()
                SettingsRow(
                    label = "Report a bug",
                    value = "Issues →",
                    onClick = {
                        uriHandler.openUri("https://github.com/rag-creation/numio-clock/issues")
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Footer
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Thanks for choosing Numio Clock 💛",
                    fontSize = 13.sp,
                    color = NumioTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "With Lo❤️e, R.R",
                    fontSize = 12.sp,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showEasterEgg) {
        EasterEggOverlay(onClose = { showEasterEgg = false })
    }
}

@Composable
fun EasterEggOverlay(onClose: () -> Unit) {
    val message = "Made from കേരളം 💛"
    val signature = "With Lo❤️e, R.R"

    // Split into real characters so emoji never show up half-drawn
    val boundaries = remember(message) {
        val iter = BreakIterator.getCharacterInstance()
        iter.setText(message)
        buildList {
            var end = iter.next()
            while (end != BreakIterator.DONE) {
                add(end)
                end = iter.next()
            }
        }
    }

    var shown by remember { mutableStateOf(0) }
    var showSignature by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        for (i in boundaries.indices) {
            shown = boundaries[i]
            delay(if (message[boundaries[i] - 1] == '\n') 300 else 60)
        }
        delay(600)
        showSignature = true
    }

    val signatureAlpha by animateFloatAsState(
        targetValue = if (showSignature) 1f else 0f,
        animationSpec = tween(900),
        label = "signature"
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF20A0A0A))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    text = message.substring(0, shown),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W300,
                    color = AppColor.accent.value,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = signature,
                    fontSize = 15.sp,
                    color = NumioTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(signatureAlpha)
                )
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "tap anywhere to close",
                    fontSize = 11.sp,
                    color = Color(0xFF333333),
                    letterSpacing = 1.sp,
                    modifier = Modifier.alpha(signatureAlpha)
                )
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 10.sp,
        color = NumioTextMuted,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.W500
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161616))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SettingsRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    // Tappable, but looks like a normal plain row (no accent color, no ripple)
    onSecretTap: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    onClick != null -> Modifier.clickable { onClick() }
                    onSecretTap != null -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSecretTap() }
                    else -> Modifier
                }
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = NumioTextSecondary)
        Text(
            value,
            fontSize = 13.sp,
            color = if (onClick != null) AppColor.accent.value else NumioTextMuted
        )
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF1E1E1E))
    )
}