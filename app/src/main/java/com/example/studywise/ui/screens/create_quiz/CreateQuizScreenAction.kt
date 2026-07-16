package com.example.studywise.ui.screens.create_quiz

import android.net.Uri

sealed interface CreateQuizScreenAction {
    data object OnDismiss : CreateQuizScreenAction
    data class OnQuizScreenSizeChange(val quizSize: QuizSize) : CreateQuizScreenAction
    data class OnQuizScreenDifficultyChange(val quizDifficulty: QuizDifficulty) : CreateQuizScreenAction
    data class OnQuizScreenSummaryChange(val quizSummary: String) : CreateQuizScreenAction
    data class OnAddAttachment(val attachmentType: AttachmentType, val uri: Uri) : CreateQuizScreenAction
    data class OnRemoveAttachment(val attachment: AttachmentPreview) : CreateQuizScreenAction
    data object OnGenerateQuizButtonClick : CreateQuizScreenAction
    data object OnFileButtonClick : CreateQuizScreenAction
    
    // Step 3 Actions
    data class OnQuizNameChange(val name: String) : CreateQuizScreenAction
    data class OnCollectionNameChange(val name: String) : CreateQuizScreenAction
    data class OnCollectionModeChange(val mode: CollectionMode) : CreateQuizScreenAction
    data class OnCollectionSelected(val collectionName: String) : CreateQuizScreenAction
    data object OnConfirmQuizCreation : CreateQuizScreenAction
}
