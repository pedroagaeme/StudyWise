package com.example.studywise

import android.content.Context
import android.util.Log
import com.example.studywise.constants.*
import com.example.studywise.data.*
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Role
import io.appwrite.enums.ExecutionStatus
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.*
import io.appwrite.services.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject
import java.io.File
import java.time.Instant
import kotlin.collections.listOf
import kotlin.time.Duration.Companion.seconds

object Appwrite {

    private const val POLL_INTERVAL_MS = 1000L
    private val EXECUTION_TIMEOUT = 90.seconds
    private val TERMINAL_STATUSES = setOf(ExecutionStatus.COMPLETED, ExecutionStatus.FAILED)

    lateinit var client: Client
    lateinit var account: Account
    lateinit var tablesDB: TablesDB
    lateinit var functions: Functions
    lateinit var storage: Storage

    fun init(context: Context) {
        client = Client(context)
            .setEndpoint(APPWRITE_PUBLIC_ENDPOINT)
            .setProject(APPWRITE_PROJECT_ID)

        account = Account(client)
        tablesDB = TablesDB(client)
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

    suspend fun getCurrentSessionOrNull(): Session? {
        return try {
            account.getSession("current")
        } catch (e: AppwriteException) {
            if (e.code == 401 || e.code == 404) null else throw e
        }
    }

    suspend fun isSessionActive(): Boolean {
        val session = getCurrentSessionOrNull() ?: return false
        return try {
            Instant.parse(session.expire).isAfter(Instant.now())
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendVerification(url: String): Token {
        return account.createEmailVerification(url)
    }

    suspend fun confirmVerification(userId: String, secret: String): Token {
        return account.updateEmailVerification(userId, secret)
    }

    suspend fun uploadQuiz(quizRequest: UploadQuizRequestData, quizId: String): String? {
        val quizData = mapOf(
            "title" to quizRequest.title,
            "quizCollection" to mapOf(
                "name" to quizRequest.quizCollection.name
            ),
            "questions" to quizRequest.questions.map { question ->
                mapOf(
                    "description" to question.description,
                    "type" to question.type,
                    "explanation" to question.explanation,
                    "answerOptions" to question.answerOptions.map { option ->
                        mapOf(
                            "text" to option.text,
                            "isCorrect" to option.isCorrect
                        )
                    }
                )
            }
        )

        try {
            val uploadedQuiz = tablesDB.createRow(
                databaseId = APPWRITE_DATABASE_ID,
                tableId = APPWRITE_QUIZ_TABLE_ID,
                rowId = quizId,
                data = quizData,
                permissions = listOf(
                    Permission.read(Role.user(getCurrentUser().id)),
                    Permission.delete(Role.user(getCurrentUser().id))
                )
            )
            return quizId
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun generateNewId(): String {
        return ID.unique()
    }

    suspend fun getQuizDetails(quizId: String): Row<Map<String, Any>> {
        return tablesDB.getRow(
            databaseId = APPWRITE_DATABASE_ID,
            tableId = APPWRITE_QUIZ_TABLE_ID,
            rowId = quizId
        )
    }

    suspend fun deleteQuiz(quizId: String) {
        tablesDB.deleteRow(
            databaseId = APPWRITE_DATABASE_ID,
            tableId = APPWRITE_QUIZ_TABLE_ID,
            rowId = quizId
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

    suspend fun fetchQuizFromBucket(quizResponseFileName: String): GenerateQuizResponse {
        val storage = Storage(client)
        val bucketId = APPWRITE_QUIZ_RESPONSES_BUCKET_ID
        val fileName = "$quizResponseFileName.json"

        return try {
            // 1. Download the file as a ByteArray
            val responseBytes = storage.getFileDownload(
                bucketId = bucketId,
                fileId = fileName
            )

            // 2. Convert bytes to String
            val jsonString = String(responseBytes, Charsets.UTF_8)

            // 3. Parse JSON using kotlinx.serialization
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString(GenerateQuizResponse.serializer(), jsonString)

        } catch (e: Exception) {
            throw Exception("Failed to download quiz data: ${e.message}")
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
            quizResponseFileName = ID.unique(),
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
            while (latest.status !in TERMINAL_STATUSES) {
                delay(POLL_INTERVAL_MS)
                try {
                    latest = functions.getExecution(functionId, latest.id)
                }
                catch (e: Exception) {
                    continue
                }
            }
            latest
        }

        val finalStatus = finalExecution.status

        if (finalStatus != ExecutionStatus.COMPLETED) {
            throw Exception(
                "Quiz generation failed. status=$finalStatus, code=${finalExecution.responseStatusCode}"
            )
        }

        return fetchQuizFromBucket(quizResponseFileName = request.quizResponseFileName)
    }



}
