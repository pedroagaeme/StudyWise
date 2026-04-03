package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.screens.tabs.search.SearchScreenAction
import com.example.studywise.ui.screens.tabs.search.SearchScreenEffect
import com.example.studywise.ui.screens.tabs.search.SearchScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchScreenUiState())
    val uiState: StateFlow<SearchScreenUiState> = _uiState.asStateFlow()

    init {
        observeDatabase()
    }

    private fun observeDatabase() {
        _uiState
            .map { it.searchQuery.trim() }
            .distinctUntilChanged()
            .flatMapLatest { query ->
                repository.getFilteredQuizzes(query)
            }
            .onEach { quizzes ->
                _uiState.update { it.copy(filteredQuizzes = quizzes) }
            }
            .launchIn(viewModelScope)
    }

    fun effectConsumed() {
        _uiState.update {
            it.copy(pendingEffect = null)
        }
    }

    fun onAction(action: SearchScreenAction) {
        when (action) {
            is SearchScreenAction.OnSearchQueryChange -> {
                _uiState.update {
                    it.copy(
                        searchQuery = action.query,
                    )
                }
            }

            is SearchScreenAction.OnQuizCardClick -> {
                _uiState.update {
                    it.copy(
                        pendingEffect = SearchScreenEffect.NavigateToAnswerQuiz(action.quizId)
                    )
                }
            }
        }
    }
}



