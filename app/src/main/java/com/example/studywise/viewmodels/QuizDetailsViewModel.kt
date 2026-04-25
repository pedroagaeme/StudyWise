package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.screens.quiz_details.AttemptCardUiState
import com.example.studywise.ui.screens.quiz_details.QuizDetailsUiState
import com.example.studywise.utils.formatDateHumanReadable
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

@HiltViewModel(assistedFactory = QuizDetailsViewModel.Factory::class)
class QuizDetailsViewModel @AssistedInject constructor(
	@Assisted private val quizId: String,
	private val repository: QuizRepository,
) : ViewModel() {

	@AssistedFactory
	interface Factory {
		fun create(quizId: String): QuizDetailsViewModel
	}

	private val _uiState = MutableStateFlow(QuizDetailsUiState(quizId = quizId))
	val uiState: StateFlow<QuizDetailsUiState> = _uiState.asStateFlow()

	init {
		observeQuizDetails()
	}

	private fun observeQuizDetails() {
		combine(
			repository.getQuizById(quizId),
			repository.getQuizAttemptsByQuizIdFlow(quizId)
		) { quiz, attempts ->
			_uiState.update { current ->
				val questionCount = quiz?.questionCount ?: 0
				val attemptCards = attempts.map { attempt ->
					val answeredQuestions = attempt.questionAttempts.count { it.selectedAnswerId != null }
					val remainingQuestions = (questionCount - answeredQuestions).coerceAtLeast(0)
					AttemptCardUiState(
						attemptId = attempt.id,
						answeredQuestions = answeredQuestions,
						remainingQuestions = remainingQuestions,
						score = if (remainingQuestions == 0) attempt.score else null,
						timeLabel = formatDateHumanReadable(attempt.createdAt)
					)
				}
				val bestScore = attemptCards.mapNotNull { it.score }.maxOrNull()

				current.copy(
					quizName = quiz?.title ?: "",
					collectionName = quiz?.collectionName ?: "-",
					questionCount = questionCount,
					createdAtLabel = quiz?.createdAt?.let { formatDateHumanReadable(it, showRelative = false) } ?: "-",
					averageScore = quiz?.averageScore,
					bestScore = bestScore,
					attempts = attemptCards,
					isLoading = false
				)
			}
		}.launchIn(viewModelScope)
	}

	companion object {
		fun factory(
			quizId: String,
			repository: QuizRepository,
		): ViewModelProvider.Factory = viewModelFactory {
			initializer {
				QuizDetailsViewModel(
					quizId = quizId,
					repository = repository,
				)
			}
		}
	}
}


