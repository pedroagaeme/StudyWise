package com.example.studywise.data.db.dao

import androidx.room.*
import com.example.studywise.data.db.entity.QuizEntity
import com.example.studywise.data.db.relation.QuizWithQuestions

@Dao
interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quiz: QuizEntity)

    @Delete
    suspend fun delete(quiz: QuizEntity)

    @Transaction
    @Query("SELECT * FROM quiz WHERE id = :id")
    suspend fun getQuizWithQuestionsById(id: String): QuizWithQuestions?
}