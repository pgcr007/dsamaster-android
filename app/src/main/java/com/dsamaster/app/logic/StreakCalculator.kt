package com.dsamaster.app.logic

import com.dsamaster.app.data.entity.StreakEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pure logic for computing streak state from a list of StreakEntry rows.
 * No Android dependencies — fully unit-testable on plain JVM.
 */
class StreakCalculator {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // "yyyy-MM-dd"

    companion object {
        const val MAX_FREEZES_PER_WEEK = 2
    }

    /**
     * Computes the current streak length in days, counting backward from [today].
     * A day "counts" if problemsSolved > 0 OR streakFreezeUsed is true.
     * The streak breaks on the first day (going backward) that has neither.
     *
     * @param entries all streak entries, any order
     * @param today the reference "today" date (injectable for testing)
     */
    fun getCurrentStreak(entries: List<StreakEntry>, today: LocalDate = LocalDate.now()): Int {
        val entriesByDate = entries.associateBy { it.date }
        var streak = 0
        var cursor = today

        // If today has no entry yet (user hasn't solved anything today), that's fine —
        // we start checking from yesterday. Today not counting yet shouldn't zero the streak.
        val todayEntry = entriesByDate[cursor.format(dateFormatter)]
        val todayCounts = todayEntry != null && (todayEntry.problemsSolved > 0 || todayEntry.streakFreezeUsed)

        if (!todayCounts) {
            cursor = cursor.minusDays(1)
        }

        while (true) {
            val entry = entriesByDate[cursor.format(dateFormatter)]
            val counts = entry != null && (entry.problemsSolved > 0 || entry.streakFreezeUsed)
            if (!counts) break
            streak++
            cursor = cursor.minusDays(1)
        }

        return streak
    }

    /**
     * Longest streak ever achieved across all entries (not just the current one).
     */
    fun getLongestStreak(entries: List<StreakEntry>): Int {
        if (entries.isEmpty()) return 0

        val sortedDates = entries
            .filter { it.problemsSolved > 0 || it.streakFreezeUsed }
            .map { LocalDate.parse(it.date, dateFormatter) }
            .sorted()

        if (sortedDates.isEmpty()) return 0

        var longest = 1
        var current = 1

        for (i in 1 until sortedDates.size) {
            val prev = sortedDates[i - 1]
            val curr = sortedDates[i]
            if (prev.plusDays(1) == curr) {
                current++
                longest = maxOf(longest, current)
            } else if (prev != curr) {
                current = 1
            }
        }

        return longest
    }

    /**
     * Counts how many freezes have been used within the 7-day window ending on [referenceDate].
     * Used to enforce the MAX_FREEZES_PER_WEEK cap.
     */
    fun freezesUsedThisWeek(entries: List<StreakEntry>, referenceDate: LocalDate = LocalDate.now()): Int {
        val weekStart = referenceDate.minusDays(6)
        return entries.count { entry ->
            entry.streakFreezeUsed &&
                    LocalDate.parse(entry.date, dateFormatter).let { it >= weekStart && it <= referenceDate }
        }
    }

    /**
     * Determines whether a freeze can still be applied for [referenceDate] without
     * exceeding the weekly cap.
     */
    fun canUseFreeze(entries: List<StreakEntry>, referenceDate: LocalDate = LocalDate.now()): Boolean {
        return freezesUsedThisWeek(entries, referenceDate) < MAX_FREEZES_PER_WEEK
    }

    /**
     * Checks whether the streak would break on [today] if no action is taken —
     * i.e. yesterday had activity/freeze, but today doesn't yet and today isn't over.
     * Useful for triggering a "your streak is at risk" notification later (Phase 10).
     */
    fun isStreakAtRisk(entries: List<StreakEntry>, today: LocalDate = LocalDate.now()): Boolean {
        val entriesByDate = entries.associateBy { it.date }
        val todayEntry = entriesByDate[today.format(dateFormatter)]
        val todayCounts = todayEntry != null && (todayEntry.problemsSolved > 0 || todayEntry.streakFreezeUsed)
        if (todayCounts) return false

        val yesterday = today.minusDays(1)
        val yesterdayEntry = entriesByDate[yesterday.format(dateFormatter)]
        val yesterdayCounted = yesterdayEntry != null &&
                (yesterdayEntry.problemsSolved > 0 || yesterdayEntry.streakFreezeUsed)

        return yesterdayCounted
    }
}