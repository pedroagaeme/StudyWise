package com.example.studywise.data.repository

import android.net.Uri
import com.example.studywise.Appwrite
import com.example.studywise.data.db.dao.QuizDao
import com.example.studywise.data.db.relation.QuizWithQuestions
import io.appwrite.models.InputFile
import java.io.File
import javax.inject.Inject


class QuizRepository @Inject constructor(
    private val quizDao: QuizDao
) {
    suspend fun getQuizDetails(quizId: String): QuizWithQuestions {
        val localResult = quizDao.getQuizWithQuestionsById(quizId)
        if (localResult != null) {
            return localResult
        }
        throw Exception("Quiz not found")
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
}