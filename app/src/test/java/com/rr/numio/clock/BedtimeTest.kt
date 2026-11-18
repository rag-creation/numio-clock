package com.rr.numio.clock

import com.rr.numio.clock.data.AlarmModel
import com.rr.numio.clock.data.Bedtime
import com.rr.numio.clock.data.BedtimeSettings
import com.rr.numio.clock.data.AlarmTimes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BedtimeTest {

    private lateinit var savedZone: TimeZone
    @Before fun zone() { savedZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dubai")) }
    @After fun restore() { TimeZone.setDefault(savedZone) }

    private fun at(day: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance().apply {
        clear(); set(2026, Calendar.NOVEMBER, day, hour, minute, 0)
    }.timeInMillis
    private fun weekdays(vararg on: Int) = List(7) { it in on }
    private val on = BedtimeSettings(enabled = true, goalMinutes = 480, leadMinutes = 30)

    @Test fun remindsHalfAnHourBeforeEightHoursOfSleep() {
        // Mon–Fri 6:00; Wed 18 Nov at 18:00 → sleep by 22:00, reminder 21:30
        val work = AlarmModel(1, 6, 0, "Work", weekdays(1, 2, 3, 4, 5), true)
        val plan = Bedtime.nextReminder(listOf(work), on, at(18, 18))!!
        assertEquals(at(19, 6), plan.ring)
        assertEquals(at(18, 22), plan.bedtime)
        assertEquals(at(18, 21, 30), plan.remindAt)
    }

    @Test fun afterTonightsReminderBooksTheNextNight() {
        val work = AlarmModel(1, 6, 0, "Work", weekdays(1, 2, 3, 4, 5), true)
        val plan = Bedtime.nextReminder(listOf(work), on, at(18, 23))!!
        assertEquals(at(20, 6), plan.ring)          // Friday's ring
        assertEquals(at(19, 21, 30), plan.remindAt)
    }

    @Test fun skippedRingMeansNoReminderThatNight() {
        val work = AlarmModel(1, 6, 0, "Work", weekdays(1, 2, 3, 4, 5), true)
        val now = at(18, 18)
        val skipped = work.copy(skipAt = AlarmTimes.next(work, now, honourSkip = false)) // Thursday off
        assertEquals(at(19, 21, 30), Bedtime.nextReminder(listOf(skipped), on, now)!!.remindAt)
    }

    @Test fun eveningAlarmsAndOffAlarmsAreIgnored() {
        val evening = AlarmModel(1, 22, 13, "Test", weekdays(3), true)
        val off = AlarmModel(2, 6, 0, "Off", weekdays(1, 2, 3, 4, 5), false)
        assertNull(Bedtime.nextReminder(listOf(evening, off), on, at(18, 18)))
    }

    @Test fun earliestMorningAlarmWins() {
        val a = AlarmModel(1, 7, 0, "A", weekdays(4), true)
        val b = AlarmModel(2, 5, 30, "B", weekdays(4), true)
        assertEquals(at(19, 5, 30), Bedtime.tonight(listOf(a, b), on, at(18, 18))!!.ring)
    }

    @Test fun hoursText() {
        assertEquals("8 h", Bedtime.hoursText(480))
        assertEquals("7½ h", Bedtime.hoursText(450))
    }
}
