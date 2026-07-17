package com.example.studywise.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "quiz",
    foreignKeys = [ForeignKey(
        entity = QuizCollectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["quizCollectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("quizCollectionId")]
)
data class QuizEntity(
    @PrimaryKey val id: String,
    val title: String,
    val quizCollectionId: String?,
    val createdAt: String,
    val updatedAt: String
)
