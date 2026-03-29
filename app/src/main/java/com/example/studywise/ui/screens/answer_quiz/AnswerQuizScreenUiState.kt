package com.example.studywise.ui.screens.answer_quiz

import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.QuestionCardUiState

data class AnswerQuizScreenUiState (
    val questionList: List<QuestionCardUiState> = listOf(),
    val currentAttempt: List<Boolean> = listOf(),
    val targetIndex: Int = 0,
    val pendingEffect: AnswerQuizUiEffect? = null,
)