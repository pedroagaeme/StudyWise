package com.example.studywise.ui.screens.answer_quiz

import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer.AnswerUiState

sealed interface AnswerQuizScreenAction {
    data class AnswerSelected(val answer: AnswerUiState, val questionIndex: Int) : AnswerQuizScreenAction
    data class FlipToggled(val questionIndex: Int) : AnswerQuizScreenAction
    data class NextQuestionCardRequested(val questionIndex: Int) : AnswerQuizScreenAction
}