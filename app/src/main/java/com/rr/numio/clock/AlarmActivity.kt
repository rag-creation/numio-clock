package com.rr.numio.clock

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rr.numio.clock.data.AlarmReceiver
import com.rr.numio.clock.data.AlarmScheduler
import com.rr.numio.clock.data.AlarmService
import com.rr.numio.clock.ui.alarm.AlarmFiringScreen
import com.rr.numio.clock.ui.alarm.ChasetheMoonScreen
import com.rr.numio.clock.ui.theme.ClockByNumioTheme

class AlarmActivity : ComponentActivity() {

    companion object {
        // Survives activity recreation. true = this alarm was already snoozed/dismissed.
        @Volatile var handled = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recreated after the user already dismissed/snoozed → close, don't start a new countdown
        if (savedInstanceState != null && handled) {
            finish()
            return
        }
        // Fresh launch from AlarmService = new alarm ringing
        if (savedInstanceState == null) handled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        showAlarm()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A new alarm fired while this screen exists
        setIntent(intent)
        handled = false
        showAlarm()
    }

    private fun showAlarm() {
        val hour       = intent?.getIntExtra("alarm_hour", 7) ?: 7
        val minute     = intent?.getIntExtra("alarm_minute", 0) ?: 0
        val label      = intent?.getStringExtra("alarm_label") ?: "Alarm"
        val alarmId    = intent?.getIntExtra("alarm_id", 0) ?: 0
        val alarmStyle = intent?.getStringExtra("alarm_style") ?: "hold"
        val snoozeMins = intent?.getIntExtra("snooze_minutes", 10) ?: 10

        setContent {
            ClockByNumioTheme {
                if (alarmStyle == "chase") {
                    ChasetheMoonScreen(
                        hour = hour,
                        minute = minute,
                        label = label,
                        snoozeMinutes = snoozeMins,
                        onSnooze = { onSnoozed() },
                        onDismiss = { onDismissed(alarmId) }
                    )
                } else {
                    AlarmFiringScreen(
                        hour = hour,
                        minute = minute,
                        label = label,
                        alarmId = alarmId,
                        snoozeMinutes = snoozeMins,
                        onSnooze = { onSnoozed() },
                        onDismiss = { onDismissed(alarmId) }
                    )
                }
            }
        }
    }

    private fun onSnoozed() {
        handled = true
        stopEverything()
        closeSoon()
    }

    private fun onDismissed(alarmId: Int) {
        handled = true
        // Kill any snooze that got scheduled, no matter how
        AlarmScheduler.cancelSnooze(this, alarmId)
        stopEverything()
        closeSoon()
    }

    private fun stopEverything() {
        AlarmReceiver.stopAlarm()
        AlarmService.stop(this)
        getSystemService(NotificationManager::class.java).cancelAll()
    }

    // Main-thread Handler, NOT lifecycleScope — lifecycleScope gets cancelled
    // if the activity is recreated, and then finish() never runs.
    private fun closeSoon() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) finish()
        }, 1500)
    }
}