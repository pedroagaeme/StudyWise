package com.example.studywise.ui.screens.tabs.home

import com.example.studywise.ui.components.model.Collection
import com.example.studywise.ui.components.model.Quiz

data class HomeScreenUiState (
    val recentQuizzes: List<Quiz> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val collectionsExpandableState: List<Boolean> = emptyList()
)
