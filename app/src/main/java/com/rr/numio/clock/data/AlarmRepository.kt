package com.rr.numio.clock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "numio_alarms")

/** Reads alarms saved by any version; a field this version doesn't know is ignored, never a reason to drop alarms. */
internal val alarmJson = Json { ignoreUnknownKeys = true }

class AlarmRepository(private val context: Context) {

    private val ALARMS_KEY = stringPreferencesKey("alarms")

    val alarms: Flow<List<AlarmModel>> = context.dataStore.data.map { prefs ->
        val json = prefs[ALARMS_KEY] ?: return@map emptyList()
        try {
            alarmJson.decodeFromString<List<AlarmModel>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveAlarms(alarms: List<AlarmModel>) {
        context.dataStore.edit { prefs ->
            prefs[ALARMS_KEY] = alarmJson.encodeToString(alarms)
        }
    }

    suspend fun addAlarm(alarm: AlarmModel) {
        val current = getCurrentAlarms().toMutableList()
        current.removeAll { it.id == alarm.id }
        current.add(alarm)
        saveAlarms(current)
        if (alarm.isEnabled) AlarmScheduler.schedule(context, alarm)
    }

    suspend fun toggleAlarm(id: Int) {
        val current = getCurrentAlarms().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val alarm = current[index]
            // Switching it off or on again forgets any skipped ring
            val updated = alarm.copy(isEnabled = !alarm.isEnabled, skipAt = 0L)
            current[index] = updated
            saveAlarms(current)
            if (updated.isEnabled) AlarmScheduler.schedule(context, updated)
            else AlarmScheduler.cancel(context, updated)
        }
    }

    /**
     * Skip only the next ring of a repeating alarm (a holiday). The alarm stays
     * on and rings again as normal after that.
     */
    suspend fun skipNext(id: Int) {
        val current = getCurrentAlarms().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return
        val alarm = current[index]
        if (!alarm.isEnabled || !alarm.repeats) return
        val updated = alarm.copy(
            skipAt = AlarmTimes.next(alarm, System.currentTimeMillis(), honourSkip = false)
        )
        current[index] = updated
        saveAlarms(current)
        AlarmScheduler.schedule(context, updated)
    }

    /** Undo "Skip next": the skipped ring is booked again. */
    suspend fun cancelSkip(id: Int) {
        val current = getCurrentAlarms().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return
        val updated = current[index].copy(skipAt = 0L)
        current[index] = updated
        saveAlarms(current)
        if (updated.isEnabled) AlarmScheduler.schedule(context, updated)
    }

    /** The saved alarm with this id, or null. */
    suspend fun find(id: Int): AlarmModel? = getCurrentAlarms().find { it.id == id }

    suspend fun deleteAlarm(id: Int) {
        val current = getCurrentAlarms().toMutableList()
        val alarm = current.find { it.id == id }
        alarm?.let { AlarmScheduler.cancel(context, it) }
        current.removeAll { it.id == id }
        saveAlarms(current)
    }

    /**
     * Android forgets every alarm on reboot, and each weekday alarm is only
     * booked once. Call this on boot, on clock/time zone changes and on app
     * start so every enabled alarm is booked again for its next ring.
     */
    suspend fun rescheduleAll() {
        getCurrentAlarms().filter { it.isEnabled }.forEach {
            AlarmScheduler.schedule(context, it)
        }
    }

    /**
     * Called right after an alarm rings.
     * Repeating alarm: book the same weekday for next week.
     * One-time alarm: switch it off, it has done its job.
     */
    suspend fun onAlarmFired(id: Int) {
        val current = getCurrentAlarms().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return
        val alarm = current[index]
        if (!alarm.isEnabled) return
        if (alarm.days.any { it }) {
            // A skip that has passed is done with
            if (alarm.skipAt != 0L && alarm.skipAt <= System.currentTimeMillis()) {
                current[index] = alarm.copy(skipAt = 0L)
                saveAlarms(current)
            }
            AlarmScheduler.schedule(context, current[index])
        } else {
            current[index] = alarm.copy(isEnabled = false)
            saveAlarms(current)
        }
    }

    private suspend fun getCurrentAlarms(): List<AlarmModel> {
        var result = emptyList<AlarmModel>()
        context.dataStore.edit { prefs ->
            val json = prefs[ALARMS_KEY] ?: return@edit
            result = try {
                alarmJson.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
        return result
    }
}