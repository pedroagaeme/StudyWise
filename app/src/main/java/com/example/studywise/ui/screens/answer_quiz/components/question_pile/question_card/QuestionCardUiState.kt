package com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card

import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer.AnswerUiState

data class QuestionCardUiState(
    val id: String,
    val description: String = "",
    val answers: List<AnswerUiState> = emptyList(),
    val selectedAnswer: AnswerUiState? = null,
    val isFlipped: Boolean = false,
    val isLoading: Boolean = true,
)