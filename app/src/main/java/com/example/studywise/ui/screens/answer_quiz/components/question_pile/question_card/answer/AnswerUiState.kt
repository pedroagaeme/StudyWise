package com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer

data class AnswerUiState(
    val id: String = "",
    val text: String = "",
    val isCorrect: Boolean = false,
    val isSelected: Boolean = false
)
