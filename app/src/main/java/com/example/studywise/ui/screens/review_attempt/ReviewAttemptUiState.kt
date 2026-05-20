package com.example.studywise.ui.screens.review_attempt

data class ReviewAttemptUiState(
    val quizName: String = "Quiz",
    val isLoading: Boolean = true,
    val currentScroll: Int = 0,
    val questions: List<ReviewQuestionUiState> = emptyList(),
)

data class ReviewQuestionUiState(
    val id: String,
    val number: Int,
    val description: String,
    val explanation: String,
    val options: List<ReviewOptionUiState>,
)

data class ReviewOptionUiState(
    val id: String,
    val label: String,
    val text: String,
    val isCorrect: Boolean,
    val isChosen: Boolean,
    val isExpanded: Boolean = false,
)

