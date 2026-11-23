package com.rr.numio.clock

import com.rr.numio.clock.ui.bedtime.Voyager
import org.junit.Assert.assertEquals
import org.junit.Test

class VoyagerTest {
    @Test fun oneLightDayOnTheMilestone() {
        assertEquals("25.9", Voyager.billionKm(Voyager.ONE_LIGHT_DAY_AT))
    }

    @Test fun aboutHalfABillionKmFurtherEachYear() {
        val year = 365L * 24 * 60 * 60 * 1000
        assertEquals("26.4", Voyager.billionKm(Voyager.ONE_LIGHT_DAY_AT + year))
    }
}
