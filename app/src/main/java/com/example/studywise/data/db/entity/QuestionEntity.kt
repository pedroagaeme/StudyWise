package com.example.studywise.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "question",
    foreignKeys = [ForeignKey(
        entity = QuizEntity::class,
        parentColumns = ["id"],
        childColumns = ["quizId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("quizId")]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val description: String,
    val quizId: String,
    val createdAt: String,
    val updatedAt: String
)
