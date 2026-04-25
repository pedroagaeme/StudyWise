package com.example.studywise.ui.screens.quiz_details

data class QuizDetailsUiState(
    val quizId: String = "",
    val quizName: String = "",
    val collectionName: String = "-",
    val questionCount: Int = 0,
    val createdAtLabel: String = "-",
    val averageScore: Float? = null,
    val bestScore: Int? = null,
    val attempts: List<AttemptCardUiState> = emptyList(),
    val isLoading: Boolean = true,
) {
    val averageProgress: Float?
        get() = if (averageScore == null || questionCount <= 0) {
            null
        } else {
            (averageScore / questionCount.toFloat()).coerceIn(0f, 1f)
        }

    val bestProgress: Float?
        get() = if (bestScore == null || questionCount <= 0) {
            null
        } else {
            (bestScore.toFloat() / questionCount.toFloat()).coerceIn(0f, 1f)
        }

    fun attemptProgress(attempt: AttemptCardUiState): Float = if (questionCount <= 0) {
            0f
        } else {
            (attempt.answeredQuestions.toFloat() / questionCount.toFloat()).coerceIn(0f, 1f)
        }
}

data class AttemptCardUiState(
    val attemptId: String,
    val answeredQuestions: Int,
    val remainingQuestions: Int,
    val score: Int?,
    val timeLabel: String,
) {
    val hasRemainingQuestions: Boolean
        get() = remainingQuestions > 0

    val hasCompletedAttempt: Boolean
        get() = remainingQuestions == 0
}


