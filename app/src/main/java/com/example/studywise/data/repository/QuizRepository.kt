package com.example.studywise.data.repository

import com.example.studywise.data.db.dao.QuizDao
import com.example.studywise.data.db.relation.QuizWithQuestions

class QuizRepository(private val quizDao: QuizDao) {
    suspend fun getQuizDetails(quizId: String): QuizWithQuestions {
        return quizDao.getQuizWithQuestionsById(quizId)
    }
}