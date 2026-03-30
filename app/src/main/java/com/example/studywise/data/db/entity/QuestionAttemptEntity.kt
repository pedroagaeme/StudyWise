package com.example.studywise.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "question_attempt",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AnswerOptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["selectedAnswerId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = QuizAttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizAttemptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("questionId"), Index("selectedAnswerId"), Index("quizAttemptId")]
)
data class QuestionAttemptEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val selectedAnswerId: String?,
    val quizAttemptId: String,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String
)

