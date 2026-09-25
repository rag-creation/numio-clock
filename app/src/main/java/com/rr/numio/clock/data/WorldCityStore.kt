package com.rr.numio.clock.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class WorldCity(val name: String, val offset: String, val utcOffset: Int)

object WorldCityStore {

    private val CITIES_KEY          = stringPreferencesKey("selected_cities")
    private val ACCENT_COLOR_KEY    = longPreferencesKey("accent_color")
    private val STOPWATCH_MS_KEY    = longPreferencesKey("stopwatch_elapsed_ms")
    private val STOPWATCH_LAPS_KEY  = stringPreferencesKey("stopwatch_laps")
    private val ALARM_STYLE_KEY     = stringPreferencesKey("alarm_style")
    private val SNOOZE_DURATION_KEY = longPreferencesKey("snooze_duration_minutes")
    private val DIAL_STYLE_KEY      = stringPreferencesKey("dial_style")
    private val ALARM_STYLE_V2_KEY  = stringPreferencesKey("alarm_style_v2")

    private val DEFAULT_CITIES = listOf(
        WorldCity("Pathanamthitta", "UTC +5:30", 5),
        WorldCity("London",         "UTC +1",    1),
        WorldCity("New York",       "UTC −4",   -4),
        WorldCity("Dubai",          "UTC +4",    4),
        WorldCity("Tokyo",          "UTC +9",    9),
    )

    // Accent color
    fun getAccentColor(context: Context): Flow<Long?> =
        context.dataStore.data.map { it[ACCENT_COLOR_KEY] }

    suspend fun saveAccentColor(context: Context, colorLong: Long) {
        context.dataStore.edit { it[ACCENT_COLOR_KEY] = colorLong }
    }

    // Cities
    fun getCities(context: Context): Flow<List<WorldCity>> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[CITIES_KEY]
            if (json.isNullOrBlank()) {
                DEFAULT_CITIES
            } else {
                try {
                    Json.decodeFromString<List<SerializableCity>>(json)
                        .map { it.toWorldCity() }
                } catch (e: Exception) {
                    DEFAULT_CITIES
                }
            }
        }
    }

    suspend fun saveCities(context: Context, cities: List<WorldCity>) {
        context.dataStore.edit { prefs ->
            prefs[CITIES_KEY] = Json.encodeToString(cities.map { it.toSerializable() })
        }
    }

    // Alarm style
    fun getAlarmStyle(context: Context): Flow<String> =
        context.dataStore.data.map { it[ALARM_STYLE_V2_KEY] ?: "hold" }

    suspend fun saveAlarmStyle(context: Context, style: String) {
        context.dataStore.edit { it[ALARM_STYLE_V2_KEY] = style }
    }

    fun getDialStyle(context: Context): Flow<String> =
        context.dataStore.data.map { it[DIAL_STYLE_KEY] ?: "CLASSIC" }

    suspend fun saveDialStyle(context: Context, style: String) {
        context.dataStore.edit { it[DIAL_STYLE_KEY] = style }
    }

    // Snooze duration
    fun getSnoozeDuration(context: Context): Flow<Int> =
        context.dataStore.data.map { (it[SNOOZE_DURATION_KEY] ?: 10L).toInt() }

    suspend fun saveSnoozeDuration(context: Context, minutes: Int) {
        context.dataStore.edit { it[SNOOZE_DURATION_KEY] = minutes.toLong() }
    }

    // Stopwatch
    fun getStopwatchState(context: Context): Flow<Pair<Long, List<Long>>> {
        return context.dataStore.data.map { prefs ->
            val elapsed = prefs[STOPWATCH_MS_KEY] ?: 0L
            val lapsJson = prefs[STOPWATCH_LAPS_KEY]
            val laps = if (lapsJson.isNullOrBlank()) emptyList()
            else try {
                Json.decodeFromString<List<Long>>(lapsJson)
            } catch (e: Exception) {
                emptyList()
            }
            Pair(elapsed, laps)
        }
    }

    suspend fun saveStopwatchState(context: Context, elapsedMs: Long, laps: List<Long>) {
        context.dataStore.edit { prefs ->
            prefs[STOPWATCH_MS_KEY] = elapsedMs
            prefs[STOPWATCH_LAPS_KEY] = Json.encodeToString(laps)
        }
    }

    suspend fun clearStopwatchState(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[STOPWATCH_MS_KEY] = 0L
            prefs[STOPWATCH_LAPS_KEY] = "[]"
        }
    }
}

@kotlinx.serialization.Serializable
private data class SerializableCity(
    val name: String,
    val offset: String,
    val utcOffset: Int
)

private fun WorldCity.toSerializable() = SerializableCity(name, offset, utcOffset)
private fun SerializableCity.toWorldCity() = WorldCity(name, offset, utcOffset)