package com.example.studywise.ui.screens.tabs.home

import androidx.compose.foundation.lazy.LazyListState
import com.example.studywise.data.QuizCollectionDto
import com.example.studywise.data.QuizDto

data class HomeScreenUiState (
    val recentQuizzes: List<QuizDto> = emptyList(),
    val collections: List<QuizCollectionDto> = emptyList(),
    val collectionsExpandableState: Map<String, Boolean> = emptyMap(),
    val collectionsScrollOffsets: Map<String, Float> = emptyMap(),
    val listState: LazyListState = LazyListState(),
    val pendingEffect: HomeScreenEffect? = null,
)
