package com.example.studywise.ui.screens.signup

sealed interface SignUpScreenAction {
    data class OnEmailChange(val email: String) : SignUpScreenAction
    data class OnPasswordChange(val password: String) : SignUpScreenAction
    data object TogglePasswordVisibility : SignUpScreenAction
    data object OnCreateAccountClick : SignUpScreenAction
    data object OnBackToLoginClick : SignUpScreenAction
}
