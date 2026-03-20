package com.example.studywise.ui.components.quiz.question_pile.question_card

import com.example.studywise.ui.components.quiz.question_pile.question_card.answer.AnswerUiState

data class QuestionCardUiState(
    val questionId: String = "",
    val questionNumber: Int = 0,
    val description: String = "",
    val answers: List<AnswerUiState> = emptyList(),
    val selectedAnswer: AnswerUiState? = null,
    val isFlipped: Boolean = false,
    val isLoading: Boolean = true,
)

val QuestionCardUiState.isSelectedCorrect: Boolean
    get() = selectedAnswer?.isCorrect == true