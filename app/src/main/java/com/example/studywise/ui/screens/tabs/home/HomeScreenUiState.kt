package com.example.studywise.ui.screens.tabs.home

import androidx.compose.foundation.lazy.LazyListState
import com.example.studywise.ui.components.model.Collection
import com.example.studywise.ui.components.model.Quiz
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class HomeScreenUiState (
    val recentQuizzes: List<Quiz> = emptyList(),
    val collections: List<Collection> = emptyList<Collection>(),
    val collectionsExpandableState: Map<String, Boolean> = emptyMap(),
    val collectionsScrollOffsets: Map<String, Float> = emptyMap(),
    val listState: LazyListState = LazyListState(),
    val pendingEffect: HomeScreenEffect? = null,
)
