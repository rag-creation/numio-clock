package com.rr.numio.clock.ui.bedtime

/**
 * Voyager 1's distance from Earth, worked out on the phone (no internet).
 *
 * Anchor: NASA's Voyager 1 reached one light-day from Earth on
 * 18 Nov 2026 at 10:16:07 UTC. It moves away at about 16.99 km/s.
 * Earth's own orbit moves the real figure a little either way, so the
 * screen rounds and says "about".
 */
object Voyager {
    /** 18 Nov 2026 10:16:07 UTC. */
    const val ONE_LIGHT_DAY_AT = 1_794_996_967_000L
    /** One light-day: 299 792.458 km/s × 86 400 s. */
    const val ONE_LIGHT_DAY_KM = 25_902_068_371.2
    const val SPEED_KM_S = 16.99

    fun distanceKm(now: Long): Double =
        ONE_LIGHT_DAY_KM + (now - ONE_LIGHT_DAY_AT) / 1000.0 * SPEED_KM_S

    /** "25.9" (billion km), one decimal. */
    fun billionKm(now: Long): String =
        String.format(java.util.Locale.US, "%.1f", distanceKm(now) / 1e9)
}
