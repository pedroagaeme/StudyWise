package com.example.studywise.ui.components.model

data class Quiz(
    val id: String,
    val title: String,
    val lastInteracted: String,
    val collectionName: String? = null,
    val questionCount: Int = 0,
    val averageScore: Float? = null
)