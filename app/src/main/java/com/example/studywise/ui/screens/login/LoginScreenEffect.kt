package com.example.studywise.ui.screens.login

sealed interface LoginScreenEffect {
    data object NavigateToSignUpScreen : LoginScreenEffect
    data object LoginSuccess : LoginScreenEffect
    data class LoginFailure(val message: String) : LoginScreenEffect
}
