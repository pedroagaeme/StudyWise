package com.example.studywise.ui.components.quiz.question_pile.question_card

import com.example.studywise.ui.components.quiz.question_pile.question_card.answer.AnswerUiState

sealed interface QuestionPileAction {
    data class AnswerSelected(val answer: AnswerUiState, val questionIndex: Int) : QuestionPileAction
    data class FlipToggled(val questionIndex: Int) : QuestionPileAction
    data class NextQuestionCardRequested(val questionIndex: Int) : QuestionPileAction
}