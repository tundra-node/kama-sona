package com.elias.kamasona.srs

import com.elias.kamasona.data.Store
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * SM-2 flavoured scheduler. Grades: 0 = again, 1 = hard, 2 = good, 3 = easy.
 * Intervals are in days; learning steps are in minutes.
 */
object Srs {

    const val AGAIN = 0
    const val HARD = 1
    const val GOOD = 2
    const val EASY = 3

    private const val MIN_EASE = 1.3
    private const val MAX_EASE = 3.0
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val MIN_MS = 60L * 1000

    data class Next(
        val ease: Double,
        val intervalDays: Double,
        val reps: Int,
        val lapses: Int,
        val due: Long,
        val known: Boolean,
    )

    fun schedule(card: Store.Card, grade: Int, now: Long = System.currentTimeMillis()): Next {
        var ease = card.ease
        var interval = card.intervalDays
        var reps = card.reps
        var lapses = card.lapses
        var known = card.known

        when (grade) {
            AGAIN -> {
                lapses += 1
                ease = max(MIN_EASE, ease - 0.2)
                interval = 0.0
                reps = 0
                known = false
                return Next(ease, interval, reps, lapses, now + 10 * MIN_MS, known)
            }
            HARD -> {
                ease = max(MIN_EASE, ease - 0.15)
                interval = if (interval < 1.0) 10.0 / (24 * 60) else interval * 1.2
            }
            GOOD -> {
                interval = if (interval < 1.0) 1.0 else interval * ease
            }
            EASY -> {
                ease = min(MAX_EASE, ease + 0.15)
                interval = if (interval < 1.0) 4.0 else interval * ease * 1.3
            }
        }
        reps += 1
        if (interval >= 1.0) interval = interval.roundToLong().toDouble().coerceAtLeast(1.0)
        if (reps >= 3 && grade >= 2) known = true
        return Next(ease, interval, reps, lapses, now + (interval * DAY_MS).toLong(), known)
    }

    /** Human label for the button, e.g. "10 min", "1 d", "3 wk". */
    fun intervalLabel(next: Next): String {
        val days = next.intervalDays
        return when {
            next.reps == 0 -> "10 min"
            days < 1 -> "${(days * 24 * 60).roundToLong()} min"
            days < 30 -> "${days.roundToLong()} d"
            else -> "${(days / 7).roundToLong()} wk"
        }
    }
}
