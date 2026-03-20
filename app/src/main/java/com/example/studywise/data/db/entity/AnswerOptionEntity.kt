package com.example.studywise.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "answer",
    foreignKeys = [ForeignKey(
        entity = QuestionEntity::class,
        parentColumns = ["id"],
        childColumns = ["questionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("questionId")]
)
data class AnswerOptionEntity(
    @PrimaryKey val id: String,
    val text: String,
    val isCorrect: Boolean,
    val questionId: String,
    val createdAt: String,
    val updatedAt: String
)
