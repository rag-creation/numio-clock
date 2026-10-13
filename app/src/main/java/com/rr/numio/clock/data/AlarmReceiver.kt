package com.rr.numio.clock.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private var ringtone: android.media.Ringtone? = null
        fun setRingtone(r: android.media.Ringtone?) { ringtone = r }
        fun stopAlarm() { ringtone?.stop(); ringtone = null }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.rr.numio.clock.ALARM_TRIGGER" -> {
                val alarmId    = intent.getIntExtra("alarm_id", 0)
                val alarmLabel = intent.getStringExtra("alarm_label") ?: "Alarm"
                val alarmHour  = intent.getIntExtra("alarm_hour", 7)
                val alarmMin   = intent.getIntExtra("alarm_minute", 0)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val style = WorldCityStore.getAlarmStyle(context).firstOrNull() ?: "hold"
                        val snoozeMins = WorldCityStore.getSnoozeDuration(context).firstOrNull() ?: 10

                        val serviceIntent = Intent(context, AlarmService::class.java).apply {
                            putExtra("alarm_id", alarmId)
                            putExtra("alarm_label", alarmLabel)
                            putExtra("alarm_hour", alarmHour)
                            putExtra("alarm_minute", alarmMin)
                            putExtra("alarm_style", style)
                            putExtra("snooze_minutes", snoozeMins)
                        }
                        context.startForegroundService(serviceIntent)
                    } finally {
                        // Book the next ring (repeating) or switch off (one-time).
                        // Without this a weekday alarm only ever rang once.
                        try {
                            AlarmRepository(context).onAlarmFired(alarmId)
                        } catch (e: Exception) {
                            android.util.Log.e("NumioAlarm", "Reschedule after ring failed", e)
                        }
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AlarmRepository(context).rescheduleAll()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}