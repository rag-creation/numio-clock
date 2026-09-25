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

class AlarmRepository(private val context: Context) {

    private val ALARMS_KEY = stringPreferencesKey("alarms")

    val alarms: Flow<List<AlarmModel>> = context.dataStore.data.map { prefs ->
        val json = prefs[ALARMS_KEY] ?: return@map emptyList()
        try {
            Json.decodeFromString<List<AlarmModel>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveAlarms(alarms: List<AlarmModel>) {
        context.dataStore.edit { prefs ->
            prefs[ALARMS_KEY] = Json.encodeToString(alarms)
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
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            current[index] = updated
            saveAlarms(current)
            if (updated.isEnabled) AlarmScheduler.schedule(context, updated)
            else AlarmScheduler.cancel(context, updated)
        }
    }

    suspend fun deleteAlarm(id: Int) {
        val current = getCurrentAlarms().toMutableList()
        val alarm = current.find { it.id == id }
        alarm?.let { AlarmScheduler.cancel(context, it) }
        current.removeAll { it.id == id }
        saveAlarms(current)
    }

    private suspend fun getCurrentAlarms(): List<AlarmModel> {
        var result = emptyList<AlarmModel>()
        context.dataStore.edit { prefs ->
            val json = prefs[ALARMS_KEY] ?: return@edit
            result = try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
        return result
    }
}