package com.rr.numio.clock.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "NumioAlarm"

    fun schedule(context: Context, alarm: AlarmModel) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        Log.d(TAG, "Scheduling alarm: ${alarm.label} at ${alarm.hour}:${alarm.minute}")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "NO PERMISSION to schedule exact alarms!")
                return
            }
        }

        val days = listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
        )

        val activeDays = alarm.days.mapIndexedNotNull { index, active ->
            if (active) days[index] else null
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

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.rr.numio.clock.ALARM_TRIGGER"
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_hour", alarm.hour)
            putExtra("alarm_minute", alarm.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "✅ Alarm set for: ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun snooze(context: Context, alarm: AlarmModel, snoozeMinutes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L

        Log.d(TAG, "Snoozing for $snoozeMinutes min")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.rr.numio.clock.ALARM_TRIGGER"
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_hour", alarm.hour)
            putExtra("alarm_minute", alarm.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id + 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
            Log.d(TAG, "✅ Snooze set for $snoozeMinutes minutes")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Snooze failed: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel(context: Context, alarm: AlarmModel) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val days = listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
        )
        days.forEach { day ->
            val requestCode = alarm.id * 10 + day
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }
}