package com.example.studywise.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateQuizRequest(
    @SerialName("quiz_response_file_name")
    val quizResponseFileName: String,
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
    val quiz: GenerateQuizResponseData
)

@Serializable
data class GenerateQuizResponseData(
    val title: String,
    @SerialName("quiz_collection")
    val quizCollection: GenerateQuizCollectionResponseData,
    val questions: List<GenerateQuizQuestionResponseData>
)

@Serializable
data class GenerateQuizCollectionResponseData(
    val name: String
)
@Serializable
data class GenerateQuizQuestionResponseData(
    val type: String,
    val description: String,
    @SerialName("answer_options")
    val answerOptions: List<GenerateQuizAnswerOptionResponseData>,
    val explanation: String
)

@Serializable
data class GenerateQuizAnswerOptionResponseData(
    val text: String,
    @SerialName("is_correct")
    val isCorrect: Boolean
)

@Serializable
data class UploadQuizRequestData (
    val title: String,
    val quizCollection: UploadQuizCollectionRequestData,
    val questions: List<UploadQuizQuestionRequestData>
)

@Serializable
data class UploadQuizCollectionRequestData (
    val name: String
)

@Serializable
data class UploadQuizQuestionRequestData (
    val description: String,
    val type: String,
    val explanation: String,
    val answerOptions: List<UploadQuizAnswerOptionRequestData>
)

@Serializable
data class UploadQuizAnswerOptionRequestData (
    val text: String,
    val isCorrect: Boolean
)