package com.rr.numio.clock.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rr.numio.clock.MainActivity
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "NumioAlarm"
    private const val ACTION_TRIGGER = "com.rr.numio.clock.ALARM_TRIGGER"

    private val DAYS = listOf(
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
        Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    fun schedule(context: Context, alarm: AlarmModel) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        Log.d(TAG, "Scheduling alarm: ${alarm.label} at ${alarm.hour}:${alarm.minute}")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "NO PERMISSION to schedule exact alarms!")
                return
            }
        }

        val activeDays = alarm.days.mapIndexedNotNull { index, active ->
            if (active) DAYS[index] else null
        }

        if (activeDays.isEmpty()) {
            scheduleOnce(context, alarmManager, alarm, -1)
        } else {
            activeDays.forEach { day ->
                scheduleOnce(context, alarmManager, alarm, day)
            }
        }
    }

    private fun scheduleOnce(
        context: Context,
        alarmManager: AlarmManager,
        alarm: AlarmModel,
        dayOfWeek: Int
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (dayOfWeek != -1) set(Calendar.DAY_OF_WEEK, dayOfWeek)
            if (timeInMillis <= System.currentTimeMillis()) {
                if (dayOfWeek != -1) add(Calendar.WEEK_OF_YEAR, 1)
                else add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val requestCode = if (dayOfWeek == -1) alarm.id else alarm.id * 10 + dayOfWeek
        val pendingIntent = triggerIntent(
            context, requestCode, alarm,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )!!

        setAlarmClock(context, alarmManager, calendar.timeInMillis, pendingIntent)
        Log.d(TAG, "✅ Alarm set for: ${calendar.time}")
    }

    fun snooze(context: Context, alarm: AlarmModel, snoozeMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L

        Log.d(TAG, "Snoozing for $snoozeMinutes min")

        val pendingIntent = triggerIntent(
            context, alarm.id + 9999, alarm,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )!!

        setAlarmClock(context, alarmManager, triggerAt, pendingIntent)
        Log.d(TAG, "✅ Snooze set for $snoozeMinutes minutes")
    }

    // Cancels any pending snooze for this alarm — call this on dismiss
    fun cancelSnooze(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelRequest(context, alarmManager, alarmId + 9999)
        Log.d(TAG, "✅ Snooze cancelled for alarm $alarmId")
    }

    fun cancel(context: Context, alarm: AlarmModel) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        DAYS.forEach { day -> cancelRequest(context, alarmManager, alarm.id * 10 + day) }
        cancelRequest(context, alarmManager, alarm.id)
        cancelSnooze(context, alarm.id)
        Log.d(TAG, "✅ Alarm ${alarm.id} cancelled")
    }

    // ---------- helpers ----------

    /**
     * setAlarmClock = the "real alarm" API.
     * - Android shows the alarm-clock icon in the status bar while any of these is pending
     * - Icon disappears automatically when all are cancelled/fired
     * - Fires exactly, even in Doze
     */
    private fun setAlarmClock(
        context: Context,
        alarmManager: AlarmManager,
        triggerAt: Long,
        operation: PendingIntent
    ) {
        // Where to go when the user taps the alarm info in the quick settings panel
        val showIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                operation
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ setAlarmClock failed: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    private fun triggerIntent(
        context: Context,
        requestCode: Int,
        alarm: AlarmModel?,
        flags: Int
    ): PendingIntent? {
        // Action MUST be the same when scheduling and cancelling,
        // otherwise Android treats them as different intents and cancel does nothing
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER
            if (alarm != null) {
                putExtra("alarm_id", alarm.id)
                putExtra("alarm_label", alarm.label)
                putExtra("alarm_hour", alarm.hour)
                putExtra("alarm_minute", alarm.minute)
            }
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun cancelRequest(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        triggerIntent(
            context, requestCode, null,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}