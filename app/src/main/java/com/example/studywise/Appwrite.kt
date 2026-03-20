package com.example.studywise

import android.content.Context
import com.example.studywise.constants.*
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.models.*
import io.appwrite.services.*

object Appwrite {
    data class CreateAnswerOptionInput(
        val text: String,
        val isCorrect: Boolean
    )

    data class CreateQuestionInput(
        val description: String,
        val answerOptions: List<CreateAnswerOptionInput>
    )

    lateinit var client: Client
    lateinit var account: Account
    lateinit var databases: Databases

    fun init(context: Context) {
        client = Client(context)
            .setEndpoint(APPWRITE_PUBLIC_ENDPOINT)
            .setProject(APPWRITE_PROJECT_ID)

        account = Account(client)
        databases = Databases(client)
    }

    // --- Authentication ---

    suspend fun onLogin(
        email: String,
        password: String,
    ): Session {
        return account.createEmailPasswordSession(email, password)
    }

    suspend fun onRegister(
        email: String,
        password: String,
    ): User<Map<String, Any>> {
        return account.create(
            userId = ID.unique(),
            email = email,
            password = password,
        )
    }

    suspend fun onLogout() {
        account.deleteSession("current")
    }

    suspend fun getCurrentUser(): User<Map<String, Any>> {
        return account.get()
    }

    suspend fun sendVerification(url: String): Token {
        return account.createVerification(url)
    }

    suspend fun confirmVerification(userId: String, secret: String): Token {
        return account.updateVerification(userId, secret)
    }

    // --- Quiz Operations ---

    suspend fun getQuizWithDetails(quizId: String): Document<Map<String, Any>> {
        return databases.getDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_TABLE_ID,
            documentId = quizId,
            queries = listOf(
                Query.select(listOf("*", "questions.*", "questions.answer_options.*"))
            )
        )
    }

    suspend fun getQuizzes(
        limit: Int = 25,
        offset: Int = 0,
        collectionId: String? = null
    ): DocumentList<Map<String, Any>> {
        val queries = mutableListOf(
            Query.limit(limit),
            Query.offset(offset),
            Query.orderDesc("\$updatedAt")
        )
        
        collectionId?.let {
            queries.add(Query.equal(APPWRITE_FIELD_QUIZ_COLLECTION_ID, it))
        }

        return databases.listDocuments(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_TABLE_ID,
            queries = queries
        )
    }

    suspend fun createQuiz(
        userId: String,
        title: String,
        collectionId: String? = null,
        questions: List<CreateQuestionInput> = emptyList()
    ): Document<Map<String, Any>> {
        val data = mutableMapOf<String, Any?>(
            APPWRITE_FIELD_TITLE to title,
            APPWRITE_FIELD_QUIZ_COLLECTION_ID to collectionId
        )

        val ownerPermissions = listOf(
            Permission.read(Role.user(userId)),
            Permission.update(Role.user(userId)),
            Permission.delete(Role.user(userId)),
        )

        val quizDocument = databases.createDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_TABLE_ID,
            documentId = ID.unique(),
            data = data,
            permissions = ownerPermissions
        )

        // Build related documents first, then connect them through parent-side relation fields.
        val createdQuestionIds = mutableListOf<String>()

        questions.forEach { question ->
            val questionDocument = databases.createDocument(
                databaseId = APPWRITE_DATABASE_ID,
                collectionId = APPWRITE_QUESTION_TABLE_ID,
                documentId = ID.unique(),
                data = mapOf(
                    APPWRITE_FIELD_DESCRIPTION to question.description
                ),
                permissions = ownerPermissions
            )

            val createdAnswerOptionIds = mutableListOf<String>()

            question.answerOptions.forEach { answerOption ->
                val answerOptionDocument = databases.createDocument(
                    databaseId = APPWRITE_DATABASE_ID,
                    collectionId = APPWRITE_ANSWER_TABLE_ID,
                    documentId = ID.unique(),
                    data = mapOf(
                        APPWRITE_FIELD_TEXT to answerOption.text,
                        APPWRITE_FIELD_IS_CORRECT to answerOption.isCorrect
                    ),
                    permissions = ownerPermissions
                )

                createdAnswerOptionIds.add(answerOptionDocument.id)
            }

            if (createdAnswerOptionIds.isNotEmpty()) {
                databases.updateDocument(
                    databaseId = APPWRITE_DATABASE_ID,
                    collectionId = APPWRITE_QUESTION_TABLE_ID,
                    documentId = questionDocument.id,
                    data = mapOf(APPWRITE_FIELD_ANSWER_OPTIONS to createdAnswerOptionIds)
                )
            }

            createdQuestionIds.add(questionDocument.id)
        }

        if (createdQuestionIds.isEmpty()) {
            return quizDocument
        }

        return databases.updateDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_TABLE_ID,
            documentId = quizDocument.id,
            data = mapOf(APPWRITE_FIELD_QUESTIONS to createdQuestionIds)
        )
    }

    suspend fun deleteQuiz(quizId: String) {
        databases.deleteDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_TABLE_ID,
            documentId = quizId
        )
    }

    // --- Attempts ---

    suspend fun getQuizAttempts(quizId: String): DocumentList<Map<String, Any>> {
        return databases.listDocuments(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_ATTEMPT_TABLE_ID,
            queries = listOf(
                Query.equal(APPWRITE_FIELD_QUIZ_ID, quizId),
                Query.orderDesc("\$createdAt")
            )
        )
    }

    suspend fun createQuizAttempt(
        userId: String,
        quizId: String
    ): Document<Map<String, Any>> {
        return databases.createDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_ATTEMPT_TABLE_ID,
            documentId = ID.unique(),
            data = mapOf(APPWRITE_FIELD_QUIZ_ID to quizId),
            permissions = listOf(
                Permission.read(Role.user(userId)),
                Permission.delete(Role.user(userId))
            )
        )
    }

    suspend fun deleteQuizAttempt(attemptId: String) {
        databases.deleteDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_ATTEMPT_TABLE_ID,
            documentId = attemptId
        )
    }

    // --- Collections ---

    suspend fun getCollections(): DocumentList<Map<String, Any>> {
        return databases.listDocuments(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_COLLECTION_TABLE_ID
        )
    }
}
