package com.example.studywise

import android.content.Context
import com.example.studywise.constants.*
import com.example.studywise.data.*
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Role
import io.appwrite.models.*
import io.appwrite.services.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.collections.listOf
import kotlin.time.Duration.Companion.seconds

object Appwrite {

    private const val POLL_INTERVAL_MS = 1000L
    private val EXECUTION_TIMEOUT = 90.seconds
    private val TERMINAL_STATUSES = setOf("completed", "failed", "cancelled")
    lateinit var client: Client
    lateinit var account: Account
    lateinit var databases: Databases
    lateinit var functions: Functions
    lateinit var storage: Storage

    fun init(context: Context) {
        client = Client(context)
            .setEndpoint(APPWRITE_PUBLIC_ENDPOINT)
            .setProject(APPWRITE_PROJECT_ID)

        account = Account(client)
        databases = Databases(client)
        functions = Functions(client)
        storage = Storage(client)
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

    suspend fun uploadQuiz(quiz: QuizData): String {
        val uploadedQuiz = databases.createDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_TABLE_ID,
            documentId = ID.unique(),
            data = quiz,
            permissions = listOf(
                Permission.read(Role.user(getCurrentUser().id)),
                Permission.delete(Role.user(getCurrentUser().id))
            )
        )
        return uploadedQuiz.id
    }

    suspend fun getQuizDetails(quizId: String): Document<Map<String, Any>> {
        return databases.getDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_TABLE_ID,
            documentId = quizId
        )
    }

    suspend fun deleteQuiz(quizId: String) {
        databases.deleteDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_QUIZ_TABLE_ID,
            documentId = quizId
        )
    }

    suspend fun uploadFilesToBucket(
        files: List<File>,
        bucketId: String = APPWRITE_FILES_BUCKET_ID
    ): List<String> {
        return files.map { file ->
            val uploadedFile = storage.createFile(
                bucketId = bucketId,
                fileId = ID.unique(),
                file = InputFile.fromFile(file)
            )
            uploadedFile.id
        }
    }

    suspend fun generateQuiz(
        difficulty: String,
        size: String,
        quizSummary: String? = null,
        files: List<File> = emptyList(),
        links: List<String> = emptyList(),
        functionId: String = APPWRITE_GENERATE_QUIZ_FUNCTION_ID
    ): GenerateQuizResponse {
        val normalizedDifficulty = difficulty.trim().lowercase()
        val normalizedSize = size.trim().lowercase()

        require(normalizedDifficulty in setOf("easy", "medium", "hard")) {
            "difficulty must be one of: easy, medium, hard"
        }
        require(normalizedSize in setOf("small", "medium", "large")) {
            "size must be one of: small, medium, large"
        }

        // Upload files to storage and get their IDs
        val uploadedFileIds = if (files.isNotEmpty()) {
            uploadFilesToBucket(files)
        } else {
            emptyList()
        }

        val documents = if (uploadedFileIds.isNotEmpty() || links.isNotEmpty()) {
            GenerateQuizDocuments(
                ids = uploadedFileIds.takeIf { it.isNotEmpty() },
                links = links.takeIf { it.isNotEmpty() }
            )
        } else {
            null
        }

        val request = GenerateQuizRequest(
            difficulty = normalizedDifficulty,
            size = normalizedSize,
            quizSummary = quizSummary?.trim()?.takeIf { it.isNotEmpty() },
            documents = documents
        )

        val json = Json { ignoreUnknownKeys = true }
        val requestBody = json.encodeToString(GenerateQuizRequest.serializer(), request)

        val created = functions.createExecution(
            functionId = functionId,
            body = requestBody,
            async = true
        )

        val finalExecution = withTimeout(EXECUTION_TIMEOUT) {
            var latest = created
            while (latest.status.lowercase() !in TERMINAL_STATUSES) {
                delay(POLL_INTERVAL_MS)
                latest = functions.getExecution(functionId, latest.id)
            }
            latest
        }

        val finalStatus = finalExecution.status.lowercase()
        val body = finalExecution.responseBody.orEmpty()

        if (finalStatus != "completed") {
            throw Exception(
                "Quiz generation failed. status=$finalStatus, code=${finalExecution.responseStatusCode}, body=$body"
            )
        }


        if (body.isBlank()) {
            throw Exception(
                "Quiz generation completed but returned empty body. executionId=${finalExecution.id}"
            )
        }

        return try {
            json.decodeFromString(GenerateQuizResponse.serializer(), body)
        } catch (e: Exception) {
            throw Exception("Failed to parse quiz response: ${e.message}")
        }
    }



}
