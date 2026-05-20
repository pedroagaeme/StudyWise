package com.example.studywise.ui.screens.answer_quiz
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.QuestionCardUiState

data class AnswerQuizScreenUiState (
    val quizName: String = "Quiz",
    val questionList: List<QuestionCardUiState> = listOf(),
    val currentAttemptId: String? = null,
    val targetIndex: Int = 0,
    val currentScroll: Int = 0,
    val pendingEffect: AnswerQuizUiEffect? = null,
)