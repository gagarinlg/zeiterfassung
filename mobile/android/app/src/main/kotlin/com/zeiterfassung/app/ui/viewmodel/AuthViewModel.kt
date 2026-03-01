package com.zeiterfassung.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeiterfassung.app.data.model.User
import com.zeiterfassung.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            _isAuthenticated.value = authRepository.isLoggedIn()
        }
    }

    fun login(
        email: String,
        password: String,
    ) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("email_and_password_required")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email, password)
            result
                .onSuccess { response ->
                    _uiState.value = AuthUiState.Success(response.user)
                    _isAuthenticated.value = true
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error("login_failed")
                    _isAuthenticated.value = false
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isAuthenticated.value = false
            _uiState.value = AuthUiState.Idle
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
