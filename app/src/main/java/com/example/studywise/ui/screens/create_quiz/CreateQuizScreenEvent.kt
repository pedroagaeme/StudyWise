package com.example.studywise.ui.screens.create_quiz


sealed interface CreateQuizScreenEvent {
    data object OpenFilePicker : CreateQuizScreenEvent
    data object Dismiss: CreateQuizScreenEvent

    data class QuizGenerated(val quizId: String) : CreateQuizScreenEvent
}
