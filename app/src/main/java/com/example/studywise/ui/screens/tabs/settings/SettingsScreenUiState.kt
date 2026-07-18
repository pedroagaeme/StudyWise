package com.example.studywise.ui.screens.tabs.settings

import com.example.studywise.data.ThemeMode

data class SettingsScreenUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoggingOut: Boolean = false,
    val pendingEffect: SettingsScreenEffect? = null
)
