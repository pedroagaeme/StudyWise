package com.example.studywise.ui.screens.answer_quiz.components.question_pile

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.example.studywise.ui.components.custom_progress_indicators.CustomLinearProgressIndicator
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizScreenUiState
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.QuestionCard
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.QuestionCardUiState
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizScreenAction
import com.example.studywise.ui.theme.LogoGreen
import com.example.studywise.ui.theme.LogoOrange
import com.example.studywise.ui.theme.LogoPink
import com.example.studywise.ui.theme.LogoTeal
import kotlin.math.min

// Since we want to simulate the feeling of a pile of cards,
// we use graphicsLayer to simulate the 3d position of the cards in 2d
// and lerp to animate from one position to the other once the pile moves
// ps: notice that it works as a queue (first in, first out)

data class CardTransitionState(
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f
)

object StackConstants {
    val SlotGhost = CardTransitionState(rotationY = 12f, rotationZ = -12f, translationY = -100f, translationX = 0f, scale = 0.85f, alpha = 0f)
    val Slot4 = CardTransitionState(rotationY = 4f, rotationZ = -8f, translationY = -90f, translationX = -10f, scale = 0.88f, alpha = 1f)
    val Slot3 = CardTransitionState(rotationY = -5f, rotationZ = 0f, translationY = 40f, translationX = 50f, scale = 0.92f, alpha = 1f)
    val Slot2 = CardTransitionState(rotationY = 2.5f, rotationZ = -5f, translationY = 40f, translationX = -45f, scale = 0.95f, alpha = 1f)
    val Slot1 = CardTransitionState(rotationY = 0f, rotationZ = 0f, translationY = 0f, translationX = 0f, scale = 1f, alpha = 1f)
    val SlotExit = CardTransitionState(rotationY = 0f, rotationZ = -45f, translationY = 100f, translationX = -1500f, scale = 1f, alpha = 1f)
}

object StackPositions {
    fun getStateFor(distance: Float): CardTransitionState {
        return when {
            distance <= -4f -> StackConstants.SlotGhost
            distance < -3f -> interpolate(StackConstants.SlotGhost, StackConstants.Slot4, distance + 4f)
            distance == -3f -> StackConstants.Slot4
            distance < -2f -> interpolate(StackConstants.Slot4, StackConstants.Slot3, distance + 3f)
            distance == -2f -> StackConstants.Slot3
            distance < -1f -> interpolate(StackConstants.Slot3, StackConstants.Slot2, distance + 2f)
            distance == -1f -> StackConstants.Slot2
            distance < 0f -> interpolate(StackConstants.Slot2, StackConstants.Slot1, distance + 1f)
            distance == 0f -> StackConstants.Slot1
            distance > 0f -> interpolate(StackConstants.Slot1, StackConstants.SlotExit, distance)
            else -> StackConstants.SlotExit
        }
    }

    private fun interpolate(from: CardTransitionState, to: CardTransitionState, progress: Float): CardTransitionState {
        return CardTransitionState(
            rotationY = lerp(from.rotationY, to.rotationY, progress),
            rotationZ = lerp(from.rotationZ, to.rotationZ, progress),
            translationX = lerp(from.translationX, to.translationX, progress),
            translationY = lerp(from.translationY, to.translationY, progress),
            scale = lerp(from.scale, to.scale, progress),
            alpha = lerp(from.alpha, to.alpha, progress)
        )
    }
}

