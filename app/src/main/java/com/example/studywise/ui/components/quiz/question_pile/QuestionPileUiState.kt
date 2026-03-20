package com.example.studywise.ui.components.quiz.question_pile

import com.example.studywise.ui.components.quiz.question_pile.question_card.QuestionCardUiState


data class QuestionPileUiState (
    val questionList: List<QuestionCardUiState> = listOf(),
    val targetIndex: Int = 0,
)