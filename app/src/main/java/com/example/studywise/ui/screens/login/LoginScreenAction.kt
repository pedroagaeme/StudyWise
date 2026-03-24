package com.example.studywise.ui.screens.login

sealed interface LoginScreenAction {
    data object TogglePasswordVisibility : LoginScreenAction
    data class OnEmailChange(val email: String) : LoginScreenAction
    data class OnPasswordChange(val password: String) : LoginScreenAction
    data object OnLoginButtonClick : LoginScreenAction
    data object OnSignUpButtonClick : LoginScreenAction
    data object OnForgotPasswordButtonClick : LoginScreenAction
}
