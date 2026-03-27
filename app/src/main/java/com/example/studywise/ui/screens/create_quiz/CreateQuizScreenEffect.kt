package com.example.studywise.ui.screens.create_quiz


sealed interface CreateQuizScreenEffect {
    data object OpenFilePicker : CreateQuizScreenEffect
    data object Dismiss: CreateQuizScreenEffect

    data class QuizGenerated(val quizId: String) : CreateQuizScreenEffect
}
