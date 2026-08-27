package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.entity.StreakEntry
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.repository.StreakRepository
import com.dsamaster.app.logic.StreakCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class StreakUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val dailyGoal: Int = 1,
    val recentEntries: List<StreakEntry> = emptyList(),
    val isStreakAtRisk: Boolean = false,
    val canUseFreeze: Boolean = true
)

class StreakViewModel(
    private val streakRepository: StreakRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val calculator = StreakCalculator()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val uiState: StateFlow<StreakUiState> = combine(
        streakRepository.getRecentStreakEntries(180), // ~6 months for heatmap
        userPreferences.dailyGoal
    ) { entries, goal ->
        val today = LocalDate.now()
        StreakUiState(
            currentStreak = calculator.getCurrentStreak(entries, today),
            longestStreak = calculator.getLongestStreak(entries),
            dailyGoal = goal,
            recentEntries = entries,
            isStreakAtRisk = calculator.isStreakAtRisk(entries, today),
            canUseFreeze = calculator.canUseFreeze(entries, today)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreakUiState()
    )

    /**
     * Records progress for today — increments problemsSolved on today's entry,
     * creating it if it doesn't exist yet.
     */
    fun recordProblemSolved() {
        viewModelScope.launch {
            val today = LocalDate.now().format(dateFormatter)
            val existing = streakRepository.getStreakEntryByDate(today)
            // Read current value once via the repository's Flow-backed DAO isn't ideal here;
            // simplest correct approach: fetch-then-write inside the coroutine.
            val current = uiState.value.recentEntries.find { it.date == today }

            val updated = current?.copy(
                problemsSolved = current.problemsSolved + 1,
                minutesActive = current.minutesActive
            ) ?: StreakEntry(
                date = today,
                minutesActive = 0,
                problemsSolved = 1,
                streakFreezeUsed = false
            )

            streakRepository.insertStreakEntry(updated)
        }
    }

    /**
     * Manually applies a streak freeze for today, if under the weekly cap.
     */
    fun useStreakFreeze() {
        viewModelScope.launch {
            val today = LocalDate.now().format(dateFormatter)
            if (!calculator.canUseFreeze(uiState.value.recentEntries, LocalDate.now())) return@launch

            val current = uiState.value.recentEntries.find { it.date == today }
            val updated = current?.copy(streakFreezeUsed = true) ?: StreakEntry(
                date = today,
                minutesActive = 0,
                problemsSolved = 0,
                streakFreezeUsed = true
            )

            streakRepository.insertStreakEntry(updated)
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            userPreferences.setDailyGoal(goal)
        }
    }
}