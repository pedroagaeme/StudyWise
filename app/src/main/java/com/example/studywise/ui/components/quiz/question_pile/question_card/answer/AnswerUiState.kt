package com.example.studywise.ui.components.quiz.question_pile.question_card.answer

data class AnswerUiState(
    val id: String,
    val text: String,
    val isCorrect: Boolean = false,
    val isSelected: Boolean = false
)
