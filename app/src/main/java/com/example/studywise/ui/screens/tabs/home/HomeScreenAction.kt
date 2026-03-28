package com.example.studywise.ui.screens.tabs.home

sealed interface HomeScreenAction {
    data class OnToggleCollectionExpandableState(val collectionId: String) : HomeScreenAction
    data class OnScrollOffsetChanged(val collectionId: String, val offset: Float) : HomeScreenAction
    data class OnQuizCardClick(val quizId: String) : HomeScreenAction
}
