package com.example.studywise.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.screens.create_quiz.AttachmentPreview
import com.example.studywise.ui.screens.create_quiz.AttachmentType
import com.example.studywise.ui.screens.create_quiz.CollectionMode
import com.example.studywise.ui.screens.create_quiz.CreateQuizScreenAction
import com.example.studywise.ui.screens.create_quiz.CreateQuizScreenEffect
import com.example.studywise.ui.screens.create_quiz.CreateQuizStep
import com.example.studywise.ui.screens.create_quiz.CreateQuizUiState
import com.example.studywise.ui.screens.create_quiz.QuizDifficulty
import com.example.studywise.ui.screens.create_quiz.QuizSize
import com.example.studywise.utils.uriToFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        loadCollections()
    }

    private fun loadCollections() {
        viewModelScope.launch {
            repository.getCollections().collect { collections ->
                _uiState.update { it.copy(existingCollections = collections.map {
                        it.name
                    })
                }
            }
        }
    }

    fun effectConsumed() {
        _uiState.update {
            it.copy(pendingEffect = null)
        }
    }

    fun onAction(action: CreateQuizScreenAction) {
        when (action) {
            is CreateQuizScreenAction.OnDismiss -> {
                _uiState.update { currentState ->
                    currentState.copy(pendingEffect = CreateQuizScreenEffect.Dismiss)
                }
            }
            is CreateQuizScreenAction.OnGenerateQuizButtonClick -> {
                generateQuiz()
            }
            is CreateQuizScreenAction.OnFileButtonClick -> {
                _uiState.update { currentState ->
                    currentState.copy(pendingEffect = CreateQuizScreenEffect.OpenFilePicker)
                }
            }
            is CreateQuizScreenAction.OnAddAttachment -> {
                if (_uiState.value.attachments.size < 3) {
                    val displayName = resolveUriDisplayName(action.uri)
                    _uiState.update { currentState ->
                        currentState.copy(
                            attachments = currentState.attachments + AttachmentPreview(
                                type = action.attachmentType,
                                name = displayName,
                                uri = action.uri
                            )
                        )
                    }
                }
            }
            is CreateQuizScreenAction.OnRemoveAttachment -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        attachments = currentState.attachments.filterNot { it == action.attachment }
                    )
                }
            }
            is CreateQuizScreenAction.OnQuizScreenDifficultyChange -> {
                _uiState.update { it.copy(quizDifficulty = action.quizDifficulty) }
            }
            is CreateQuizScreenAction.OnQuizScreenSizeChange -> {
                _uiState.update { it.copy(quizSize = action.quizSize) }
            }
            is CreateQuizScreenAction.OnQuizScreenSummaryChange -> {
                _uiState.update { it.copy(quizSummary = action.quizSummary) }
            }
            is CreateQuizScreenAction.OnQuizNameChange -> {
                _uiState.update { it.copy(quizName = action.name) }
            }
            is CreateQuizScreenAction.OnCollectionNameChange -> {
                _uiState.update { it.copy(collectionName = action.name) }
            }
            is CreateQuizScreenAction.OnCollectionModeChange -> {
                _uiState.update { it.copy(collectionMode = action.mode) }
            }
            is CreateQuizScreenAction.OnCollectionSelected -> {
                _uiState.update { it.copy(selectedCollection = action.collectionName) }
            }
            is CreateQuizScreenAction.OnConfirmQuizCreation -> {
                confirmQuizCreation()
            }
        }
    }

    private fun generateQuiz() {
        viewModelScope.launch {
            _uiState.update { it.copy(currentStep = CreateQuizStep.GENERATING) }
            
            val state = _uiState.value
            try {
                val result = repository.generateQuiz(
                    difficulty = state.quizDifficulty.name.lowercase(),
                    size = state.quizSize.name.lowercase(),
                    quizSummary = state.quizSummary,
                    files = state.attachments
                        .filter { it.type == AttachmentType.FILE }
                        .map { context.uriToFile(it.uri) },
                    links = state.attachments
                        .filter { it.type == AttachmentType.LINK }
                        .map { it.uri.toString() }
                )
                
                val quizId = repository.uploadQuiz(result)
                if (quizId != null) {
                    _uiState.update { it.copy(
                        currentStep = CreateQuizStep.CONFIRMATION,
                        generatedQuizId = quizId,
                        quizName = result.quiz.title,
                        collectionName = result.quiz.quizCollection.name
                    ) }
                } else {
                    // Handle error, maybe go back to config
                    _uiState.update { it.copy(currentStep = CreateQuizStep.CONFIGURATION) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(currentStep = CreateQuizStep.CONFIGURATION) }
            }
        }
    }

    private fun confirmQuizCreation() {
        val state = _uiState.value
        val quizId = state.generatedQuizId ?: return
        
        viewModelScope.launch {
            repository.updateQuizDetails(quizId, state.quizName, state.collectionName)
            
            _uiState.update { it.copy(
                pendingEffect = CreateQuizScreenEffect.QuizGenerated(quizId)
            ) }
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

        val lastSegment = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        if (lastSegment != null) return lastSegment

        val ext = context.contentResolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.let { ".$it" }
            ?: ""

        return "attachment_${UUID.randomUUID()}$ext"
    }
}