@Composable
fun QuestionPile(
    state: AnswerQuizScreenUiState,
    onAction: (AnswerQuizScreenAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Calculate the values for the progress bar
    val questionCount = state.questionList.count()
    val currentQuestionNumber = if (questionCount == 0) 0 else min(state.targetIndex + 1, questionCount)
    val paddedQuestionCount = questionCount.toString().padStart(2, '0')
    val paddedQuestionNumber = currentQuestionNumber.toString().padStart(2, '0')

    val targetProgress = if (questionCount == 0) 0f else currentQuestionNumber.toFloat() / questionCount
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(600)
    )

    // Animate cards whenever a new index value is provided
    val animatedCurrentIndex = remember { Animatable(state.targetIndex.toFloat()) }
    
    // Animate the flip of the current card
    val isCurrentCardFlippedProgress = remember { Animatable(0f) }
    val isCurrentCardFlippedFloat = state.questionList.getOrNull(state.targetIndex)
        ?.let { if (it.isFlipped) 1f else 0f }
        ?: 0f

    // Animate the card in a way only one type of animation runs at a time
    val isQuestionIndexStill = state.targetIndex.toFloat() == animatedCurrentIndex.value
    val isFlipProgressStill = isCurrentCardFlippedProgress.value == isCurrentCardFlippedProgress.targetValue
    // 1. Check if current card needs to flip
    // It can only flip if the card is still (not going from question 1 to 2, for example)
    LaunchedEffect(isCurrentCardFlippedFloat, isQuestionIndexStill) {
        if (isQuestionIndexStill) {
            if (isCurrentCardFlippedProgress.targetValue != isCurrentCardFlippedFloat) {
                isCurrentCardFlippedProgress.animateTo(
                    targetValue = isCurrentCardFlippedFloat,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = LinearOutSlowInEasing
                    )
                )
            }
        }
    }
    LaunchedEffect(state.targetIndex) {
        // 2. Check if the target index has changed
        // Can only animate once the card is still regarding flip position
        if (isFlipProgressStill) {
            if (animatedCurrentIndex.targetValue != state.targetIndex.toFloat()) {
                val current = animatedCurrentIndex.value.toInt()
                val target = state.targetIndex
                val stepDuration = 200

                // Reset the flip state for the next card
                isCurrentCardFlippedProgress.snapTo(0f)

                if (target > current) {
                    // Step forward one by one
                    for (i in (current + 1)..target) {
                        animatedCurrentIndex.animateTo(
                            targetValue = i.toFloat(),
                            animationSpec = tween(
                                durationMillis = if (i == target) 600 else stepDuration,
                                easing = if (i == target) FastOutSlowInEasing else FastOutLinearInEasing
                            ),

                            )
                    }
                } else if (target < current) {
                    // Step backward one by one
                    for (i in (current - 1) downTo target) {
                        animatedCurrentIndex.animateTo(
                            targetValue = i.toFloat(),
                            animationSpec = tween(
                                durationMillis = if (i == target) 600 else stepDuration,
                                easing = if (i == target) FastOutSlowInEasing else LinearEasing
                            )
                        )
                    }
                }
            }
        }
    }

    val currentIndexValue = animatedCurrentIndex.value
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomLinearProgressIndicator(
                modifier = Modifier
                    .weight(1f),
                progress
            )
            Text(
                text = "${paddedQuestionNumber}/${paddedQuestionCount}",
                style = MaterialTheme.typography.titleLarge.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Animate only the cards that should be visible
            // (the one that gets thrown away and the next 4 in line)
            state.questionList.forEachIndexed { index, questionCardState ->
                val distance = currentIndexValue - index.toFloat()
                if (distance in -4f..1f) {
                    CardInPile(
                        state = questionCardState,
                        cardTransitionState = StackPositions.getStateFor(distance),
                        cardIndex = index,
                        flipProgress = if (index == state.targetIndex) {
                            isCurrentCardFlippedProgress.value
                        } else {
                            if (questionCardState.isFlipped) 1f else 0f
                        },
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@Composable
fun CardInPile(
    state: QuestionCardUiState,
    cardTransitionState: CardTransitionState,
    cardIndex: Int,
    flipProgress: Float,
    onAction: (AnswerQuizScreenAction) -> Unit
) {
    Box(
        modifier = Modifier
            // Cards that appear first on the list are closer to user
            .zIndex(-cardIndex.toFloat())
            .fillMaxWidth()
            .aspectRatio(0.55f)
            .graphicsLayer {
                rotationY = cardTransitionState.rotationY
                rotationZ = cardTransitionState.rotationZ
                translationX = cardTransitionState.translationX
                translationY = cardTransitionState.translationY
                scaleX = cardTransitionState.scale
                scaleY = cardTransitionState.scale
                alpha = cardTransitionState.alpha
                cameraDistance = 12f * density
                transformOrigin = TransformOrigin.Center
            },
        contentAlignment = Alignment.Center
    ) {
        QuestionCard(
            state = state,
            questionNumber = cardIndex + 1,
            onAction = onAction,
            flipProgress = flipProgress,
            questionColor = when(cardIndex % 4) {
                0 -> LogoPink
                1 -> LogoOrange
                2 -> LogoTeal
                else -> LogoGreen
            }
        )
    }
}
