package com.rr.numio.clock.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rr.numio.clock.AlarmActivity

/**
 * The Snooze and Dismiss buttons on the ringing alarm's notification.
 * They work even when the alarm screen can't be used.
 */
class AlarmActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE = "com.rr.numio.clock.ALARM_SNOOZE"
        const val ACTION_DISMISS = "com.rr.numio.clock.ALARM_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", 0)
        when (intent.action) {
            ACTION_SNOOZE -> AlarmScheduler.snooze(
                context,
                AlarmModel(
                    id = alarmId,
                    hour = intent.getIntExtra("alarm_hour", 7),
                    minute = intent.getIntExtra("alarm_minute", 0),
                    label = intent.getStringExtra("alarm_label") ?: "Alarm",
                    days = List(7) { false },
                    isEnabled = true
                ),
                intent.getIntExtra("snooze_minutes", 10)
            )
            ACTION_DISMISS -> AlarmScheduler.cancelSnooze(context, alarmId)
            else -> return
        }
        AlarmReceiver.stopAlarm()
        AlarmService.stop(context)
        AlarmActivity.closeIfOpen()
    }
}
