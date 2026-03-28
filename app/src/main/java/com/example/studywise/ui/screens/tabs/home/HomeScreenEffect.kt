package com.example.studywise.ui.screens.tabs.home

sealed interface HomeScreenEffect {
    data class ScrollBy(val offset: Float) : HomeScreenEffect
    data class NavigateToAnswerQuiz(val quizId: String) : HomeScreenEffect
}
