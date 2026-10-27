package com.elias.kamasona.srs

import com.elias.kamasona.data.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun card(
        ease: Double = 2.5, interval: Double = 0.0, reps: Int = 0, lapses: Int = 0, known: Boolean = false,
    ) = Store.Card(1, "moku", "tp>en", ease, interval, reps, lapses, 0, 0, known)

    @Test fun newCardGoodIsOneDay() {
        val n = Srs.schedule(card(), Srs.GOOD, now)
        assertEquals(1.0, n.intervalDays, 0.0)
        assertEquals(1, n.reps)
        assertEquals(now + day, n.due)
    }

    @Test fun newCardEasyIsFourDaysAndRaisesEase() {
        val n = Srs.schedule(card(), Srs.EASY, now)
        assertEquals(4.0, n.intervalDays, 0.0)
        assertEquals(2.65, n.ease, 1e-9)
    }

    @Test fun newCardHardIsTenMinutes() {
        val n = Srs.schedule(card(), Srs.HARD, now)
        assertEquals(now + 10 * 60 * 1000, n.due)
        assertEquals("10 min", Srs.intervalLabel(n))
    }

    @Test fun againResetsAndCountsLapse() {
        val n = Srs.schedule(card(interval = 12.0, reps = 5, known = true), Srs.AGAIN, now)
        assertEquals(0.0, n.intervalDays, 0.0)
        assertEquals(0, n.reps)
        assertEquals(1, n.lapses)
        assertFalse(n.known)
        assertEquals(2.3, n.ease, 1e-9)
        assertEquals(now + 10 * 60 * 1000, n.due)
    }

    @Test fun goodMultipliesByEaseAndRounds() {
        val n = Srs.schedule(card(interval = 3.0, reps = 2), Srs.GOOD, now)
        assertEquals(8.0, n.intervalDays, 0.0) // 3 * 2.5 = 7.5 -> 8
        assertTrue(n.known) // third successful rep
    }

    @Test fun easeStaysWithinBounds() {
        var c = card(ease = 1.35)
        repeat(5) {
            val n = Srs.schedule(c, Srs.AGAIN, now)
            c = c.copy(ease = n.ease)
        }
        assertEquals(1.3, c.ease, 1e-9)
        val high = Srs.schedule(card(ease = 2.95, interval = 2.0, reps = 1), Srs.EASY, now)
        assertEquals(3.0, high.ease, 1e-9)
    }

    @Test fun labels() {
        assertEquals("1 d", Srs.intervalLabel(Srs.Next(2.5, 1.0, 1, 0, 0, false)))
        assertEquals("6 wk", Srs.intervalLabel(Srs.Next(2.5, 40.0, 4, 0, 0, true)))
    }
}
