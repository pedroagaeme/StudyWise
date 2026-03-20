package com.example.studywise.ui.components.model

data class Collection(
    val id: String,
    val name: String,
    val quizzes: List<Quiz>
)
