package com.zeiterfassung.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeiterfassung.app.data.model.TrackingStatusResponse
import com.zeiterfassung.app.data.model.User
import com.zeiterfassung.app.data.repository.AuthRepository
import com.zeiterfassung.app.data.repository.TimeTrackingRepository
import com.zeiterfassung.app.data.repository.VacationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val status: TrackingStatusResponse? = null,
    val remainingVacationDays: Double = 0.0,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val timeTrackingRepository: TimeTrackingRepository,
    private val vacationRepository: VacationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "not_authenticated")
                return@launch
            }

            // Load user, status, and vacation balance concurrently
            val userResult = authRepository.getUser(userId)
            val statusResult = timeTrackingRepository.getStatus(userId)
            val vacationResult = vacationRepository.getBalance(userId)

            _uiState.value =
                DashboardUiState(
                    isLoading = false,
                    user = userResult.getOrNull(),
                    status = statusResult.getOrNull(),
                    remainingVacationDays = vacationResult.getOrNull()?.remainingDays ?: 0.0,
                    error = if (userResult.isFailure) "load_failed" else null,
                )
        }
    }
}
