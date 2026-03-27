package com.example.studywise.ui.screens.login

data class LoginScreenUiState (
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val pendingEffect: LoginScreenEffect? = null
)
