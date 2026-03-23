package com.example.studywise.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.tabs.create.AttachmentPreview
import com.example.studywise.ui.tabs.create.AttachmentType
import com.example.studywise.ui.tabs.create.CreateQuizScreenAction
import com.example.studywise.ui.tabs.create.CreateQuizScreenEvent
import com.example.studywise.ui.tabs.create.CreateQuizUiState
import com.example.studywise.utils.uriToFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateQuizScreenViewModel @Inject constructor(
    private val repository: QuizRepository,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<CreateQuizScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: CreateQuizScreenAction) {
        when (action) {
            is CreateQuizScreenAction.OnGenerateQuizButtonClick -> {
                generateQuiz()
            }
            is CreateQuizScreenAction.OnFileButtonClick -> {
                viewModelScope.launch {
                    eventChannel.send(CreateQuizScreenEvent.OpenFilePicker)
                }
            }
            is CreateQuizScreenAction.OnAddAttachment -> {
                val currentList = _uiState.value.attachments
                if (currentList.size < 3) {
                    val displayName = resolveUriDisplayName(action.uri)
                    _uiState.update { currentState -> currentState.copy(
                        attachments = currentState.attachments +
                                AttachmentPreview(
                                    type = action.attachmentType,
                                    name = displayName,
                                    uri = action.uri
                                )
                    )}
                }
            }
            is CreateQuizScreenAction.OnRemoveAttachment -> {
                _uiState.update { currentState -> currentState.copy(
                    attachments = _uiState.value.attachments.filterNot { it == action.attachment }
                )}
            }
            is CreateQuizScreenAction.OnQuizScreenDifficultyChange -> {
                _uiState.update { currentState ->
                    currentState.copy(quizDifficulty = action.quizDifficulty)
                }
            }
            is CreateQuizScreenAction.OnQuizScreenSizeChange -> {
                _uiState.update { currentState ->
                    currentState.copy(quizSize = action.quizSize)
                }
            }
            is CreateQuizScreenAction.OnQuizScreenSummaryChange -> {
                _uiState.update { currentState ->
                    currentState.copy(quizSummary = action.quizSummary)
                }
            }
            else -> Unit
        }
    }


    private fun generateQuiz() {
        viewModelScope.launch {
            val state = _uiState.value
            val result = repository.generateQuiz(
                difficulty = state.quizDifficulty.name.lowercase(),
                size = state.quizSize.name.lowercase(),
                quizSummary = state.quizSummary,
                files = state.attachments
                    .filter { it.type == AttachmentType.FILE }
                    .map { attachmentPreview ->
                        context.uriToFile(attachmentPreview.uri)
                    }
                ,
                links = state.attachments
                    .filter { it.type == AttachmentType.LINK }
                    .map { it.uri.toString() }
            )
        }
    }

    private fun resolveUriDisplayName(uri: Uri): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }

        // Fallbacks when provider doesn't expose DISPLAY_NAME
        val lastSegment = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        if (lastSegment != null) return lastSegment

        val ext = context.contentResolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.let { ".$it" }
            ?: ""

        return "attachment_${UUID.randomUUID()}$ext"
    }
}