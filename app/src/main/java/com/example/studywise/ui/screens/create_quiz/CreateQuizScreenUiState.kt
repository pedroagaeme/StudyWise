package com.example.studywise.ui.screens.create_quiz

import android.net.Uri

const val MAX_ATTACHMENTS = 3

enum class CreateQuizStep {
    CONFIGURATION,
    GENERATING,
    CONFIRMATION
}

enum class CollectionMode {
    NEW,
    EXISTING
}

enum class QuizSize(val label: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large")
}

enum class QuizDifficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

enum class AttachmentType {
    FILE,
    LINK
}

data class AttachmentPreview(
    val type: AttachmentType,
    val name: String,
    val uri: Uri
)

data class CreateQuizUiState(
    val currentStep: CreateQuizStep = CreateQuizStep.CONFIGURATION,
    val quizSize: QuizSize = QuizSize.MEDIUM,
    val quizDifficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    val quizSummary: String = "",
    val attachments: List<AttachmentPreview> = emptyList(),
    val pendingEffect: CreateQuizScreenEffect? = null,
    
    // Step 3 fields
    val quizName: String = "",
    val collectionName: String = "",
    val collectionMode: CollectionMode = CollectionMode.NEW,
    val existingCollections: List<String> = emptyList(),
    val selectedCollection: String? = null,
    val generatedQuizId: String? = null
)
