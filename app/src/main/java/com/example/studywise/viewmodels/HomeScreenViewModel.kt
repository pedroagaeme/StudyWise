package com.example.studywise.viewmodels

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.components.model.Collection
import com.example.studywise.ui.components.model.Quiz
import com.example.studywise.ui.screens.tabs.home.HomeScreenAction
import com.example.studywise.ui.screens.tabs.home.HomeScreenEffect
import com.example.studywise.ui.screens.tabs.home.HomeScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: QuizRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val recentQuizzes = repository.getMostRecentQuizzes(3)
            val collections = repository.getCollectionsWithQuizzes()
            _uiState.value = HomeScreenUiState(
                recentQuizzes = recentQuizzes,
                collections = collections,
                collectionsExpandableState = collections.map { it ->
                    false
                },
                collectionsScrollOffsets = collections.map {
                    0f
                }
            )
        }
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
                    currentState.copy(
                        collectionsExpandableState = currentState.collectionsExpandableState.mapIndexed { index, isExpanded ->
                            if (index == action.collectionIndex) !isExpanded else isExpanded
                        },
                    )
                }
                if (_uiState.value.collectionsExpandableState[action.collectionIndex]) {
                    _uiState.update {
                        currentState ->
                        currentState.copy(
                            pendingEffect = HomeScreenEffect.ScrollBy(
                                _uiState.value.collectionsScrollOffsets[action.collectionIndex]
                            )
                        )
                    }
                }
            }
            is HomeScreenAction.OnScrollOffsetChanged -> {
                _uiState.update {
                    currentState ->
                    currentState.copy(
                        collectionsScrollOffsets = currentState.collectionsScrollOffsets.mapIndexed {
                            index, offset -> if(index == action.collectionIndex) action.offset else offset
                        }
                    )
                }
            }
        }
    }
}