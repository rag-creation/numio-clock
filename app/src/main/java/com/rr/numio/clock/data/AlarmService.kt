package com.rr.numio.clock.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rr.numio.clock.AlarmActivity
import com.rr.numio.clock.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId    = intent?.getIntExtra("alarm_id", 0) ?: 0
        val alarmLabel = intent?.getStringExtra("alarm_label") ?: "Alarm"
        val alarmHour  = intent?.getIntExtra("alarm_hour", 7) ?: 7
        val alarmMin   = intent?.getIntExtra("alarm_minute", 0) ?: 0
        val alarmStyle  = intent?.getStringExtra("alarm_style") ?: "hold"
        val snoozeMins  = intent?.getIntExtra("snooze_minutes", 10) ?: 10

        val channelId = "numio_alarm_service"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val alarmSound = android.media.RingtoneManager.getDefaultUri(
            android.media.RingtoneManager.TYPE_ALARM
        )

        val channel = NotificationChannel(
            channelId,
            "Numio Alarm Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                alarmSound,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        manager.createNotificationChannel(channel)

        // Prevent system from silencing alarm on pickup
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    android.os.PowerManager.ON_AFTER_RELEASE,
            "numio:alarm_wakelock"
        )
        wakeLock.acquire(10 * 60 * 1000L)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        // FIX #2 — Start at 0 volume, fade in to max over 30 seconds
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)

        // Stop any existing ringtone before starting new one
        AlarmReceiver.stopAlarm()

        // Start ringtone on STREAM_ALARM to bypass silent mode
        val ringtone = android.media.RingtoneManager.getRingtone(this, alarmSound)
        ringtone?.streamType = AudioManager.STREAM_ALARM
        ringtone?.isLooping = true
        AlarmReceiver.setRingtone(ringtone)
        ringtone?.play()

        // Gradually ramp up volume over 30 seconds
        CoroutineScope(Dispatchers.IO).launch {
            val steps = 30
            for (i in 1..steps) {
                delay(1000)
                val vol = ((i.toFloat() / steps) * maxVolume).toInt().coerceAtMost(maxVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0)
            }
        }

        val h = if (alarmHour % 12 == 0) 12 else alarmHour % 12
        val m = alarmMin.toString().padStart(2, '0')
        val ap = if (alarmHour < 12) "AM" else "PM"

        val firingIntent = Intent(this, AlarmActivity::class.java)
        firingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        firingIntent.putExtra("alarm_hour", alarmHour)
        firingIntent.putExtra("alarm_minute", alarmMin)
        firingIntent.putExtra("alarm_label", alarmLabel)
        firingIntent.putExtra("alarm_id", alarmId)
        firingIntent.putExtra("alarm_style", alarmStyle)
        firingIntent.putExtra("snooze_minutes", snoozeMins)

        val pendingIntent = PendingIntent.getActivity(
            this, alarmId, firingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("⏰ $alarmLabel")
            .setContentText("$h:$m $ap — tap to open")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(alarmId + 1000, notification)

        startActivity(firingIntent)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        AlarmReceiver.stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}