package com.example.studywise.ui.screens.tabs.home

sealed interface HomeScreenEffect {
    data class ScrollBy(val offset: Float) : HomeScreenEffect
    data class NavigateToQuizDetails(val quizId: String) : HomeScreenEffect
}
