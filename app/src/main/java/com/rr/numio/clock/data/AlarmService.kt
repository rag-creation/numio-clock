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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var rampJob: Job? = null
    private var vibrator: android.os.Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId    = intent?.getIntExtra("alarm_id", 0) ?: 0
        val alarmLabel = intent?.getStringExtra("alarm_label") ?: "Alarm"
        val alarmHour  = intent?.getIntExtra("alarm_hour", 7) ?: 7
        val alarmMin   = intent?.getIntExtra("alarm_minute", 0) ?: 0
        val alarmStyle  = intent?.getStringExtra("alarm_style") ?: "hold"
        val snoozeMins  = intent?.getIntExtra("snooze_minutes", 10) ?: 10
        val chosenSound = intent?.getStringExtra("alarm_sound") ?: ""
        val vibrate     = intent?.getBooleanExtra("alarm_vibrate", true) ?: true

        // The notification itself is silent: the alarm's own sound and vibration
        // are played below, so each alarm can have its own (v2 always beeped and buzzed)
        val channelId = "numio_alarm_v3"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel("numio_alarm_v2")

        val channel = NotificationChannel(
            channelId,
            "Numio Alarm Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)

        val alarmSound = playableSound(chosenSound)

        // Prevent system from silencing alarm on pickup
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        // Held only while the alarm rings: released in onDestroy (snooze, dismiss or auto-snooze)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    android.os.PowerManager.ON_AFTER_RELEASE,
            "numio:alarm_wakelock"
        ).apply { acquire(10 * 60 * 1000L) }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        // Remember the phone's own alarm volume once, so it can be put back when the alarm stops
        if (savedAlarmVolume == null) {
            savedAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        }

        // Start at 0 volume, fade in to max over 15 seconds
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)

        // Stop any existing ringtone before starting new one
        AlarmReceiver.stopAlarm()

        // Start ringtone on STREAM_ALARM to bypass silent mode
        val ringtone = android.media.RingtoneManager.getRingtone(this, alarmSound)
            ?: android.media.RingtoneManager.getRingtone(this, defaultSound())
        ringtone?.streamType = AudioManager.STREAM_ALARM
        ringtone?.isLooping = true
        AlarmReceiver.setRingtone(ringtone)
        ringtone?.play()

        // Keep vibrating while it rings, if this alarm wants it
        vibrator?.cancel()
        vibrator = if (vibrate) startVibrating() else null

        // Gradually ramp up volume over 30 seconds
        rampJob?.cancel()
        rampJob = CoroutineScope(Dispatchers.IO).launch {
            val steps = 15
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
        // One id per ring, so the alarm screen can tell a new ring from the same ring opened twice
        firingIntent.putExtra("ring_id", System.currentTimeMillis())
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
            // Always-working way out, even if the alarm screen can't be used
            .addAction(0, "Snooze", actionIntent(AlarmActionReceiver.ACTION_SNOOZE, alarmId, alarmHour, alarmMin, alarmLabel, snoozeMins))
            .addAction(0, "Dismiss", actionIntent(AlarmActionReceiver.ACTION_DISMISS, alarmId, alarmHour, alarmMin, alarmLabel, snoozeMins))
            .build()

        startForeground(alarmId + 1000, notification)

        startActivity(firingIntent)

        return START_NOT_STICKY
    }

    /** The phone's alarm sound, or its ringtone if no alarm sound is set. */
    private fun defaultSound(): android.net.Uri =
        android.media.RingtoneManager.getActualDefaultRingtoneUri(this, android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)

    /**
     * The alarm's chosen sound if it can still be opened, else the default.
     * A deleted music file or a lost permission must never mean a silent alarm.
     */
    private fun playableSound(chosen: String): android.net.Uri {
        if (chosen.isBlank()) return defaultSound()
        return try {
            val uri = android.net.Uri.parse(chosen)
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { uri } ?: defaultSound()
        } catch (e: Exception) {
            android.util.Log.w("NumioAlarm", "Chosen sound can't be played, using default", e)
            defaultSound()
        }
    }

    private fun startVibrating(): android.os.Vibrator? {
        val v = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
        if (v == null || !v.hasVibrator()) return null
        // buzz 0.8 s, rest 0.6 s, repeat from the start until the alarm stops
        val effect = android.os.VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0)
        val attrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .build()
        @Suppress("DEPRECATION")
        v.vibrate(effect, attrs)
        return v
    }

    private fun actionIntent(
        action: String, alarmId: Int, hour: Int, minute: Int, label: String, snoozeMins: Int
    ): PendingIntent {
        val intent = Intent(this, AlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra("alarm_id", alarmId)
            putExtra("alarm_hour", hour)
            putExtra("alarm_minute", minute)
            putExtra("alarm_label", label)
            putExtra("snooze_minutes", snoozeMins)
        }
        val code = alarmId * 2 + if (action == AlarmActionReceiver.ACTION_SNOOZE) 0 else 1
        return PendingIntent.getBroadcast(
            this, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        rampJob?.cancel()
        rampJob = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        savedAlarmVolume?.let { vol ->
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0)
        }
        savedAlarmVolume = null
        AlarmReceiver.stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.cancelAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // The phone's alarm volume before this alarm started ringing (null when not ringing)
        @Volatile private var savedAlarmVolume: Int? = null

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }
    }
}