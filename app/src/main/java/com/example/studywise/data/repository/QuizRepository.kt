package com.example.studywise.data.repository

import com.example.studywise.Appwrite
import com.example.studywise.data.QuestionAnswerOptionData
import com.example.studywise.data.QuizCollectionData
import com.example.studywise.data.QuizData
import com.example.studywise.data.QuizQuestionData
import com.example.studywise.data.db.dao.QuizDao
import com.example.studywise.data.db.entity.AnswerOptionEntity
import com.example.studywise.data.db.entity.QuestionEntity
import com.example.studywise.data.db.entity.QuizCollectionEntity
import com.example.studywise.data.db.entity.QuizEntity
import com.example.studywise.data.db.relation.CollectionWithQuizzes
import com.example.studywise.data.db.relation.QuizBasicInfo
import com.example.studywise.ui.components.model.Collection
import com.example.studywise.ui.components.model.Quiz
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject


class QuizRepository @Inject constructor(
    private val quizDao: QuizDao
) {

    private suspend fun getQuizDetailsLocal(quizId: String): QuizData? {
        val localResult = quizDao.getQuizWithQuestionsById(quizId) ?: return null
        val collectionEntity = quizDao.getQuizCollectionByQuizId(quizId)?: return null
        return QuizData(
            title = localResult.quiz.title,
            quizCollection = QuizCollectionData(
                name = collectionEntity.name
            ),
            questions = localResult.questions.map { questionWithAnswers ->
                QuizQuestionData(
                    type = questionWithAnswers.question.type,
                    description = questionWithAnswers.question.description,
                    answerOptions = questionWithAnswers.answers.map { answer ->
                        QuestionAnswerOptionData(
                            text = answer.text,
                            isCorrect = answer.isCorrect
                        )
                    },
                    explanation = questionWithAnswers.question.explanation
                )
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun getQuizDetailsRemote(quizId: String): QuizData? {
        try {
            val remoteResult = Appwrite.getQuizDetails(quizId).data

            // Deserialize map to QuizData
            return remoteResult.let {
                QuizData(
                    title = it["title"] as? String ?: return null,
                    quizCollection = QuizCollectionData(
                        name = (it["quizCollection"] as? Map<*, *>)?.get("name") as? String ?: return null
                    ),
                    questions = (it["questions"] as? List<Map<String, Any>>)?.mapNotNull { questionMap ->
                        QuizQuestionData(
                            type = questionMap["type"] as? String ?: return@mapNotNull null,
                            description = questionMap["description"] as? String ?: return@mapNotNull null,
                            answerOptions = (questionMap["answerOptions"] as? List<Map<String, Any>>)?.mapNotNull { answerMap ->
                                QuestionAnswerOptionData(
                                    text = answerMap["text"] as? String ?: return@mapNotNull null,
                                    isCorrect = answerMap["isCorrect"] as? Boolean ?: false
                                )
                            } ?: emptyList(),
                            explanation = questionMap["explanation"] as? String ?: ""
                        )
                    } ?: emptyList()
                )
            }
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun getQuizDetails(quizId: String): QuizData? {
        val localQuiz = getQuizDetailsLocal(quizId)
        if (localQuiz != null) return localQuiz

        val remoteQuiz = getQuizDetailsRemote(quizId)
        if (remoteQuiz != null) {
            uploadQuizLocal(remoteQuiz)
        }
        return remoteQuiz
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

    private suspend fun uploadQuizLocal(quiz: QuizData): String? {
        val quizCollectionEntity = quizCollectionDataToEntity(quiz.quizCollection)
        val quizEntity = quizDataToEntity(quiz, quizCollectionEntity.id)

        try {
            quizDao.insertQuizWithQuestionsAndAnswers(
                quizCollection = quizCollectionEntity,
                quiz = quizEntity,
                questions = quiz.questions.map { questionData ->
                    val questionEntity = questionDataToEntity(questionData, quizEntity.id)
                    val answers = questionData.answerOptions.map { answerData ->
                        answerOptionDataToEntity(answerData, questionEntity.id)
                    }
                    Pair(questionEntity, answers)
                }
            )

            return quizEntity.id
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun uploadQuizRemote(quiz: QuizData): String? {
        return try {
            Appwrite.uploadQuiz(quiz)
        } catch (e: Exception) {
            null
        }
    }

    fun getMostRecentQuizzes(limit: Int): Flow<List<Quiz>> {
        return quizDao.getMostRecentQuizzes(limit).map { quizList ->
            quizList.map { quizInfo ->
                quizInfoToQuiz(quizInfo)
            }
        }
    }

    fun getCollectionsWithQuizzes(): Flow<List<Collection>>{
        return quizDao.getCollectionsWithQuizzes().map { collectionList ->
            collectionList.map { collectionWithQuizzes ->
                collectionWithQuizzesToCollection(collectionWithQuizzes)
            }
        }
    }

    fun collectionWithQuizzesToCollection(collectionWithQuizzes: CollectionWithQuizzes): Collection {
        return Collection(
            id = collectionWithQuizzes.collection.id,
            name = collectionWithQuizzes.collection.name,
            quizzes = collectionWithQuizzes.quizzes.map { quizInfo ->
                quizInfoToQuiz(quizInfo = quizInfo)
            }
        )
    }

    fun quizInfoToQuiz(quizInfo: QuizBasicInfo): Quiz {
        return Quiz(
            id = quizInfo.quiz.id,
            title = quizInfo.quiz.title,
            lastInteracted = quizInfo.lastAttemptedAt ?: quizInfo.quiz.createdAt,
            questionCount = quizInfo.questionCount,
            averageScore = quizInfo.averageScore,
            collectionName = quizInfo.collectionName
        )
    }

    suspend fun uploadQuiz(quiz: QuizData): String? {
        val localResultId = uploadQuizLocal(quiz)
        val remoteResultId = uploadQuizRemote(quiz)
        return remoteResultId ?: localResultId
    }

    private suspend fun quizCollectionDataToEntity(
        data: QuizCollectionData
    ): QuizCollectionEntity {
        val quizCollectionId = UUID.randomUUID().toString()
        val creationDate = Instant.now().toString()
        return QuizCollectionEntity(
            id = quizCollectionId,
            name = data.name,
            createdAt = creationDate,
            updatedAt = creationDate
        )
    }
    private suspend fun quizDataToEntity(data: QuizData, collectionId: String): QuizEntity {
        val quizId = UUID.randomUUID().toString()
        val creationDate = Instant.now().toString()
        return QuizEntity(
            id = quizId,
            title = data.title,
            quizCollectionId = collectionId,
            createdAt = creationDate,
            updatedAt = creationDate
        )
    }

    private fun questionDataToEntity(data: QuizQuestionData, quizId: String): QuestionEntity {
        return QuestionEntity(
            id = UUID.randomUUID().toString(),
            description = data.description,
            type = data.type,
            explanation = data.explanation,
            quizId = quizId,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
    }

    private fun answerOptionDataToEntity(data: QuestionAnswerOptionData, questionId: String):
            AnswerOptionEntity {
        return AnswerOptionEntity(
            id = UUID.randomUUID().toString(),
            text = data.text,
            isCorrect = data.isCorrect,
            questionId = questionId,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
    }
}