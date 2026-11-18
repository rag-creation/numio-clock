package com.rr.numio.clock.data

import kotlinx.serialization.Serializable

/**
 * One alarm, saved as JSON.
 *
 * Every field added after v1.4.0 has a default, so alarms saved by older
 * versions load unchanged (AlarmTimesTest checks this).
 */
@Serializable
data class AlarmModel(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val days: List<Boolean>,
    val isEnabled: Boolean,
    /** Sound to ring with, as a content uri. Empty = the phone's default alarm sound. */
    val sound: String = "",
    /** Name shown for [sound] on the alarm card. */
    val soundName: String = "",
    /** Keep vibrating while it rings. */
    val vibrate: Boolean = true,
    /** Ring time (millis) of one skipped ring of a repeating alarm. 0 = nothing skipped. */
    val skipAt: Long = 0L
) {
    val repeats: Boolean get() = days.any { it }
}
