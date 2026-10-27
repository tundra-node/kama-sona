package com.elias.kamasona.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class StreakTest {

    private fun at(y: Int, m: Int, d: Int): Calendar =
        GregorianCalendar(TimeZone.getTimeZone("America/New_York")).apply { clear(); set(y, m - 1, d, 12, 0) }

    @Test fun empty() = assertEquals(0, Store.streakFrom(emptyList(), at(2026, 10, 26)))

    @Test fun runEndingToday() =
        assertEquals(3, Store.streakFrom(listOf("2026-10-26", "2026-10-25", "2026-10-24", "2026-10-20"), at(2026, 10, 26)))

    @Test fun runEndingYesterdayStillCounts() =
        assertEquals(2, Store.streakFrom(listOf("2026-10-25", "2026-10-24"), at(2026, 10, 26)))

    @Test fun brokenStreak() =
        assertEquals(0, Store.streakFrom(listOf("2026-10-23"), at(2026, 10, 26)))

    @Test fun acrossMonthAndYear() =
        assertEquals(3, Store.streakFrom(listOf("2027-01-01", "2026-12-31", "2026-12-30"), at(2027, 1, 1)))
}
