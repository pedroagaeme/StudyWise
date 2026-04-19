package com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizScreenAction
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer.AnswerOptionCard
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.question_card.answer.AnswerUiState
import com.example.studywise.ui.theme.CardSurfaceColor
import com.example.studywise.ui.theme.CardTextColor
import kotlin.math.abs
import kotlin.math.roundToInt

private const val CARD_ASPECT_RATIO = 0.55f
private val CARD_PADDING_HORIZONTAL = 20.dp
private val CARD_PADDING_TOP = 20.dp
private val CARD_PADDING_BOTTOM = 32.dp
private val BLOCK_SPACING = 20.dp

private enum class MeasureSlot {
    NumberBadge,
    QuestionBlock,
    FlippableCard
}

private data class IndexedAnswer(
    val originalIndex: Int,
    val answer: AnswerUiState
)

@Composable
fun QuestionCard(
    state: QuestionCardUiState,
    onAction: (AnswerQuizScreenAction) -> Unit,
    modifier: Modifier = Modifier,
    questionColor: Color,
    questionNumber: Int,
    flipProgress: Float = 0f,
) {
    val indexedAnswers = state.answers.mapIndexed { index, answer -> IndexedAnswer(index, answer) }

    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO)
    ) { constraints ->
        val width = if (constraints.maxWidth == Constraints.Infinity) constraints.minWidth else constraints.maxWidth
        val height = (width / CARD_ASPECT_RATIO).roundToInt()

        if (width <= 0 || height <= 0) {
            return@SubcomposeLayout layout(0, 0) {}
        }

        val paddingHorizontalPx = CARD_PADDING_HORIZONTAL.roundToPx()
        val paddingTopPx = CARD_PADDING_TOP.roundToPx()
        val paddingBottomPx = CARD_PADDING_BOTTOM.roundToPx()
        val spacingPx = BLOCK_SPACING.roundToPx()
        val innerMaxHeight = (height - paddingTopPx - paddingBottomPx).coerceAtLeast(0)
        val innerMaxWidth = (width - (paddingHorizontalPx * 2)).coerceAtLeast(0)

        val childConstraints = Constraints(
            minWidth = 0,
            maxWidth = innerMaxWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )

        val numberBadgeHeight = subcompose(MeasureSlot.NumberBadge) {
            QuestionNumberBadge(questionNumber = questionNumber, questionColor = questionColor)
        }.first().measure(childConstraints).height

        val questionBlockHeight = subcompose(MeasureSlot.QuestionBlock) {
            QuestionPromptBlock(description = state.description)
        }.first().measure(childConstraints).height

        val answerHeights = indexedAnswers.map { indexed ->
            subcompose("answer_measure_${indexed.originalIndex}") {
                AnswerOptionCard(
                    onAction = onAction,
                    questionNumber = questionNumber,
                    state = indexed.answer,
                    selectedAnswer = state.selectedAnswer,
                    questionColor = questionColor,
                    label = ('A' + indexed.originalIndex).toString()
                )
            }.first().measure(childConstraints).height
        }

        val frontCount = maxAnswersThatFitFront(
            innerMaxHeight = innerMaxHeight,
            numberBadgeHeight = numberBadgeHeight,
            questionBlockHeight = questionBlockHeight,
            answerHeights = answerHeights,
            spacingPx = spacingPx
        )

        val frontAnswers = indexedAnswers.take(frontCount)
        val backAnswers = indexedAnswers.drop(frontCount)
        val hasBackSide = backAnswers.isNotEmpty()

        val fixedCardConstraints = Constraints.fixed(width, height)
        val effectiveFlipProgress = if (hasBackSide) flipProgress else 0f
        val depthPulse = (1f - abs((effectiveFlipProgress * 2f) - 1f)).coerceIn(0f, 1f)
        val flipScale = 1f + (0.15f * depthPulse)

        val flippablePlaceable = subcompose(MeasureSlot.FlippableCard) {
            DoubleSidedComposable(
                rotationX = 0f,
                rotationY = -180f * effectiveFlipProgress,
                rotationZ = 0f,
                cameraDistance = 28.dp.toPx(),
                flipType = FLIPTYPE.VERTICAL,
                front = {
                    QuestionCardFront(
                        description = state.description,
                        selectedAnswer = state.selectedAnswer,
                        onAction = onAction,
                        questionColor = questionColor,
                        questionNumber = questionNumber,
                        answers = frontAnswers,
                        canToggleSide = hasBackSide,
                        modifier = Modifier.graphicsLayer {
                            scaleX = flipScale
                            scaleY = flipScale
                        }
                    )
                },
                back = {
                    QuestionCardBack(
                        selectedAnswer = state.selectedAnswer,
                        onAction = onAction,
                        questionColor = questionColor,
                        questionNumber = questionNumber,
                        answers = backAnswers,
                        modifier = Modifier.graphicsLayer {
                            scaleX = flipScale
                            scaleY = flipScale
                        }
                    )
                }
            )
        }.first().measure(fixedCardConstraints)

        layout(flippablePlaceable.width, flippablePlaceable.height) {
            flippablePlaceable.placeRelative(0, 0)
        }
    }
}

