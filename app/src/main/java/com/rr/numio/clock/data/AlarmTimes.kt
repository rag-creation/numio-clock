package com.rr.numio.clock.data

import java.util.Calendar

/**
 * When an alarm rings next. Used both to book alarms (AlarmScheduler) and to
 * show "Rings in …" on the card, so the two can never disagree.
 * Plain Kotlin, no Android, so it is unit tested.
 */
object AlarmTimes {

    /** Calendar days in the same order as AlarmModel.days: Sunday … Saturday. */
    val DAYS = listOf(
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
        Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    /**
     * Next time [hour]:[minute] comes round after [now], on [dayOfWeek]
     * (a Calendar day), or on any day when [dayOfWeek] is -1.
     */
    fun occurrence(hour: Int, minute: Int, dayOfWeek: Int, now: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (dayOfWeek != -1) set(Calendar.DAY_OF_WEEK, dayOfWeek)
            if (timeInMillis <= now) {
                if (dayOfWeek != -1) add(Calendar.WEEK_OF_YEAR, 1)
                else add(Calendar.DAY_OF_YEAR, 1)
            }
            // set(DAY_OF_WEEK) can land on an earlier day of this week
            while (timeInMillis <= now) add(Calendar.WEEK_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    /**
     * When the booking for one weekday (or a one-time alarm, -1) should ring.
     * If that ring is the skipped one, the same weekday a week later.
     */
    fun ringFor(alarm: AlarmModel, dayOfWeek: Int, now: Long, honourSkip: Boolean = true): Long {
        val time = occurrence(alarm.hour, alarm.minute, dayOfWeek, now)
        if (honourSkip && dayOfWeek != -1 && alarm.skipAt != 0L && time == alarm.skipAt) {
            return Calendar.getInstance().apply {
                timeInMillis = time
                add(Calendar.WEEK_OF_YEAR, 1)
            }.timeInMillis
        }
        return time
    }

    /** The alarm's next ring after [now]; with honourSkip = false, the one "Skip next" would skip. */
    fun next(alarm: AlarmModel, now: Long, honourSkip: Boolean = true): Long {
        if (!alarm.repeats) return occurrence(alarm.hour, alarm.minute, -1, now)
        return alarm.days.withIndex()
            .filter { it.value }
            .minOf { ringFor(alarm, DAYS[it.index], now, honourSkip) }
    }

    /** True while a skip is set and still ahead. */
    fun isSkipping(alarm: AlarmModel, now: Long): Boolean =
        alarm.repeats && alarm.skipAt > now
}
