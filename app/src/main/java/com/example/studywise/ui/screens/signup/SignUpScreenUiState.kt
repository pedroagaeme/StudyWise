package com.example.studywise.ui.screens.signup

enum class SignUpStep {
    FORM,
    LOADING,
    CHECK_EMAIL
}

data class SignUpScreenUiState(
    val currentStep: SignUpStep = SignUpStep.FORM,
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val errorMessage: String? = null,
    val pendingEffect: SignUpScreenEffect? = null
)
