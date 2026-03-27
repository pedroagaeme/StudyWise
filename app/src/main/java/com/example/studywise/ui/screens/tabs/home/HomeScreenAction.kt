package com.example.studywise.ui.screens.tabs.home

sealed interface HomeScreenAction {
    data class OnToggleCollectionExpandableState(val collectionIndex: Int) : HomeScreenAction
    data class OnScrollOffsetChanged(val collectionIndex: Int, val offset: Float) : HomeScreenAction
}
