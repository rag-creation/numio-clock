package com.rr.numio.clock.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.rr.numio.clock.MainActivity
import com.rr.numio.clock.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** Bedtime reminder settings. Off until the user turns it on. */
data class BedtimeSettings(
    val enabled: Boolean = false,
    /** How long the user wants to sleep, in minutes. */
    val goalMinutes: Int = 8 * 60,
    /** How long before bedtime the reminder comes: 0, 15 or 30 minutes. */
    val leadMinutes: Int = 30
)

/** The next morning ring and when to sleep for it. */
data class BedtimePlan(val ring: Long, val bedtime: Long, val remindAt: Long)

/**
 * Bedtime: a gentle reminder before the next morning alarm, worked out from
 * the alarms themselves (a skipped ring counts as no alarm). Nothing leaves the phone.
 */
object Bedtime {

    private val ENABLED_KEY = booleanPreferencesKey("bedtime_enabled")
    private val GOAL_KEY    = intPreferencesKey("bedtime_goal_minutes")
    private val LEAD_KEY    = intPreferencesKey("bedtime_lead_minutes")

    private const val CHANNEL_ID = "numio_bedtime"
    private const val ACTION = "com.rr.numio.clock.BEDTIME"
    const val ACTION_LATER = "com.rr.numio.clock.BEDTIME_LATER"
    const val ACTION_SKIP = "com.rr.numio.clock.BEDTIME_SKIP"
    private const val AMBER = 0xFFFFC21A.toInt()
    private const val NOTIFICATION_ID = 7700

    // ---------- settings ----------

    fun settings(context: Context): Flow<BedtimeSettings> = context.dataStore.data.map {
        BedtimeSettings(
            enabled = it[ENABLED_KEY] ?: false,
            goalMinutes = it[GOAL_KEY] ?: 8 * 60,
            leadMinutes = it[LEAD_KEY] ?: 30
        )
    }

    suspend fun save(context: Context, settings: BedtimeSettings) {
        context.dataStore.edit {
            it[ENABLED_KEY] = settings.enabled
            it[GOAL_KEY] = settings.goalMinutes
            it[LEAD_KEY] = settings.leadMinutes
        }
        update(context)
    }

    // ---------- the maths (plain Kotlin, unit tested) ----------

