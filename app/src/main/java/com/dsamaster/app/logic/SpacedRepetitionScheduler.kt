package com.dsamaster.app.logic

/**
 * Pure logic for SM-2-lite spaced repetition scheduling.
 * No Android dependencies — fully unit-testable on plain JVM.
 *
 * Every time a problem is genuinely solved (first time, or as a due review),
 * timesReviewed increments by one and the next review date is pushed out
 * further, following a fixed interval schedule. Once the schedule is
 * exhausted, the problem is considered "mastered" and drops out of the
 * review queue (nextReviewDate = null).
 */
object SpacedRepetitionScheduler {

    // Days until the next review, indexed by (timesReviewed - 1) after this solve.
    val INTERVAL_DAYS = listOf(1L, 3L, 7L, 21L)

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    /**
     * @param timesReviewedAfterThisSolve total successful solves for this problem,
     *        INCLUDING the one that just happened (pass existing + 1)
     * @param fromMillis reference "now" to schedule from (injectable for testing)
     * @return epoch millis of the next review, or null if the schedule is exhausted
     *         (problem is mastered — no more scheduled reviews)
     */
    fun computeNextReviewDate(timesReviewedAfterThisSolve: Int, fromMillis: Long): Long? {
        val index = timesReviewedAfterThisSolve - 1
        if (index < 0 || index >= INTERVAL_DAYS.size) return null
        return fromMillis + INTERVAL_DAYS[index] * DAY_MILLIS
    }

    /**
     * True once a problem has cleared every scheduled interval.
     */
    fun isMastered(timesReviewed: Int): Boolean = timesReviewed >= INTERVAL_DAYS.size
}