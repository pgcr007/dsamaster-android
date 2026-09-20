package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.repository.LearningPathRepository
import com.dsamaster.app.data.repository.LearningProgressRepository
import com.dsamaster.app.data.seed.LearningContentSeeder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LearningGateViewModel(
    learningPathRepository: LearningPathRepository,
    learningProgressRepository: LearningProgressRepository,
    userPreferences: UserPreferences
) : ViewModel() {

    sealed class GateState {
        data object Loading : GateState()
        data object Unlocked : GateState()
        data class Locked(val completed: Int, val total: Int) : GateState()
    }

    private val realGateState = learningPathRepository.getAllPaths()
        .map { paths -> paths.firstOrNull { it.name == LearningContentSeeder.FOUNDATIONS_PATH_NAME } }
        .flatMapLatest { path ->
            if (path == null) {
                flowOf(GateState.Loading)
            } else {
                combine(
                    learningProgressRepository.getTotalLessonCountForPath(path.id),
                    learningProgressRepository.getIncompleteLessonCountForPath(path.id)
                ) { total, incomplete ->
                    when {
                        total == 0 -> GateState.Loading
                        incomplete == 0 -> GateState.Unlocked
                        else -> GateState.Locked(completed = total - incomplete, total = total)
                    }
                }
            }
        }

    /**
     * Dev override wins outright when on — doesn't touch LearningProgress,
     * so turning it off drops you right back into wherever real progress
     * actually stands.
     */
    val gateState: StateFlow<GateState> = combine(
        userPreferences.learningGateDebugUnlocked,
        realGateState
    ) { debugUnlocked, realState ->
        if (debugUnlocked) GateState.Unlocked else realState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GateState.Loading
    )
}