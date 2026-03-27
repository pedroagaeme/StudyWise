package com.example.studywise.ui.screens.tabs.home

sealed interface HomeScreenEvent {
    data class ScrollBy(val offset: Float) : HomeScreenEvent
}