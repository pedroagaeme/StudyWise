package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.screens.review_attempt.ReviewAttemptUiState
import com.example.studywise.ui.screens.review_attempt.ReviewOptionUiState
import com.example.studywise.ui.screens.review_attempt.ReviewQuestionUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ReviewAttemptViewModel.Factory::class)
class ReviewAttemptViewModel @AssistedInject constructor(
    @Assisted("quizId") private val quizId: String,
    @Assisted("attemptId") private val attemptId: String,
    private val repository: QuizRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("quizId") quizId: String,
            @Assisted("attemptId") attemptId: String,
        ): ReviewAttemptViewModel
    }

    private val _uiState = MutableStateFlow(ReviewAttemptUiState())
    val uiState: StateFlow<ReviewAttemptUiState> = _uiState.asStateFlow()

    init {
        observeReviewAttempt()
    }

    @Suppress("unused")
    fun onScrollChanged(currentScroll: Int) {
        _uiState.update { currentState ->
            if (currentState.currentScroll == currentScroll) {
                currentState
            } else {
                currentState.copy(currentScroll = currentScroll)
            }
        }
    }

    fun onToggleOption(questionId: String, optionId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                questions = currentState.questions.map { question ->
                    if (question.id != questionId) {
                        question
                    } else {
                        question.copy(
                            options = question.options.map { option ->
                                if (option.id == optionId) {
                                    option.copy(isExpanded = !option.isExpanded)
                                } else {
                                    option
                                }
                            }
                        )
                    }
                }
            )
        }
    }

    private fun observeReviewAttempt() {
        viewModelScope.launch {
            val questionsById = repository.getQuestionsByQuizId(quizId).associateBy { it.id }

            combine(
                repository.getQuizById(quizId),
                repository.getQuizAttemptByIdFlow(attemptId)
            ) { quiz, attempt ->
                quiz to attempt
            }.collect { (quiz, attempt) ->
                val reviewQuestions = attempt?.questionAttempts
                    ?.sortedBy { it.sortOrder }
                    ?.mapIndexedNotNull { index, questionAttempt ->
                        val question = questionsById[questionAttempt.questionId] ?: return@mapIndexedNotNull null
                        ReviewQuestionUiState(
                            id = question.id,
                            number = index + 1,
                            description = question.description,
                            explanation = question.explanation,
                            options = question.answerOptions.mapIndexed { optionIndex, answerOption ->
                                ReviewOptionUiState(
                                    id = answerOption.id,
                                    label = "option ${(97 + optionIndex).toChar()}",
                                    text = answerOption.text,
                                    isCorrect = answerOption.isCorrect,
                                    isChosen = answerOption.id == questionAttempt.selectedAnswerId,
                                    isExpanded = answerOption.isCorrect || answerOption.id == questionAttempt.selectedAnswerId,
                                )
                            }
                        )
                    }
                    ?: emptyList()

                _uiState.update { currentState ->
                    currentState.copy(
                        quizName = quiz?.title ?: "Quiz",
                        questions = reviewQuestions,
                        isLoading = false,
                    )
                }
            }
        }
    }
}





