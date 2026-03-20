package com.example.studywise.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_collection")
data class QuizCollectionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String
)
