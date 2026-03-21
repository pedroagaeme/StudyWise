package com.example.studywise.ui.tabs.create

sealed interface CreateQuizAction {
    data object OnDismiss : CreateQuizAction
    data class OnQuizSizeChange(val quizSize: QuizSize) : CreateQuizAction
    data class OnQuizDifficultyChange(val quizDifficulty: QuizDifficulty) : CreateQuizAction
    data class OnQuizContentChange(val quizContent: String) : CreateQuizAction
    data class OnAddAttachment(val attachmentType: AttachmentType) : CreateQuizAction
    data class OnRemoveAttachment(val attachment: AttachmentPreview) : CreateQuizAction
    data object OnGenerateQuiz : CreateQuizAction
}

