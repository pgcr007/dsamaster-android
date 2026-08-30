package com.dsamaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsamaster.app.data.preferences.UserPreferences
import com.dsamaster.app.data.remote.AuthTokenStore
import com.dsamaster.app.data.remote.ProfileApiClient
import com.dsamaster.app.data.remote.ProfileResult
import com.dsamaster.app.data.remote.dto.ProfileDto
import com.dsamaster.app.data.remote.dto.UpdateProfileRequestDto
import com.dsamaster.app.data.repository.ProblemRepository
import com.dsamaster.app.data.repository.StreakRepository
import com.dsamaster.app.data.repository.UserProgressRepository
import com.dsamaster.app.logic.StreakCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

data class ProfileStats(
    val problemsSolved: Int = 0,
    val totalProblems: Int = 0,
    val easySolved: Int = 0,
    val mediumSolved: Int = 0,
    val hardSolved: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

data class ProfileEditState(
    val name: String = "",
    val bio: String = "",
    val targetRole: String = "",
    val targetCompanies: List<String> = emptyList(),
    val experienceLevel: String = "",
    val preferredLanguage: String = "python",
    val githubHandle: String = "",
    val linkedinUrl: String = "",
    val interviewTargetDate: String? = null
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val profile: ProfileDto? = null,
    val draft: ProfileEditState = ProfileEditState()
)

class ProfileViewModel(
    private val userPreferences: UserPreferences,
    private val userProgressRepository: UserProgressRepository,
    private val problemRepository: ProblemRepository,
    private val streakRepository: StreakRepository,
    private val profileApiClient: ProfileApiClient
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val calculator = StreakCalculator()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Reactive local stats — always live, independent of network state.
    val stats: StateFlow<ProfileStats> = combine(
        userProgressRepository.getAllProgress(),
        problemRepository.getAllProblems(),
        streakRepository.getRecentStreakEntries(365)
    ) { progress, problems, streakEntries ->
        val solvedIds = progress.filter { it.status == "solved" }.map { it.problemId }.toSet()
        val solvedProblems = problems.filter { it.id in solvedIds }
        val today = LocalDate.now()

        ProfileStats(
            problemsSolved = solvedProblems.size,
            totalProblems = problems.size,
            easySolved = solvedProblems.count { it.difficulty == "Easy" },
            mediumSolved = solvedProblems.count { it.difficulty == "Medium" },
            hardSolved = solvedProblems.count { it.difficulty == "Hard" },
            currentStreak = calculator.getCurrentStreak(streakEntries, today),
            longestStreak = calculator.getLongestStreak(streakEntries)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileStats()
    )

    init {
        loadCachedProfile()
        refreshProfile()
    }

    private fun loadCachedProfile() {
        viewModelScope.launch {
            val cached = userPreferences.profileCacheJson.first().takeIf { it.isNotBlank() }
            val parsed = cached?.let {
                try {
                    json.decodeFromString<ProfileDto>(it)
                } catch (e: Exception) {
                    null
                }
            }
            if (parsed != null) {
                _uiState.value = _uiState.value.copy(profile = parsed, draft = parsed.toEditState())
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = profileApiClient.getProfile()) {
                is ProfileResult.Success -> {
                    userPreferences.setProfileCacheJson(json.encodeToString(result.profile))
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = result.profile,
                        draft = if (_uiState.value.isEditing) {
                            _uiState.value.draft
                        } else {
                            result.profile.toEditState()
                        }
                    )
                }
                is ProfileResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun startEditing() {
        val current = _uiState.value.profile
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            draft = current?.toEditState() ?: _uiState.value.draft,
            infoMessage = null,
            errorMessage = null
        )
    }

    fun cancelEditing() {
        val current = _uiState.value.profile
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            draft = current?.toEditState() ?: ProfileEditState()
        )
    }

    fun updateDraft(transform: (ProfileEditState) -> ProfileEditState) {
        _uiState.value = _uiState.value.copy(draft = transform(_uiState.value.draft))
    }

    fun toggleTargetCompany(company: String) {
        updateDraft { draft ->
            val current = draft.targetCompanies
            val updated = if (current.contains(company)) current - company else current + company
            draft.copy(targetCompanies = updated)
        }
    }

    fun save() {
        val draft = _uiState.value.draft

        if (draft.name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Name can't be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            val request = UpdateProfileRequestDto(
                name = draft.name.trim(),
                bio = draft.bio.trim(),
                targetRole = draft.targetRole,
                targetCompanies = draft.targetCompanies,
                experienceLevel = draft.experienceLevel,
                preferredLanguage = draft.preferredLanguage,
                githubHandle = draft.githubHandle.trim(),
                linkedinUrl = draft.linkedinUrl.trim(),
                interviewTargetDate = draft.interviewTargetDate ?: ""
            )

            when (val result = profileApiClient.updateProfile(request)) {
                is ProfileResult.Success -> {
                    userPreferences.setProfileCacheJson(json.encodeToString(result.profile))
                    // Keep the cached display name (used elsewhere) in sync.
                    userPreferences.setAuthSession(
                        token = AuthTokenStore.token.orEmpty(),
                        email = result.profile.email,
                        name = result.profile.name
                    )
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isEditing = false,
                        profile = result.profile,
                        draft = result.profile.toEditState(),
                        infoMessage = "Profile updated"
                    )
                }
                is ProfileResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearAuthSession()
            AuthTokenStore.token = null
        }
    }

    private fun ProfileDto.toEditState() = ProfileEditState(
        name = name,
        bio = bio,
        targetRole = targetRole,
        targetCompanies = targetCompanies,
        experienceLevel = experienceLevel,
        preferredLanguage = preferredLanguage,
        githubHandle = githubHandle,
        linkedinUrl = linkedinUrl,
        interviewTargetDate = interviewTargetDate
    )
}