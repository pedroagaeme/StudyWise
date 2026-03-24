package com.example.studywise.ui.screens.login

sealed interface LoginScreenEvent {
    data object NavigateToSignUpScreen : LoginScreenEvent
    data object LoginSuccess : LoginScreenEvent
    data class LoginFailure(val message: String) : LoginScreenEvent
}
