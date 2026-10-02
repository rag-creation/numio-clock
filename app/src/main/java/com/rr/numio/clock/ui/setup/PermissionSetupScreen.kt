package com.rr.numio.clock.ui.setup

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rr.numio.clock.ui.theme.*

// ─────────────────────────────────────────────────────────────
// Permission checks + where to send the user to grant each one
// ─────────────────────────────────────────────────────────────
object NumioPermissions {

    fun notifications(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        else
            ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    fun exactAlarms(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        else true

    fun fullScreen(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        else true

    fun battery(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(ctx.packageName)

    /** The ones an alarm can't work without. Battery is only recommended. */
    fun allRequired(ctx: Context): Boolean =
        notifications(ctx) && exactAlarms(ctx) && fullScreen(ctx)

    private fun pkgUri(ctx: Context) = Uri.parse("package:${ctx.packageName}")

    fun openNotificationSettings(ctx: Context) = open(
        ctx,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
    )

    fun openExactAlarmSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            open(ctx, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri(ctx)))
        }
    }

    fun openFullScreenSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            open(ctx, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri(ctx)))
        }
    }

    @SuppressLint("BatteryLife")
    fun openBatterySettings(ctx: Context) =
        open(ctx, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri(ctx)))

    // Some phones don't support a specific settings page — fall back to the app info page
    private fun open(ctx: Context, intent: Intent) {
        runCatching { ctx.startActivity(intent) }.onFailure {
            runCatching {
                ctx.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(ctx))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Re-check permissions whenever the user comes back from Settings
// ─────────────────────────────────────────────────────────────
private fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
private fun rememberResumeTick(): Int {
    val activity = LocalContext.current.findActivity() as? ComponentActivity
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    return tick
}

private data class PermItem(
    val title: String,
    val description: String,
    val required: Boolean,
    val granted: Boolean,
    val onAllow: () -> Unit
)

@Composable
private fun rememberPermItems(): List<PermItem> {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    var notifAsked by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifAsked = true }

    // `tick` changes on every resume, so this list is rebuilt with fresh statuses
    return remember(tick, notifAsked) {
        buildList {
            add(
                PermItem(
                    title = "Notifications",
                    description = "So the alarm can ring and show its controls.",
                    required = true,
                    granted = NumioPermissions.notifications(context),
                    onAllow = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifAsked) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // Already asked once (or older Android) — go to settings
                            NumioPermissions.openNotificationSettings(context)
                        }
                    }
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    PermItem(
                        title = "Alarms & reminders",
                        description = "So alarms ring at the exact minute you set.",
                        required = true,
                        granted = NumioPermissions.exactAlarms(context),
                        onAllow = { NumioPermissions.openExactAlarmSettings(context) }
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(
                    PermItem(
                        title = "Full screen alerts",
                        description = "So the alarm screen opens over your lock screen.",
                        required = true,
                        granted = NumioPermissions.fullScreen(context),
                        onAllow = { NumioPermissions.openFullScreenSettings(context) }
                    )
                )
            }
            add(
                PermItem(
                    title = "Battery optimisation",
                    description = "Stops your phone from putting the app to sleep. Recommended on Samsung.",
                    required = false,
                    granted = NumioPermissions.battery(context),
                    onAllow = { NumioPermissions.openBatterySettings(context) }
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// First-launch setup screen
// ─────────────────────────────────────────────────────────────
@Composable
fun PermissionSetupScreen(onDone: () -> Unit) {
    val items = rememberPermItems()
    val requiredDone = items.filter { it.required }.all { it.granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NumioDark)
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "SETUP · NUMIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                color = AppColor.accent.value,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Before your\nfirst alarm",
                fontSize = 34.sp,
                fontWeight = FontWeight.W300,
                color = NumioTextPrimary,
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Clock by Numio needs a few permissions so your alarms ring on time, even when the phone is locked. Nothing ever leaves your device.",
                fontSize = 14.sp,
                color = NumioTextMuted,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(28.dp))

            items.forEachIndexed { index, item ->
                PermissionRow(number = index + 1, item = item, compact = false)
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom button
        if (!requiredDone) {
            Text(
                text = "Alarms may not ring properly until the required ones are allowed.",
                fontSize = 11.sp,
                color = NumioTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (requiredDone) Modifier.background(AppColor.accent.value)
                    else Modifier.border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
                )
                .clickable { onDone() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (requiredDone) "Continue" else "Skip for now",
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
                color = if (requiredDone) NumioDark else NumioTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// Compact list for the Settings screen
// ─────────────────────────────────────────────────────────────
@Composable
fun PermissionList() {
    val items = rememberPermItems()
    Column {
        items.forEachIndexed { index, item ->
            PermissionRow(number = index + 1, item = item, compact = true)
            if (index < items.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF1E1E1E))
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(number: Int, item: PermItem, compact: Boolean) {
    val accent = AppColor.accent.value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (compact) Modifier.padding(vertical = 12.dp)
                else Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF161616))
                    .padding(16.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step number, or a tick once granted
        if (!compact) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (item.granted) accent.copy(alpha = 0.15f) else Color(0xFF1F1F1F)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (item.granted) "✓" else "$number",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W500,
                    color = if (item.granted) accent else NumioTextMuted
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    color = if (compact) NumioTextSecondary else NumioTextPrimary
                )
                if (!item.required) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECOMMENDED",
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.W600,
                        color = NumioTextMuted
                    )
                }
            }
            if (!compact) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = NumioTextMuted,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (item.granted) {
            Text(
                text = "Allowed",
                fontSize = 12.sp,
                color = accent
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent)
                    .clickable { item.onAllow() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Allow",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W600,
                    color = NumioDark
                )
            }
        }
    }
}