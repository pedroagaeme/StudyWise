package com.example.studywise.ui.screens.answer_quiz

sealed interface AnswerQuizUiEffect{
    data class FinishQuiz(val quizId: String, val attemptId: String) : AnswerQuizUiEffect
}

