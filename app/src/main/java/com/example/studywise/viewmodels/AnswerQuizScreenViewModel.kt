package com.example.studywise.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.studywise.data.repository.QuizRepository
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizScreenUiState
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer.AnswerUiState
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.QuestionCardUiState
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizScreenAction
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizUiEffect
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AnswerQuizScreenViewModel.Factory::class)
class AnswerQuizScreenViewModel @AssistedInject constructor(
    @Assisted private val quizId: String,
    @Assisted private val forceNewAttempt: Boolean,
    private val repository: QuizRepository,
): ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(quizId: String, forceNewAttempt: Boolean = false): AnswerQuizScreenViewModel
    }
    private val _uiState = MutableStateFlow(AnswerQuizScreenUiState())
    val uiState: StateFlow<AnswerQuizScreenUiState> = _uiState.asStateFlow()

    init {
        loadQuestionPile()
    }

    fun onScrollChanged(currentScroll: Int) {
        _uiState.update { currentState ->
            if (currentState.currentScroll == currentScroll) currentState else currentState.copy(currentScroll = currentScroll)
        }
    }

    private fun loadQuestionPile() {
        viewModelScope.launch {
            val quizName = repository.getQuizById(quizId).firstOrNull()?.title ?: "Quiz"

            // 1. Fetch the data
            val questions = repository.getQuestionsByQuizId(quizId)

            // 2. Transform the database models into UI models
            val uiQuestionList = questions.map { questionWithAnswers ->
                QuestionCardUiState(
                    id = questionWithAnswers.id,
                    description = questionWithAnswers.description,
                    answers = questionWithAnswers.answerOptions.map { answerData ->
                        AnswerUiState(
                            id = answerData.id,
                            text = answerData.text,
                            isCorrect = answerData.isCorrect
                        )
                    },
                    selectedAnswer = null,
                    isFlipped = false,
                    isLoading = false
                )
            }

            // 3. Resume unfinished attempt unless caller explicitly requests a fresh one.
            if (!forceNewAttempt) {
                val lastAttempt = repository.getLastQuizAttemptById(quizId)
                if (lastAttempt != null) {
                    val firstUnansweredQuestion = lastAttempt.questionAttempts.find { it.selectedAnswerId == null }
                    if (firstUnansweredQuestion != null) {
                        val orderedPile = lastAttempt.questionAttempts.mapNotNull { questionAttempt ->
                            uiQuestionList.find { question -> question.id == questionAttempt.questionId }?.let { baseQuestion ->
                                baseQuestion.copy(answers = shuffleAnswersWithHash(baseQuestion.answers, lastAttempt.id))
                            }
                        }
                        val targetIndex = orderedPile.indexOfFirst { it.id == firstUnansweredQuestion.questionId}
                        _uiState.update {
                            it.copy(
                                quizName = quizName,
                                currentAttemptId = lastAttempt.id,
                                questionList = orderedPile,
                                targetIndex = targetIndex
                            )
                        }
                        return@launch
                    }
                }
            }

            // 4. Create new QuizAttempt with shuffled questions
            val shuffleOrder = uiQuestionList.indices.shuffled()

            val shuffleMap = mapOf(
                *shuffleOrder.mapIndexed { index, order -> uiQuestionList[order].id to index }.toTypedArray()
            )

            val quizAttemptId = repository.createQuizAttempt(quizId, shuffleMap)

            val initialQuestionList = shuffleOrder.map { index ->
                uiQuestionList[index].copy(answers = shuffleAnswersWithHash(uiQuestionList[index].answers, quizAttemptId ?: ""))
            }

            _uiState.update { currentState ->
                currentState.copy(
                    quizName = quizName,
                    currentAttemptId = quizAttemptId,
                    questionList = initialQuestionList ,
                    targetIndex = 0
                )
            }

        }
    }

    private fun shuffleAnswersWithHash(answers: List<AnswerUiState>, seed: String): List<AnswerUiState> {
        val hash = seed.hashCode().toLong()
        val random = kotlin.random.Random(hash)
        return answers.shuffled(random)
    }


    fun onAction(action: AnswerQuizScreenAction) {
        when(action) {
            is AnswerQuizScreenAction.AnswerSelected -> {
                viewModelScope.launch {
                    _uiState.update { currentState ->
                        currentState.copy(
                            questionList = currentState.questionList.mapIndexed { index, question ->
                                if (index == action.questionIndex) {
                                    question.copy(
                                        selectedAnswer = action.answer,
                                    )
                                } else {
                                    question
                                }
                            }
                        )
                    }
                    val questionId = _uiState.value.questionList[action.questionIndex].id
                    val selectedAnswerId =
                        _uiState.value.questionList[action.questionIndex].selectedAnswer?.id
                    val quizAttemptId = _uiState.value.currentAttemptId
                    if (quizAttemptId != null && selectedAnswerId != null) {
                        viewModelScope.launch {
                            repository.updateQuestionAttempt(
                                questionId = questionId,
                                selectedAnswerId = selectedAnswerId,
                                quizAttemptId = quizAttemptId
                            )
                        }
                    }
                    delay(action.transitionDelayMs)
                    _uiState.update { currentState ->
                        currentState.copy(
                            questionList = currentState.questionList.mapIndexed { index, question ->
                                if (index == action.questionIndex) {
                                    val updatedFlipped =
                                        if (action.flipNeeded) !question.isFlipped else question.isFlipped
                                    question.copy(
                                        isFlipped = updatedFlipped,
                                    )
                                } else {
                                    question
                                }
                            }
                        )
                    }
                    goToNextQuestion(action.questionIndex)
                }
            }
            is AnswerQuizScreenAction.FlipToggled -> {
                val tempQuestionList = _uiState.value.questionList.toMutableList()
                tempQuestionList[action.questionIndex] = tempQuestionList[action.questionIndex].copy(isFlipped = !tempQuestionList[action.questionIndex].isFlipped)
                _uiState.update { currentState ->
                    currentState.copy(questionList = tempQuestionList) }
            }
        }
    }
    private fun goToNextQuestion(questionIndex: Int) {
        _uiState.update { it.copy(targetIndex = questionIndex + 1) }
        if (questionIndex + 1 >= _uiState.value.questionList.size) {
            _uiState.update { currentState ->
                currentState.copy(
                    pendingEffect = AnswerQuizUiEffect.FinishQuiz(
                        quizId = quizId,
                        attemptId = currentState.currentAttemptId ?: ""
                    )
                )
            }
        }
    }

    companion object {

        fun factory(
            quizId: String,
            forceNewAttempt: Boolean,
            repository: QuizRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AnswerQuizScreenViewModel(
                    quizId = quizId,
                    forceNewAttempt = forceNewAttempt,
                    repository = repository
                )
            }
        }
    }
}