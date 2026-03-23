package com.example.studywise.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateQuizRequest(
    val difficulty: String,
    val size: String,
    @SerialName("quiz_summary")
    val quizSummary: String? = null,
    val documents: GenerateQuizDocuments? = null
)

@Serializable
data class GenerateQuizDocuments(
    val ids: List<String>? = null,
    val links: List<String>? = null
)

@Serializable
data class GenerateQuizResponse(
    val quiz: QuizData
)

@Serializable
data class QuizData(
    val title: String,
    @SerialName("quiz_collection")
    val quizCollection: QuizCollectionData,
    val questions: List<QuizQuestionData>
)

@Serializable
data class QuizCollectionData(
    val title: String
)
@Serializable
data class QuizQuestionData(
    val type: String,
    val description: String,
    @SerialName("answer_options")
    val answerOptions: List<QuestionAnswerOptionData>,
    val explanation: String
)

@Serializable
data class QuestionAnswerOptionData(
    val text: String,
    val isCorrect: Boolean
)


