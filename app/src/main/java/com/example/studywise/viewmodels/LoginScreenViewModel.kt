package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.repository.AuthRepository
import com.example.studywise.ui.screens.login.LoginScreenAction
import com.example.studywise.ui.screens.login.LoginScreenEvent
import com.example.studywise.ui.screens.login.LoginScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(LoginScreenUiState())
    val uiState: StateFlow<LoginScreenUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<LoginScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            if(authRepository.isSessionActive()) {
                eventChannel.send(LoginScreenEvent.LoginSuccess)
            }
        }
    }
    fun onAction(action: LoginScreenAction) {
        when (action) {
            is LoginScreenAction.OnLoginButtonClick -> {
                viewModelScope.launch {
                    try {
                        authRepository.login(
                            email = _uiState.value.email,
                            password = _uiState.value.password
                        )
                        eventChannel.send(LoginScreenEvent.LoginSuccess)
                    } catch (e: Exception) {
                        eventChannel.send(LoginScreenEvent.LoginFailure(e.message ?: "An error occurred"))
                    }
                }
            }
            is LoginScreenAction.OnSignUpButtonClick -> {
                viewModelScope.launch {
                    eventChannel.send(LoginScreenEvent.NavigateToSignUpScreen)
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