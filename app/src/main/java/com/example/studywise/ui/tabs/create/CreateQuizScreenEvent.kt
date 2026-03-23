package com.example.studywise.ui.tabs.create

sealed interface CreateQuizScreenEvent {
    data object OpenFilePicker : CreateQuizScreenEvent
}