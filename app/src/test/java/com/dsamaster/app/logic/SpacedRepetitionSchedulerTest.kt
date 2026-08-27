package com.dsamaster.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionSchedulerTest {

    private val dayMillis = 24L * 60L * 60L * 1000L
    private val now = 1_700_000_000_000L // fixed reference instant

    @Test
    fun `first solve schedules review 1 day out`() {
        val next = SpacedRepetitionScheduler.computeNextReviewDate(1, now)
        assertEquals(now + dayMillis, next)
    }

    @Test
    fun `second successful review schedules 3 days out`() {
        val next = SpacedRepetitionScheduler.computeNextReviewDate(2, now)
        assertEquals(now + 3 * dayMillis, next)
    }

    @Test
    fun `third successful review schedules 7 days out`() {
        val next = SpacedRepetitionScheduler.computeNextReviewDate(3, now)
        assertEquals(now + 7 * dayMillis, next)
    }

    @Test
    fun `fourth successful review schedules 21 days out`() {
        val next = SpacedRepetitionScheduler.computeNextReviewDate(4, now)
        assertEquals(now + 21 * dayMillis, next)
    }

    @Test
    fun `fifth successful review has no more scheduled reviews`() {
        val next = SpacedRepetitionScheduler.computeNextReviewDate(5, now)
        assertNull(next)
    }

    @Test
    fun `mastered check matches schedule exhaustion`() {
        assertFalse(SpacedRepetitionScheduler.isMastered(3))
        assertTrue(SpacedRepetitionScheduler.isMastered(4))
        assertTrue(SpacedRepetitionScheduler.isMastered(10))
    }
}