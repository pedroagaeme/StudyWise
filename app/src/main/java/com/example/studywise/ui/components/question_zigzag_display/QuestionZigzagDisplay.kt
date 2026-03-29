package com.example.studywise.ui.components.question_zigzag_display

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer.AnswerUiState
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.QuestionCard
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.QuestionCardUiState
import com.example.studywise.ui.theme.LogoGreen
import com.example.studywise.ui.theme.LogoOrange
import com.example.studywise.ui.theme.LogoPink
import com.example.studywise.ui.theme.LogoTeal

data class ZigzagCardTransitionState (
    val offset: Float,
    val rotationAxis: Pair<Float, Float>,
    val isClockwise: Boolean
)

object  ZigzagDisplayCardSide {
    val leftSide = ZigzagCardTransitionState(-0.5f, Pair(1f, 0f), true)
    val rightSide = ZigzagCardTransitionState(0.5f, Pair(0f, 0f), false)
    fun getSide(index: Int): ZigzagCardTransitionState {
        if (index % 2 == 0) {
            return leftSide
        }
        return rightSide
    }
}

@Composable
fun ZigzagDisplayQuestionCard(
    state: QuestionCardUiState,
    cardIndex: Int,
    rotationAngle: Float,
    cardGap: Int
) {
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val transitionState = ZigzagDisplayCardSide.getSide(cardIndex)
    val rotationDirection = if(transitionState.isClockwise) 1 else -1


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.55f)
            .onGloballyPositioned { coordinates ->
                cardSize = coordinates.size
            }
            .offset {
                IntOffset(x = (cardSize.width * transitionState.offset).toInt(), y = cardGap * (cardIndex + 1))
            }
            .graphicsLayer {
                transformOrigin = TransformOrigin(
                    transitionState.rotationAxis.first,
                    transitionState.rotationAxis.second
                )
                rotationZ = rotationAngle * rotationDirection
            }
        ,
    ) {
        QuestionCard(
            state = state,
            questionNumber = cardIndex + 1,
            onAction = {},
            questionColor = when(cardIndex % 4) {
                0 -> LogoPink
                1 -> LogoOrange
                2 -> LogoTeal
                else -> LogoGreen
            }
        )
    }
}


@Composable
fun QuestionZigzagDisplay(
    cardGap: Int = 400
) {
    Box(
        modifier = Modifier
    ) {
        ZigzagDisplayQuestionCard(
            state = QuestionCardUiState(
                id = "1",
                description = "What is the capital of France?",
                answers = listOf(
                    AnswerUiState(id = "1", text = "Paris", isCorrect = true),
                    AnswerUiState(id = "2", text = "London", isCorrect = false)
                )
            ),
            cardIndex = 0,
            rotationAngle = 45f,
            cardGap = cardGap
        )
        ZigzagDisplayQuestionCard(
            state = QuestionCardUiState(
                id = "2",
                description = "What is the largest planet in our solar system?",
                answers = listOf(
                    AnswerUiState(id = "1", text = "Earth", isCorrect = false),
                    AnswerUiState(id = "2", text = "Jupiter", isCorrect = true)
                )
            ),
            cardIndex = 1,
            rotationAngle = 45f,
            cardGap = cardGap
        )
        ZigzagDisplayQuestionCard(
            state = QuestionCardUiState(
                id = "3",
                description = "Which country is known as the 'Land of the Rising Sun",
                answers = listOf(
                    AnswerUiState(id = "1", text = "China", isCorrect = false),
                    AnswerUiState(id = "2", text = "Japan", isCorrect = true)
                )
            ),
            cardIndex = 2,
            rotationAngle = 45f,
            cardGap = cardGap
        )
        ZigzagDisplayQuestionCard(
            state = QuestionCardUiState(
                id = "4",
                description = "What is the largest ocean in the world?",
                answers = listOf(
                    AnswerUiState(id = "1", text = "Atlantic Ocean", isCorrect = true),
                    AnswerUiState(id = "2", text = "Indian Ocean", isCorrect = false)
                )
            ),
            cardIndex = 3,
            rotationAngle = 45f,
            cardGap = cardGap
        )
    }
}
