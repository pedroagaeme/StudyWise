package com.example.studywise.ui.screens.tabs.settings

sealed interface SettingsScreenEffect {
    data object NavigateToLogin : SettingsScreenEffect
}
