package com.zeiterfassung.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeiterfassung.app.data.model.TrackingStatusResponse
import com.zeiterfassung.app.data.repository.AuthRepository
import com.zeiterfassung.app.data.repository.TimeTrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimeTrackingUiState(
    val isLoading: Boolean = true,
    val isActionLoading: Boolean = false,
    val status: TrackingStatusResponse? = null,
    val error: String? = null,
    val actionError: String? = null,
)

@HiltViewModel
class TimeTrackingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val timeTrackingRepository: TimeTrackingRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TimeTrackingUiState())
    val uiState: StateFlow<TimeTrackingUiState> = _uiState.asStateFlow()

    init {
        loadStatus()
    }

    fun loadStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "not_authenticated")
                return@launch
            }
            timeTrackingRepository
                .getStatus(userId)
                .onSuccess { status ->
                    _uiState.value = _uiState.value.copy(isLoading = false, status = status)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "load_failed")
                }
        }
    }

    fun clockIn() = performAction { timeTrackingRepository.clockIn() }

    fun clockOut() = performAction { timeTrackingRepository.clockOut() }

    fun startBreak() = performAction { timeTrackingRepository.startBreak() }

    fun endBreak() = performAction { timeTrackingRepository.endBreak() }

    private fun performAction(action: suspend () -> Result<*>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, actionError = null)
            action()
                .onSuccess { loadStatus() }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, actionError = "action_failed")
                }
        }
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionError = null)
    }
}
