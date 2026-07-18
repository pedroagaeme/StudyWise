package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.repository.AuthRepository
import com.example.studywise.data.repository.ThemePreferencesRepository
import com.example.studywise.ui.screens.tabs.settings.SettingsScreenAction
import com.example.studywise.ui.screens.tabs.settings.SettingsScreenEffect
import com.example.studywise.ui.screens.tabs.settings.SettingsScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themePreferencesRepository: ThemePreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsScreenUiState())
    val uiState: StateFlow<SettingsScreenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferencesRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun effectConsumed() {
        _uiState.update { it.copy(pendingEffect = null) }
    }

    fun onAction(action: SettingsScreenAction) {
        when (action) {
            is SettingsScreenAction.OnThemeModeChange -> {
                themePreferencesRepository.setThemeMode(action.themeMode)
            }
            is SettingsScreenAction.OnLogoutClick -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoggingOut = true) }
                    try {
                        authRepository.logout()
                        _uiState.update {
                            it.copy(
                                isLoggingOut = false,
                                pendingEffect = SettingsScreenEffect.NavigateToLogin
                            )
                        }
                    } catch (_: Exception) {
                        _uiState.update { it.copy(isLoggingOut = false) }
                    }
                }
            }
        }
    }
}
