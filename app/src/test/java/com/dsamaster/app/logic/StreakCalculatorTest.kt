package com.dsamaster.app.logic

import com.dsamaster.app.data.entity.StreakEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val calculator = StreakCalculator()
    private val today = LocalDate.of(2026, 8, 8) // fixed reference date

    private fun entry(daysAgo: Long, solved: Int = 1, freeze: Boolean = false) = StreakEntry(
        date = today.minusDays(daysAgo).toString(),
        minutesActive = 30,
        problemsSolved = solved,
        streakFreezeUsed = freeze
    )

    @Test
    fun `no entries means zero streak`() {
        val streak = calculator.getCurrentStreak(emptyList(), today)
        assertEquals(0, streak)
    }

    @Test
    fun `consecutive days including today gives correct streak`() {
        val entries = listOf(
            entry(daysAgo = 0), // today
            entry(daysAgo = 1),
            entry(daysAgo = 2)
        )
        assertEquals(3, calculator.getCurrentStreak(entries, today))
    }

    @Test
    fun `streak intact if today has no entry yet but yesterday does`() {
        val entries = listOf(
            entry(daysAgo = 1),
            entry(daysAgo = 2),
            entry(daysAgo = 3)
        )
        // "today" not solved yet, but streak should still count from yesterday backward
        assertEquals(3, calculator.getCurrentStreak(entries, today))
    }

    @Test
    fun `streak breaks on a gap day`() {
        val entries = listOf(
            entry(daysAgo = 0),
            entry(daysAgo = 1),
            // gap at daysAgo = 2
            entry(daysAgo = 3)
        )
        assertEquals(2, calculator.getCurrentStreak(entries, today))
    }

    @Test
    fun `freeze day counts toward streak`() {
        val entries = listOf(
            entry(daysAgo = 0),
            entry(daysAgo = 1, solved = 0, freeze = true), // froze this day, didn't solve
            entry(daysAgo = 2)
        )
        assertEquals(3, calculator.getCurrentStreak(entries, today))
    }

    @Test
    fun `longest streak finds max run even if not current`() {
        val entries = listOf(
            entry(daysAgo = 0), // current streak of 1
            // gap
            entry(daysAgo = 10),
            entry(daysAgo = 11),
            entry(daysAgo = 12),
            entry(daysAgo = 13),
            entry(daysAgo = 14) // old streak of 5
        )
        assertEquals(5, calculator.getLongestStreak(entries))
        assertEquals(1, calculator.getCurrentStreak(entries, today))
    }

    @Test
    fun `freeze cap blocks after max freezes used this week`() {
        val entries = listOf(
            entry(daysAgo = 1, solved = 0, freeze = true),
            entry(daysAgo = 3, solved = 0, freeze = true)
        )
        assertEquals(2, calculator.freezesUsedThisWeek(entries, today))
        assertFalse(calculator.canUseFreeze(entries, today))
    }

    @Test
    fun `freeze cap allows freeze when under weekly limit`() {
        val entries = listOf(
            entry(daysAgo = 1, solved = 0, freeze = true)
        )
        assertEquals(1, calculator.freezesUsedThisWeek(entries, today))
        assertTrue(calculator.canUseFreeze(entries, today))
    }

    @Test
    fun `streak at risk when yesterday counted but today has not yet`() {
        val entries = listOf(
            entry(daysAgo = 1)
        )
        assertTrue(calculator.isStreakAtRisk(entries, today))
    }

    @Test
    fun `streak not at risk if today already counted`() {
        val entries = listOf(
            entry(daysAgo = 0),
            entry(daysAgo = 1)
        )
        assertFalse(calculator.isStreakAtRisk(entries, today))
    }

    @Test
    fun `streak not at risk if yesterday was already a gap`() {
        val entries = listOf(
            entry(daysAgo = 2)
        )
        assertFalse(calculator.isStreakAtRisk(entries, today))
    }
}