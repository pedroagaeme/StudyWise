package com.example.studywise.ui.screens.tabs.search

sealed interface SearchScreenAction {
    data class OnSearchQueryChange(val query: String) : SearchScreenAction
    data class OnQuizCardClick(val quizId: String) : SearchScreenAction
}

