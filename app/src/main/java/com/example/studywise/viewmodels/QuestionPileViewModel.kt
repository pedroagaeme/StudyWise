package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.components.quiz.question_pile.QuestionPileUiState
import com.example.studywise.ui.components.quiz.question_pile.question_card.answer.AnswerUiState
import com.example.studywise.ui.components.quiz.question_pile.question_card.QuestionCardUiState
import com.example.studywise.ui.components.quiz.question_pile.question_card.QuestionPileAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuestionPileViewModel (
    private val quizId: String,
    private val repository: QuizRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(QuestionPileUiState())
    val uiState: StateFlow<QuestionPileUiState> = _uiState.asStateFlow()

    init {
        loadQuestionPile()
    }

    private fun loadQuestionPile() {
        viewModelScope.launch {
            // 1. Fetch the data
            val quizWithQuestions = repository.getQuizDetails(quizId)

            // 2. Transform the database models into UI models
            val uiQuestionList = quizWithQuestions.questions.mapIndexed { index, questionWithAnswers ->
                QuestionCardUiState(
                    questionId = questionWithAnswers.question.id,
                    questionNumber = index,
                    description = questionWithAnswers.question.description,
                    // Map the nested answers list
                    answers = questionWithAnswers.answers.map { answerEntity ->
                        // Assuming you have an AnswerUiState or similar
                        AnswerUiState(
                            id = answerEntity.id,
                            text = answerEntity.text,
                            isCorrect = answerEntity.isCorrect
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

    fun onAction(action: QuestionPileAction) {
        when(action) {
            is QuestionPileAction.AnswerSelected -> {
                val tempQuestionList = _uiState.value.questionList.toMutableList()
                tempQuestionList[action.questionIndex] = tempQuestionList[action.questionIndex].copy(selectedAnswer = action.answer)
                _uiState.update { it.copy(questionList = tempQuestionList) }
            }
            is QuestionPileAction.FlipToggled -> {
                val tempQuestionList = _uiState.value.questionList.toMutableList()
                tempQuestionList[action.questionIndex] = tempQuestionList[action.questionIndex].copy(isFlipped = !tempQuestionList[action.questionIndex].isFlipped)
                _uiState.update { it.copy(questionList = tempQuestionList) }
            }
            is QuestionPileAction.NextQuestionCardRequested -> {
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
                QuestionPileViewModel(
                    quizId = quizId,
                    repository = repository
                )
            }
        }
    }
}