    /** A "morning" alarm rings between 3:00 AM and 12:59 PM. */
    fun isMorning(ring: Long): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = ring }.get(Calendar.HOUR_OF_DAY)
        return hour in 3..12
    }

    /** Each enabled alarm's next two rings (one for a one-time alarm). */
    private fun upcomingRings(alarms: List<AlarmModel>, now: Long): List<Long> =
        alarms.filter { it.isEnabled }.flatMap { alarm ->
            val first = AlarmTimes.next(alarm, now)
            if (alarm.repeats) listOf(first, AlarmTimes.next(alarm, first)) else listOf(first)
        }

    /** The next reminder still to come: for the earliest morning ring whose reminder is ahead. */
    fun nextReminder(alarms: List<AlarmModel>, settings: BedtimeSettings, now: Long): BedtimePlan? =
        upcomingRings(alarms, now)
            .filter { isMorning(it) }
            .map { ring ->
                val bedtime = ring - settings.goalMinutes * 60_000L
                BedtimePlan(ring, bedtime, bedtime - settings.leadMinutes * 60_000L)
            }
            .filter { it.remindAt > now }
            .minByOrNull { it.remindAt }

    /** Tonight's plan for the Alarm tab: the next morning ring within a day, if any. */
    fun tonight(alarms: List<AlarmModel>, settings: BedtimeSettings, now: Long): BedtimePlan? =
        upcomingRings(alarms, now)
            .filter { isMorning(it) && it - now <= 24 * 60 * 60_000L }
            .minOrNull()
            ?.let { ring ->
                val bedtime = ring - settings.goalMinutes * 60_000L
                BedtimePlan(ring, bedtime, bedtime - settings.leadMinutes * 60_000L)
            }

    // ---------- booking the reminder ----------

    /** Book (or cancel) the next reminder. Called whenever alarms or these settings change, and on boot. */
    suspend fun update(context: Context) {
        try {
            val settings = settings(context).first()
            val alarms = AlarmRepository(context).alarms.first()
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val pending = reminderIntent(context)
            alarmManager.cancel(pending)
            if (!settings.enabled) return
            val plan = nextReminder(alarms, settings, System.currentTimeMillis()) ?: return
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            if (exact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.remindAt, pending)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.remindAt, pending)
        } catch (e: Exception) {
            android.util.Log.e("NumioBedtime", "Couldn't book bedtime reminder", e)
        }
    }

    private fun reminderIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, NOTIFICATION_ID,
            Intent(context, BedtimeReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Show the reminder for the coming morning ring, if there still is one. */
    suspend fun remind(context: Context) {
        val settings = settings(context).first()
        if (!settings.enabled) return
        val now = System.currentTimeMillis()
        // The ring this reminder is for: the next morning ring that's still ahead
        val plan = tonight(AlarmRepository(context).alarms.first(), settings, now) ?: return
        if (plan.ring - now > settings.goalMinutes * 60_000L + settings.leadMinutes * 60_000L + 30 * 60_000L) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Bedtime reminder", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val time = DateFormat.getTimeInstance(DateFormat.SHORT)
        val hours = hoursText(settings.goalMinutes)
        val (title, text) = if (now >= plan.bedtime || settings.leadMinutes == 0) {
            "Time to sleep" to "Sleep now for $hours before your ${time.format(Date(plan.ring))} alarm."
        } else {
            "Time to wind down" to
                "Sleep by ${time.format(Date(plan.bedtime))} for $hours before your ${time.format(Date(plan.ring))} alarm."
        }
        val open = PendingIntent.getActivity(
            context, NOTIFICATION_ID, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val left = ((plan.ring - now) / 60_000L).toInt()
        val ringTime = time.format(Date(plan.ring))
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_moon)
            .setColor(AMBER)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText("${left / 60}h ${left % 60}m until your alarm")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Remind me in 15 min", actionIntent(context, ACTION_LATER, 1))
            .addAction(0, "Skip $ringTime alarm", actionIntent(context, ACTION_SKIP, 2))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) { }
    }

    private fun actionIntent(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, NOTIFICATION_ID + code,
            Intent(context, BedtimeReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** "Remind me in 15 min": the same reminder again a quarter of an hour later. */
    fun later(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        val at = System.currentTimeMillis() + 15 * 60_000L
        val pending = PendingIntent.getBroadcast(
            context, NOTIFICATION_ID + 3,
            Intent(context, BedtimeReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    /**
     * "Skip 7:12 AM alarm": skips that one ring of every alarm due then
     * (a one-time alarm is switched off), and says so for a few seconds.
     */
    suspend fun skipComingAlarm(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        val settings = settings(context).first()
        val repository = AlarmRepository(context)
        val now = System.currentTimeMillis()
        val plan = tonight(repository.alarms.first(), settings, now) ?: return
        repository.alarms.first()
            .filter { it.isEnabled && AlarmTimes.next(it, now) == plan.ring }
            .forEach { if (it.repeats) repository.skipNext(it.id) else repository.toggleAlarm(it.id) }

        val ringTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(plan.ring))
        val done = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_moon)
            .setColor(AMBER)
            .setContentTitle("Alarm skipped")
            .setContentText("The $ringTime alarm won't ring. Sleep well.")
            .setTimeoutAfter(10_000L)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, done)
        } catch (e: SecurityException) { }
    }

    /** "8 h", "7½ h". */
    fun hoursText(minutes: Int): String {
        val h = minutes / 60
        return if (minutes % 60 == 30) "${h}½ h" else "$h h"
    }
}

/** Fires at reminder time: shows the reminder, then books the next one. */
class BedtimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Bedtime.ACTION_LATER -> Bedtime.later(context)
                    Bedtime.ACTION_SKIP -> Bedtime.skipComingAlarm(context)
                    else -> {
                        Bedtime.remind(context)
                        Bedtime.update(context)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
