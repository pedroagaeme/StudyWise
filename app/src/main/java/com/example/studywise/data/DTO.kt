package com.example.studywise.data

data class QuizCollectionDto(
    val id: String,
    val name: String,
    val quizzes: List<QuizDto> = emptyList()
)

data class QuizDto(
    val id: String,
    val title: String,
    val questionCount: Int,
    val collectionName: String? = null,
    val averageScore: Float? = null,
    val lastInteracted: String
)

data class QuestionDto(
    val id: String,
    val description: String,
    val type: String,
    val explanation: String,
    val answerOptions: List<AnswerOptionDto>
)

data class AnswerOptionDto(
    val id: String,
    val text: String,
    val isCorrect: Boolean,
)

data class QuizAttemptDto(
    val id: String,
    val quizId: String,
    val score: Int?,
    val createdAt: String,
    val questionAttempts: List<QuestionAttemptDto>
)

data class QuestionAttemptDto(
    val id: String,
    val questionId: String,
    val selectedAnswerId: String?,
    val sortOrder: Int
)