private fun maxAnswersThatFitFront(
    innerMaxHeight: Int,
    numberBadgeHeight: Int,
    questionBlockHeight: Int,
    answerHeights: List<Int>,
    spacingPx: Int
): Int {
    // Base blocks for front: [number badge, question block]
    var usedHeight = numberBadgeHeight + spacingPx + questionBlockHeight
    if (usedHeight > innerMaxHeight) return 0

    var count = 0
    answerHeights.forEach { answerHeight ->
        val next = usedHeight + spacingPx + answerHeight
        if (next > innerMaxHeight) return count
        usedHeight = next
        count++
    }
    return count
}

@Composable
private fun QuestionCardFront(
    description: String,
    selectedAnswer: AnswerUiState?,
    onAction: (AnswerQuizScreenAction) -> Unit,
    questionColor: Color,
    questionNumber: Int,
    answers: List<IndexedAnswer>,
    canToggleSide: Boolean,
    modifier: Modifier = Modifier
) {
    QuestionCardScaffold(
        modifier = modifier,
        questionColor = questionColor
    ) {
        SubcomposeLayout(modifier = Modifier.fillMaxSize()) { constraints ->
            val availableWidth = constraints.maxWidth
            val availableHeight = constraints.maxHeight

            if (availableWidth <= 0 || availableHeight <= 0) {
                return@SubcomposeLayout layout(0, 0) {}
            }

            val childConstraints = Constraints(
                minWidth = 0,
                maxWidth = availableWidth,
                minHeight = 0,
                maxHeight = Constraints.Infinity
            )

            val rowPlaceable = subcompose("front_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuestionNumberBadge(questionNumber = questionNumber, questionColor = questionColor)
                    if (canToggleSide) {
                        SideToggleIconButton(
                            onClick = { onAction(AnswerQuizScreenAction.FlipToggled(questionNumber - 1)) },
                            label = "See more options"
                        )
                    }
                }
            }.first().measure(childConstraints)

            val answerPlaceables = answers.map { indexed ->
                subcompose("front_answer_${indexed.originalIndex}") {
                    AnswerOptionCard(
                        onAction = onAction,
                        questionNumber = questionNumber,
                        state = indexed.answer,
                        selectedAnswer = selectedAnswer,
                        questionColor = questionColor,
                        label = ('A' + indexed.originalIndex).toString()
                    )
                }.first().measure(childConstraints)
            }

            val promptMeasurePlaceable = subcompose("front_prompt_measure") {
                QuestionPromptBlock(description = description)
            }.first().measure(childConstraints)
            
            val spacingPx = BLOCK_SPACING.roundToPx()
            val usedWithoutPrompt = rowPlaceable.height + answerPlaceables.sumOf { it.height } + spacingPx * (answers.size + 1)
            val promptHeight = maxOf(promptMeasurePlaceable.height, (availableHeight - usedWithoutPrompt).coerceAtLeast(0))

            val promptPlaceable = subcompose("front_prompt") {
                QuestionPromptBlock(
                    description = description,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp)
                )
            }.first().measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = availableWidth,
                    minHeight = promptHeight,
                    maxHeight = promptHeight
                )
            )

            val totalHeight = rowPlaceable.height + promptPlaceable.height + answerPlaceables.sumOf { it.height } + spacingPx * (answers.size + 1)
            val layoutHeight = totalHeight.coerceAtMost(availableHeight)

            layout(availableWidth, layoutHeight) {
                var y = 0
                rowPlaceable.placeRelative(0, y)
                y += rowPlaceable.height + spacingPx

                promptPlaceable.placeRelative(0, y)
                y += promptPlaceable.height + spacingPx

                answerPlaceables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + spacingPx
                }
            }
        }
    }
}

