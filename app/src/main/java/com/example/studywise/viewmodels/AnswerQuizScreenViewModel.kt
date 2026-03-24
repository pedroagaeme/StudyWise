package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizScreenUiState
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer.AnswerUiState
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.QuestionCardUiState
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizScreenAction
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel(assistedFactory = AnswerQuizScreenViewModel.Factory::class)
class AnswerQuizScreenViewModel @AssistedInject constructor(
    @Assisted private val quizId: String,
    private val repository: QuizRepository,
): ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(quizId: String): AnswerQuizScreenViewModel
    }
    private val _uiState = MutableStateFlow(AnswerQuizScreenUiState())
    val uiState: StateFlow<AnswerQuizScreenUiState> = _uiState.asStateFlow()

    init {
        loadQuestionPile()
    }

    private fun loadQuestionPile() {
        viewModelScope.launch {
            // 1. Fetch the data
            val quizWithQuestions = repository.getQuizDetails(quizId)

            // 2. Transform the database models into UI models
           quizWithQuestions?: return@launch
            val uiQuestionList = quizWithQuestions.questions.mapIndexed { index, questionWithAnswers ->
                QuestionCardUiState(
                    questionNumber = index,
                    description = questionWithAnswers.description,
                    // Map the nested answers list
                    answers = questionWithAnswers.answerOptions.map { answerData ->
                        AnswerUiState(
                            text = answerData.text,
                            isCorrect = answerData.isCorrect
                        )
                    }
                )
            }

            // 3. Update the UI state
            _uiState.update { currentState ->
                currentState.copy(
                    questionList = uiQuestionList,
                    targetIndex = 0 // Reset or set initial index
                )
            }
        }
    }

    fun onAction(action: AnswerQuizScreenAction) {
        when(action) {
            is AnswerQuizScreenAction.AnswerSelected -> {
                val tempQuestionList = _uiState.value.questionList.toMutableList()
                tempQuestionList[action.questionIndex] = tempQuestionList[action.questionIndex].copy(selectedAnswer = action.answer)
                _uiState.update { it.copy(questionList = tempQuestionList) }
            }
            is AnswerQuizScreenAction.FlipToggled -> {
                val tempQuestionList = _uiState.value.questionList.toMutableList()
                tempQuestionList[action.questionIndex] = tempQuestionList[action.questionIndex].copy(isFlipped = !tempQuestionList[action.questionIndex].isFlipped)
                _uiState.update { it.copy(questionList = tempQuestionList) }
            }
            is AnswerQuizScreenAction.NextQuestionCardRequested -> {
                _uiState.update { it.copy(targetIndex = action.questionIndex + 1) }
            }
        }
    }

    companion object {
        fun factory(
            quizId: String,
            repository: QuizRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AnswerQuizScreenViewModel(
                    quizId = quizId,
                    repository = repository
                )
            }
        }
    }
}