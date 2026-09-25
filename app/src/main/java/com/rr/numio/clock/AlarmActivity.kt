package com.rr.numio.clock

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.rr.numio.clock.data.AlarmReceiver
import com.rr.numio.clock.data.AlarmService
import com.rr.numio.clock.ui.alarm.AlarmFiringScreen
import com.rr.numio.clock.ui.alarm.ChasetheMoonScreen
import com.rr.numio.clock.ui.theme.ClockByNumioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // FIX #3 — Dismiss keyguard so user can interact with alarm screen
            // without unlocking the phone first, but prevents editing other apps
            setInheritShowWhenLocked(false)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

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
                        onSnooze = {
                            stopEverything()
                            lifecycleScope.launch {
                                delay(2000)
                                finish()
                            }
                        },
                        onDismiss = {
                            stopEverything()
                            lifecycleScope.launch {
                                delay(2000)
                                finish()
                            }
                        }
                    )
                } else {
                    AlarmFiringScreen(
                        hour = hour,
                        minute = minute,
                        label = label,
                        alarmId = alarmId,
                        snoozeMinutes = snoozeMins,
                        onSnooze = {
                            stopEverything()
                            lifecycleScope.launch {
                                delay(2000)
                                finish()
                            }
                        },
                        onDismiss = {
                            stopEverything()
                            lifecycleScope.launch {
                                delay(2000)
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun stopEverything() {
        AlarmReceiver.stopAlarm()
        AlarmService.stop(this)
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancelAll()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}