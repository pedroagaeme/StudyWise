package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.components.model.Collection
import com.example.studywise.ui.components.model.Quiz
import com.example.studywise.ui.screens.tabs.home.HomeScreenAction
import com.example.studywise.ui.screens.tabs.home.HomeScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: QuizRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    val recentQuizzes = listOf(
        Quiz("1", "Organic Chemistry", "2m ago", "University", questionCount = 18, averageScore = 14f),
        Quiz("2", "Spanish Vocabulary - Unit 5", "47m ago", "Languages", questionCount = 25, averageScore = 20f),
        Quiz("7", "System Design Fundamentals", "3h ago", "Career Prep", questionCount = 12, averageScore = 8f)
    )
    val collections = listOf(
        com.example.studywise.ui.components.model.Collection(
            "c1", "University", listOf(
                Quiz("3", "Advanced Math", "Yesterday", questionCount = 30, averageScore = 24f),
                Quiz("4", "Quantum Physics", "3 days ago", questionCount = 16, averageScore = 11f),
                Quiz(
                    "8",
                    "Cell Biology Lab Prep",
                    "5 days ago",
                    questionCount = 10,
                    averageScore = 9f
                )
            )
        ),
        Collection(
            "c2", "Personal", listOf(
                Quiz("5", "World Geography", "Last week", questionCount = 22, averageScore = 15f),
                Quiz("6", "Cooking Basics", "2 weeks ago", questionCount = 14, averageScore = 6f),
                Quiz("9", "Chess Openings", "Mar 12", questionCount = 9, averageScore = 7f)
            )
        )
    )

    init {
        _uiState.value = HomeScreenUiState(
            recentQuizzes = recentQuizzes,
            collections = collections,
            collectionsExpandableState = collections.map {
                it -> false
            }
        )
    }

    fun onAction(action: HomeScreenAction) {
        when(action) {
            is HomeScreenAction.ToggleCollectionExpandableState -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        collectionsExpandableState = currentState.collectionsExpandableState.mapIndexed { index, isExpanded ->
                            if (index == action.collectionIndex) !isExpanded else isExpanded
                        }
                    )
                }
            }
        }
    }


}