package com.zeiterfassung.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeiterfassung.app.data.model.VacationBalance
import com.zeiterfassung.app.data.model.VacationRequest
import com.zeiterfassung.app.data.repository.AuthRepository
import com.zeiterfassung.app.data.repository.VacationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VacationUiState(
    val isLoading: Boolean = true,
    val balance: VacationBalance? = null,
    val requests: List<VacationRequest> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class VacationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vacationRepository: VacationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VacationUiState())
    val uiState: StateFlow<VacationUiState> = _uiState.asStateFlow()

    init {
        loadVacationData()
    }

    fun loadVacationData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "not_authenticated")
                return@launch
            }

            val balanceResult = vacationRepository.getBalance(userId)
            val requestsResult = vacationRepository.getRequests(userId)

            _uiState.value =
                VacationUiState(
                    isLoading = false,
                    balance = balanceResult.getOrNull(),
                    requests = requestsResult.getOrNull()?.content ?: emptyList(),
                    error = if (balanceResult.isFailure) "load_failed" else null,
                )
        }
    }
}
