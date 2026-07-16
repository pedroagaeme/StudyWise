package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.repository.AuthRepository
import com.example.studywise.ui.screens.login.LoginScreenAction
import com.example.studywise.ui.screens.login.LoginScreenEffect
import com.example.studywise.ui.screens.login.LoginScreenUiState
import com.example.studywise.ui.screens.login.LoginStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(LoginScreenUiState())
    val uiState: StateFlow<LoginScreenUiState> = _uiState.asStateFlow()


    init {
        viewModelScope.launch {
            if(authRepository.isSessionActive()) {
                _uiState.update { currentState ->
                    currentState.copy(pendingEffect = LoginScreenEffect.LoginSuccess)
                }
            } else {
                _uiState.update { currentState ->
                    currentState.copy(currentStep = LoginStep.NOT_LOADING)
                }
            }
        }
    }

    fun effectConsumed() {
        _uiState.update {
            it.copy(pendingEffect = null)
        }
    }

    fun onAction(action: LoginScreenAction) {
        when (action) {
            is LoginScreenAction.OnLoginButtonClick -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(currentStep = LoginStep.LOADING) }
                    try {
                        authRepository.login(
                            email = _uiState.value.email,
                            password = _uiState.value.password
                        )
                        _uiState.update { currentState ->
                            currentState.copy(pendingEffect = LoginScreenEffect.LoginSuccess)
                        }
                    } catch (e: Exception) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                currentStep = LoginStep.NOT_LOADING,
                                pendingEffect = LoginScreenEffect.LoginFailure(e.message ?: "Unknown error")
                            )
                        }
                    }
                }
            }
            is LoginScreenAction.OnSignUpButtonClick -> {
                viewModelScope.launch {
                    _uiState.update { currentState ->
                        currentState.copy(pendingEffect = LoginScreenEffect.NavigateToSignUpScreen)
                    }
                }
            }
            is LoginScreenAction.OnEmailChange -> {
                _uiState.update { currentState ->
                    currentState.copy(email = action.email)
                }
            }
            is LoginScreenAction.OnPasswordChange -> {
                _uiState.update { currentState ->
                    currentState.copy(password = action.password)
                }
            }
            is LoginScreenAction.TogglePasswordVisibility -> {
                _uiState.update { currentState ->
                    currentState.copy(showPassword = !_uiState.value.showPassword)
                }
            }
            else -> Unit
        }
    }


}
