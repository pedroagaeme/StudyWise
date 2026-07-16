package com.example.studywise.ui.screens.login

enum class LoginStep {
    LOADING,
    NOT_LOADING
}

data class LoginScreenUiState (
    val currentStep: LoginStep = LoginStep.LOADING,
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val pendingEffect: LoginScreenEffect? = null
)
