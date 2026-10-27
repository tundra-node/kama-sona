package com.elias.kamasona.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

class WeekKeyTest {

    private fun us(y: Int, m: Int, d: Int): Calendar =
        GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US).apply { clear(); set(y, m - 1, d) }

    @Test fun midYear() = assertEquals("2026-W44", weekKey(us(2026, 10, 26)))

    @Test fun lateDecemberInWeekOneBelongsToNextYear() {
        // In US numbering, Sun 2026-12-27 starts the week containing Jan 1 2027.
        assertEquals("2027-W1", weekKey(us(2026, 12, 28)))
        assertEquals("2027-W1", weekKey(us(2027, 1, 1)))
    }
}
