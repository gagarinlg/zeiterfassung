package com.zeiterfassung.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeiterfassung.app.data.repository.ServerConfigPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerSettingsViewModel @Inject constructor(
    private val serverConfigPreferences: ServerConfigPreferences,
) : ViewModel() {
    companion object {
        const val DEFAULT_URL = "https://zeiterfassung.example.com/api/"
    }

    private val _serverUrl = MutableStateFlow(DEFAULT_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _isManaged = MutableStateFlow(false)
    val isManaged: StateFlow<Boolean> = _isManaged.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        loadServerUrl()
    }

    private fun loadServerUrl() {
        viewModelScope.launch {
            val managedUrl = serverConfigPreferences.getManagedServerUrl()
            if (managedUrl != null) {
                _isManaged.value = true
                _serverUrl.value = managedUrl
            } else {
                _serverUrl.value = serverConfigPreferences.getServerUrl() ?: DEFAULT_URL
            }
        }
    }

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            serverConfigPreferences.saveServerUrl(url)
            _serverUrl.value = url
            _saveSuccess.value = true
        }
    }
}
