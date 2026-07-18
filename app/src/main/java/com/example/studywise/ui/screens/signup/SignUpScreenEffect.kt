package com.example.studywise.ui.screens.signup

sealed interface SignUpScreenEffect {
    data object NavigateToLogin : SignUpScreenEffect
}
