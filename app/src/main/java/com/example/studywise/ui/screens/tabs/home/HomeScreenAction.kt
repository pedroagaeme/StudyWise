package com.example.studywise.ui.screens.tabs.home

sealed interface HomeScreenAction {
    data class ToggleCollectionExpandableState(val collectionIndex: Int) : HomeScreenAction
}
