package com.example.studywise.data.repository

import android.util.Log
import com.example.studywise.Appwrite
import com.example.studywise.data.AnswerOptionDto
import com.example.studywise.data.GenerateQuizResponse
import com.example.studywise.data.QuestionDto
import com.example.studywise.data.QuizAttemptDto
import com.example.studywise.data.QuizCollectionDto
import com.example.studywise.data.QuizDto
import com.example.studywise.data.UploadQuizAnswerOptionRequestData
import com.example.studywise.data.UploadQuizCollectionRequestData
import com.example.studywise.data.UploadQuizQuestionRequestData
import com.example.studywise.data.UploadQuizRequestData
import com.example.studywise.data.db.dao.QuizDao
import com.example.studywise.data.db.entity.AnswerOptionEntity
import com.example.studywise.data.db.entity.QuestionAttemptEntity
import com.example.studywise.data.db.entity.QuestionEntity
import com.example.studywise.data.db.entity.QuizAttemptEntity
import com.example.studywise.data.db.entity.QuizCollectionEntity
import com.example.studywise.data.db.entity.QuizEntity
import com.example.studywise.data.db.relation.CollectionWithQuizzes
import com.example.studywise.data.db.relation.QuizBasicInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import javax.inject.Inject

// Mappers
object QuizMappers {
    fun toQuizCollectionDto(collectionWithQuizzes: CollectionWithQuizzes): QuizCollectionDto {
        return QuizCollectionDto(
            id = collectionWithQuizzes.collection.id,
            name = collectionWithQuizzes.collection.name,
            quizzes = collectionWithQuizzes.quizzes.map { quizInfo ->
                toQuizDto(quizInfo = quizInfo)
            }
        )
    }

    fun toQuizDto(quizInfo: QuizBasicInfo): QuizDto {
        return QuizDto(
            id = quizInfo.quiz.id,
            title = quizInfo.quiz.title,
            lastInteracted = quizInfo.lastAttemptedAt ?: quizInfo.quiz.createdAt,
            questionCount = quizInfo.questionCount,
            averageScore = quizInfo.averageScore,
            collectionName = quizInfo.collectionName
        )
    }
    fun toQuestionDto(entity: QuestionEntity, answerOptions: List<AnswerOptionDto>): QuestionDto =
        QuestionDto(
            id = entity.id,
            description = entity.description,
            type = entity.type,
            explanation = entity.explanation,
            answerOptions = answerOptions
        )

    fun toAnswerOptionDto(entity: AnswerOptionEntity): AnswerOptionDto =
        AnswerOptionDto(
            id = entity.id,
            text = entity.text,
            isCorrect = entity.isCorrect,
        )
}