@Composable
private fun QuestionCardBack(
    selectedAnswer: AnswerUiState?,
    onAction: (AnswerQuizScreenAction) -> Unit,
    questionColor: Color,
    questionNumber: Int,
    answers: List<IndexedAnswer>,
    modifier: Modifier = Modifier
) {
    QuestionCardScaffold(
        modifier = modifier,
        questionColor = questionColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SideToggleIconButton(
                onClick = { onAction(AnswerQuizScreenAction.FlipToggled(questionNumber - 1)) },
                label = "See front side"
            )
        }
        answers.forEach { indexed ->
            AnswerOptionCard(
                onAction = onAction,
                questionNumber = questionNumber,
                state = indexed.answer,
                selectedAnswer = selectedAnswer,
                questionColor = questionColor,
                label = ('A' + indexed.originalIndex).toString()
            )
        }
    }
}

@Composable
private fun SideToggleIconButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = CardSurfaceColor
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = CardSurfaceColor,
                style = MaterialTheme.typography.labelLarge.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0x33000000),
                        offset = Offset(1f, 1f),
                        blurRadius = 8f
                    )
                )
            )
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Rounded.Autorenew,
                contentDescription = "Switch card side",
                tint = CardSurfaceColor
            )
        }
    }
}

@Composable
private fun QuestionCardScaffold(
    questionColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = questionColor),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                    .padding(
                        start = CARD_PADDING_HORIZONTAL,
                        top = CARD_PADDING_TOP,
                        end = CARD_PADDING_HORIZONTAL,
                        bottom = CARD_PADDING_BOTTOM
                    ),
            verticalArrangement = Arrangement.spacedBy(BLOCK_SPACING)
        ) {
            content()
        }
    }
}

@Composable
private fun QuestionNumberBadge(
    questionNumber: Int,
    questionColor: Color
) {
    val paddedQuestionNumber = questionNumber.toString().padStart(2, '0')
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .background(CardSurfaceColor, shape = RoundedCornerShape(12.dp))
            .innerShadow(
                shape = RoundedCornerShape(12.dp),
                shadow = Shadow(
                    radius = 4.dp,
                    spread = 2.dp,
                    color = Color(0x14000000),
                    offset = DpOffset(1.dp, 1.dp)
                )
            )
    ) {
        Text(
            text = paddedQuestionNumber,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = questionColor
            )
        )
    }
}

@Composable
private fun QuestionPromptBlock(
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = CardSurfaceColor,
                shape = RoundedCornerShape(12.dp)
            )
            .innerShadow(
                shape = RoundedCornerShape(12.dp),
                shadow = Shadow(
                    radius = 4.dp,
                    spread = 2.dp,
                    color = Color(0x14000000),
                    offset = DpOffset(1.dp, 1.dp)
                )
            )
            .defaultMinSize(minHeight = 140.dp)
            .padding(20.dp)
    ) {
        Text(
            text = description,
            textAlign = TextAlign.Center,
            color = CardTextColor,
            style = MaterialTheme.typography.titleMedium
        )
    }
}