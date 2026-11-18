package com.rr.numio.clock

import com.rr.numio.clock.data.AlarmModel
import com.rr.numio.clock.data.AlarmTimes
import com.rr.numio.clock.data.alarmJson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AlarmTimesTest {

    private lateinit var savedZone: TimeZone

    @Before fun zone() { savedZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dubai")) }
    @After fun restore() { TimeZone.setDefault(savedZone) }

    /** Wednesday 18 Nov 2026, [hour]:[minute] in Dubai. */
    private fun at(day: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance().apply {
        clear(); set(2026, Calendar.NOVEMBER, day, hour, minute, 0)
    }.timeInMillis

    private fun weekdays(vararg on: Int) = List(7) { it in on } // 0 = Sunday

    @Test fun alarmsSavedByV140LoadUnchanged() {
        val old = """[{"id":5,"hour":6,"minute":30,"label":"Work","days":[false,true,true,true,true,true,false],"isEnabled":true}]"""
        val alarm = alarmJson.decodeFromString<List<AlarmModel>>(old).single()
        assertEquals(5, alarm.id); assertEquals(6, alarm.hour); assertEquals(30, alarm.minute)
        assertEquals("Work", alarm.label); assertTrue(alarm.isEnabled)
        assertEquals("", alarm.sound); assertTrue(alarm.vibrate); assertEquals(0L, alarm.skipAt)
    }

    @Test fun unknownFieldsDoNotDropAlarms() {
        val future = """[{"id":1,"hour":7,"minute":0,"label":"A","days":[false,false,false,false,false,false,false],"isEnabled":true,"somethingNew":3}]"""
        assertEquals(1, alarmJson.decodeFromString<List<AlarmModel>>(future).size)
    }

    @Test fun oneTimeAlarmRingsTodayOrTomorrow() {
        val a = AlarmModel(1, 7, 0, "A", weekdays(), true)
        assertEquals(at(18, 7), AlarmTimes.next(a, at(18, 6)))
        assertEquals(at(19, 7), AlarmTimes.next(a, at(18, 8)))
    }

    @Test fun weekdayAlarmFindsNextChosenDay() {
        // Mon–Fri 6:00; Wed 18 Nov at 07:00 → Thu 19 Nov 06:00
        val a = AlarmModel(1, 6, 0, "W", weekdays(1, 2, 3, 4, 5), true)
        assertEquals(at(19, 6), AlarmTimes.next(a, at(18, 7)))
        // Fri 20 Nov 07:00 → Mon 23 Nov 06:00
        assertEquals(at(23, 6), AlarmTimes.next(a, at(20, 7)))
    }

    @Test fun skipMovesOnlyTheNextRing() {
        val a = AlarmModel(1, 6, 0, "W", weekdays(1, 2, 3, 4, 5), true)
        val now = at(18, 7)
        val skipped = a.copy(skipAt = AlarmTimes.next(a, now, honourSkip = false))
        assertEquals(at(19, 6), skipped.skipAt)                    // Thursday is skipped
        assertEquals(at(20, 6), AlarmTimes.next(skipped, now))      // Friday rings
        assertEquals(at(26, 6), AlarmTimes.ringFor(skipped, Calendar.THURSDAY, now)) // Thursday is booked a week later
        assertEquals(at(20, 6), AlarmTimes.ringFor(skipped, Calendar.FRIDAY, now))   // other days untouched
        assertTrue(AlarmTimes.isSkipping(skipped, now))
    }

    @Test fun skipOfOnlyChosenDayMovesItAWeek() {
        val a = AlarmModel(1, 6, 0, "Thu", weekdays(4), true)
        val now = at(18, 7)
        val skipped = a.copy(skipAt = AlarmTimes.next(a, now, honourSkip = false))
        assertEquals(at(26, 6), AlarmTimes.next(skipped, now))
    }

    @Test fun passedSkipIsIgnored() {
        val a = AlarmModel(1, 6, 0, "W", weekdays(1, 2, 3, 4, 5), true, skipAt = at(18, 6))
        val now = at(18, 7)
        assertEquals(at(19, 6), AlarmTimes.next(a, now))
        assertFalse(AlarmTimes.isSkipping(a, now))
    }
}
