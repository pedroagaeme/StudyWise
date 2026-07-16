package com.example.studywise.ui.screens.tabs.search

import com.example.studywise.data.QuizDto

enum class SearchStep {
    LOADING,
    HAS_CONTENT,
    EMPTY
}

data class SearchScreenUiState(
    val currentStep: SearchStep = SearchStep.HAS_CONTENT,
    val searchQuery: String = "",
    val filteredQuizzes: List<QuizDto> = emptyList(),
    val pendingEffect: SearchScreenEffect? = null,
)
