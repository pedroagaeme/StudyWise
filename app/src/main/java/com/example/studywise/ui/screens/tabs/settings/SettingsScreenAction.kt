package com.example.studywise.ui.screens.tabs.settings

import com.example.studywise.data.ThemeMode

sealed interface SettingsScreenAction {
    data class OnThemeModeChange(val themeMode: ThemeMode) : SettingsScreenAction
    data object OnLogoutClick : SettingsScreenAction
}
