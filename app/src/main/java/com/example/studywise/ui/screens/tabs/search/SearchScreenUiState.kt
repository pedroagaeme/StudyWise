package com.example.studywise.ui.screens.tabs.search

import com.example.studywise.data.QuizDto

data class SearchScreenUiState(
    val searchQuery: String = "",
    val filteredQuizzes: List<QuizDto> = emptyList(),
    val pendingEffect: SearchScreenEffect? = null,
)


