package com.example.studywise.ui.screens.answer_quiz

import com.example.studywise.data.QuizAttemptDto
import com.example.studywise.data.db.entity.QuizAttemptEntity
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.QuestionCardUiState

data class AnswerQuizScreenUiState (
    val questionList: List<QuestionCardUiState> = listOf(),
    val currentAttemptId: String? = null,
    val targetIndex: Int = 0,
    val pendingEffect: AnswerQuizUiEffect? = null,
)