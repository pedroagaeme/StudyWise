package com.example.studywise.ui.screens.tabs.search

sealed interface SearchScreenEffect {
    data class NavigateToQuizDetails(val quizId: String) : SearchScreenEffect
}