class QuizRepository @Inject constructor(
    private val quizDao: QuizDao
) {

    fun generateNewId() = Appwrite.generateNewId()

    // Fetch from local
    private suspend fun getQuestionsByQuizIdLocal(quizId: String): List<QuestionDto> {
        val quizWithQuestions = quizDao.getQuizWithQuestionsById(quizId) ?: return emptyList()

        return quizWithQuestions.questions.map { questionWithAnswers ->
            QuizMappers.toQuestionDto(
                entity = questionWithAnswers.question,
                answerOptions = questionWithAnswers.answers.map { QuizMappers.toAnswerOptionDto(it) }
            )
        }
    }

    // Fetch from remote, convert to local aggregate (entities)
    @Suppress("UNCHECKED_CAST")
    private suspend fun getQuestionsByQuizIdRemote(quizId: String): List<QuestionDto> {
        try {
            val remoteResult = Appwrite.getQuizDetails(quizId).data
            val questions =
                (remoteResult["questions"] as? List<Map<String, Any>>)?.mapNotNull { questionMap ->
                    QuestionDto(
                        id = questionMap["id"] as? String ?: return@mapNotNull null,
                        description = questionMap["description"] as? String
                            ?: return@mapNotNull null,
                        type = questionMap["type"] as? String ?: return@mapNotNull null,
                        explanation = questionMap["explanation"] as? String ?: "",
                        answerOptions = (questionMap["answerOptions"] as? List<Map<String, Any>>)?.mapNotNull { answerOptionMap ->
                            AnswerOptionDto(
                                id = answerOptionMap["id"] as? String ?: return@mapNotNull null,
                                text = answerOptionMap["text"] as? String ?: return@mapNotNull null,
                                isCorrect
                                = answerOptionMap["isCorrect"] as? Boolean ?: false,
                            )
                        } ?: emptyList()
                    )
                } ?: emptyList()
            return questions
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun getQuestionsByQuizId(quizId: String): List<QuestionDto> {
        val localResult = getQuestionsByQuizIdLocal(quizId)
        if (!localResult.isEmpty()) {
            return localResult
        }

        // If not found locally, fetch remote
        val remoteResult = getQuestionsByQuizIdRemote(quizId)
        if (!remoteResult.isEmpty()) {
            return remoteResult
        }
        return emptyList()
    }

    suspend fun generateQuiz(
        difficulty: String,
        size: String,
        quizSummary: String,
        files: List<File>,
        links: List<String>
    ) = Appwrite.generateQuiz(
        difficulty = difficulty,
        size = size,
        quizSummary = quizSummary,
        files = files,
        links = links
    )

    private suspend fun uploadQuizRemote(request: UploadQuizRequestData, quizId: String) =
        Appwrite.uploadQuiz(request, quizId)

    private suspend fun uploadQuizLocal(request: UploadQuizRequestData, quizId: String): String? {
        val now = Instant.now().toString()
        try {
            val quizCollectionId = Appwrite.generateNewId()
            return quizDao.insertQuizWithQuestionsAndAnswers(
                quizCollection = QuizCollectionEntity(
                    id = quizCollectionId,
                    name = request.quizCollection.name,
                    createdAt = now,
                    updatedAt = now
                ),
                quiz = QuizEntity(
                    id = quizId,
                    title = request.title,
                    quizCollectionId = quizCollectionId,
                    createdAt = now,
                    updatedAt = now
                ),
                questions = request.questions.map {
                    val questionId = Appwrite.generateNewId()
                    Pair(
                        QuestionEntity(
                            id = questionId,
                            description = it.description,
                            type = it.type,
                            explanation = it.explanation,
                            quizId = quizId,
                            createdAt = now,
                            updatedAt = now,
                        ),
                        it.answerOptions.map { answerOption ->
                            AnswerOptionEntity(
                                id = Appwrite.generateNewId(),
                                text = answerOption.text,
                                isCorrect = answerOption.isCorrect,
                                questionId = questionId,
                                createdAt = now,
                                updatedAt = now,
                            )
                        }
                    )
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun uploadQuiz(quizResponse: GenerateQuizResponse): String? {
        val quizId = generateNewId()
        val request = UploadQuizRequestData(
            title = quizResponse.quiz.title,
            quizCollection = UploadQuizCollectionRequestData(
                name = quizResponse.quiz.quizCollection.name
            ),
            questions = quizResponse.quiz.questions.map { question ->
                UploadQuizQuestionRequestData(
                    description = question.description,
                    type = question.type,
                    explanation = question.explanation,
                    answerOptions = question.answerOptions.map { answerOption ->
                        UploadQuizAnswerOptionRequestData(
                            text = answerOption.text,
                            isCorrect = answerOption.isCorrect
                        )
                    }
                )
            }
        )

        val remoteResult = uploadQuizRemote(request, quizId)
        val localResult = uploadQuizLocal(request, quizId)
        Log.d("QuizRepository", "Remote result: $remoteResult, Local result: $localResult")
        if (remoteResult != localResult) {
            return null
        }
        return remoteResult
    }

    fun getMostRecentQuizzes(limit: Int): Flow<List<QuizDto>> {
        return quizDao.getMostRecentQuizzes(limit).map { quizList ->
            quizList.map { quizInfo ->
                QuizMappers.toQuizDto(quizInfo)
            }
        }
    }

    fun getCollectionsWithQuizzes(): Flow<List<QuizCollectionDto>> {
        return quizDao.getCollectionsWithQuizzes().map { collectionList ->
            collectionList.map { collectionWithQuizzes ->
                QuizMappers.toQuizCollectionDto(collectionWithQuizzes)
            }
        }
    }

    suspend fun saveQuestionAttempt(
        questionId: String,
        selectedAnswerId: String,
        quizAttemptId: String,
        quizId: String
    ) {
        val now = Instant.now().toString()
        val questionAttempt = QuestionAttemptEntity(
            id = generateNewId(),
            questionId = questionId,
            selectedAnswerId = selectedAnswerId,
            quizAttemptId = quizAttemptId,
            createdAt = now,
            updatedAt = now,
        )
        val quizAttempt = QuizAttemptEntity(
            id = quizAttemptId,
            quizId = quizId,
            createdAt = now,
            updatedAt = now
        )
        quizDao.insertQuestionAttempt(
            questionAttempt = questionAttempt,
            quizAttempt = quizAttempt
        )
    }
}