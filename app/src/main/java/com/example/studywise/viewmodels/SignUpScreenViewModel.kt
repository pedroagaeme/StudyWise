package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.constants.EMAIL_VERIFICATION_DEEP_LINK
import com.example.studywise.data.repository.AuthRepository
import com.example.studywise.ui.screens.signup.SignUpScreenAction
import com.example.studywise.ui.screens.signup.SignUpScreenEffect
import com.example.studywise.ui.screens.signup.SignUpScreenUiState
import com.example.studywise.ui.screens.signup.SignUpStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpScreenUiState())
    val uiState: StateFlow<SignUpScreenUiState> = _uiState.asStateFlow()

    fun effectConsumed() {
        _uiState.update { it.copy(pendingEffect = null) }
    }

    fun onAction(action: SignUpScreenAction) {
        when (action) {
            is SignUpScreenAction.OnEmailChange -> {
                _uiState.update { it.copy(email = action.email, errorMessage = null) }
            }
            is SignUpScreenAction.OnPasswordChange -> {
                _uiState.update { it.copy(password = action.password, errorMessage = null) }
            }
            is SignUpScreenAction.TogglePasswordVisibility -> {
                _uiState.update { it.copy(showPassword = !it.showPassword) }
            }
            is SignUpScreenAction.OnBackToLoginClick -> {
                _uiState.update { it.copy(pendingEffect = SignUpScreenEffect.NavigateToLogin) }
            }
            is SignUpScreenAction.OnCreateAccountClick -> {
                viewModelScope.launch {
                    val email = _uiState.value.email.trim()
                    val password = _uiState.value.password
                    if (email.isEmpty() || password.isEmpty()) {
                        _uiState.update {
                            it.copy(errorMessage = "Please enter your email and password")
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(currentStep = SignUpStep.LOADING, errorMessage = null)
                    }
                    try {
                        authRepository.register(email = email, password = password)
                        authRepository.login(email = email, password = password)
                        authRepository.sendVerification(EMAIL_VERIFICATION_DEEP_LINK)
                        _uiState.update { it.copy(currentStep = SignUpStep.CHECK_EMAIL) }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                currentStep = SignUpStep.FORM,
                                errorMessage = e.message ?: "Could not create account"
                            )
                        }
                    }
                }
            }
        }
    }
}
