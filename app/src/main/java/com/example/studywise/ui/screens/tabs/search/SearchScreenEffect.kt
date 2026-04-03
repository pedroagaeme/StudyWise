package com.example.studywise.ui.screens.tabs.search

sealed interface SearchScreenEffect {
    data class NavigateToAnswerQuiz(val quizId: String) : SearchScreenEffect
}

