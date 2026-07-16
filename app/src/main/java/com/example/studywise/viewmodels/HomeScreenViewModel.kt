package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.screens.tabs.home.HomeScreenAction
import com.example.studywise.ui.screens.tabs.home.HomeScreenEffect
import com.example.studywise.ui.screens.tabs.home.HomeScreenUiState
import com.example.studywise.ui.screens.tabs.home.HomeStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: QuizRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    init {
        observeDatabase()
    }

    private fun observeDatabase() {
        combine(
            repository.getMostRecentQuizzes(3),
            repository.getCollectionsWithQuizzes()
        ) { recent, collections ->
            _uiState.update { currentState ->
                val step = when {
                    recent.isEmpty() && collections.isEmpty() -> HomeStep.EMPTY
                    else -> HomeStep.HAS_CONTENT
                }
                currentState.copy(
                    currentStep = step,
                    recentQuizzes = recent,
                    collections = collections,
                    // New collections are initially collapsed
                    collectionsExpandableState = collections.associate { it.id to false },
                    collectionsScrollOffsets = collections.associate { it.id to 0f }
                )
            }
        }.launchIn(viewModelScope)
    }
    fun effectConsumed() {
        _uiState.update {
            it.copy(pendingEffect = null)
        }
    }

    fun onAction(action: HomeScreenAction) {
        when(action) {
            is HomeScreenAction.OnToggleCollectionExpandableState -> {
                _uiState.update { currentState ->
                    val currentVal = currentState.collectionsExpandableState[action.collectionId] ?: false
                    val newMap = currentState.collectionsExpandableState + (action.collectionId to !currentVal)
                    currentState.copy(collectionsExpandableState = newMap)
                }
                if (_uiState.value.collectionsExpandableState[action.collectionId] == true) {
                    _uiState.update {
                        currentState ->
                        currentState.copy(
                            pendingEffect = HomeScreenEffect.ScrollBy(
                                _uiState.value.collectionsScrollOffsets[action.collectionId] ?: 0f
                            )
                        )
                    }
                }
            }
            is HomeScreenAction.OnScrollOffsetChanged -> {
                _uiState.update {
                    currentState ->
                    val newMap = currentState.collectionsScrollOffsets + (action.collectionId to action.offset)
                    currentState.copy(collectionsScrollOffsets = newMap)
                }
            }
            is HomeScreenAction.OnQuizCardClick -> {
                _uiState.update {
                    currentState ->
                    currentState.copy(
                        pendingEffect = HomeScreenEffect.NavigateToQuizDetails(action.quizId)
                    )
                }
            }
        }
    }
}
