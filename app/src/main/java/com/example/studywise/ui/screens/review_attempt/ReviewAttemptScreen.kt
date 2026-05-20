package com.example.studywise.ui.screens.review_attempt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studywise.ui.components.stack_screen.StackScreen
import com.example.studywise.ui.theme.CardSurfaceColor
import com.example.studywise.ui.theme.LogoGreen
import com.example.studywise.ui.theme.LogoOrange
import com.example.studywise.ui.theme.LogoPink
import com.example.studywise.ui.theme.LogoTeal
import com.example.studywise.viewmodels.ReviewAttemptViewModel
private val CorrectOptionColor = Color(0xFF2E7D32).copy(alpha = 0.16f)
private val WrongChosenOptionColor = Color(0xFFC62828).copy(alpha = 0.16f)
@Composable
fun ReviewAttemptScreen(
    modifier: Modifier = Modifier,
    viewModel: ReviewAttemptViewModel,
    goBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewAttemptScreenContent(
        modifier = modifier,
        state = state,
        goBack = goBack,
        onScrollChanged = viewModel::onScrollChanged,
        onToggleOption = viewModel::onToggleOption,
    )
}
@Composable
fun ReviewAttemptScreenContent(
    modifier: Modifier = Modifier,
    state: ReviewAttemptUiState,
    goBack: () -> Unit = {},
    onScrollChanged: (Int) -> Unit = {},
    onToggleOption: (questionId: String, optionId: String) -> Unit = { _, _ -> },
) {
    val lazyListState = rememberLazyListState(initialFirstVisibleItemScrollOffset = state.currentScroll)
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            if (lazyListState.firstVisibleItemIndex > 0) {
                1
            } else {
                lazyListState.firstVisibleItemScrollOffset
            }
        }.collect { onScrollChanged(it) }
    }
    StackScreen(
        title = "Review Attempt",
        modifier = modifier,
        transitionProgress = if (state.isLoading) 0f else 1f,
        currentScroll = state.currentScroll,
        onBackClick = goBack,
    ) { contentModifier ->
        LazyColumn(
            modifier = contentModifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            state = lazyListState,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.quizName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${state.questions.size} questions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(items = state.questions, key = { it.id }) { question ->
                ReviewQuestionCard(
                    question = question,
                    onToggleOption = onToggleOption,
                )
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = goBack,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    Text(
                        "Finish Review",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
@Composable
private fun ReviewQuestionCard(
    question: ReviewQuestionUiState,
    onToggleOption: (questionId: String, optionId: String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = indexedTitleColor(question.number - 1),
                modifier = Modifier.innerShadow(
                    shape = RoundedCornerShape(12.dp),
                    shadow = androidx.compose.ui.graphics.shadow.Shadow(
                        radius = 4.dp,
                        spread = 2.dp,
                        color = Color(0x14000000),
                        offset = DpOffset(1.dp, 1.dp)
                    )
                )
            ) {
                Text(
                    text = "Question ${question.number}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0x33000000),
                            offset = Offset(1f, 1f),
                            blurRadius = 8f
                        )
                    ),
                    color = CardSurfaceColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Text(
                text = question.description,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            question.options.forEach { option ->
                ReviewOptionCard(
                    questionId = question.id,
                    option = option,
                    onToggleOption = onToggleOption,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Explanation",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = question.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
@Composable
private fun ReviewOptionCard(
    questionId: String,
    option: ReviewOptionUiState,
    onToggleOption: (questionId: String, optionId: String) -> Unit,
) {
    val backgroundColor = when {
        option.isCorrect -> CorrectOptionColor
        option.isChosen -> WrongChosenOptionColor
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val outlineColor = when {
        option.isCorrect -> Color(0xFF2E7D32).copy(alpha = 0.8f)
        option.isChosen -> Color(0xFFC62828).copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    }

    val expansionDuration = 300
    val fadeInDuration = 500
    val fadeOutDuration = 300

    val iconRotation by animateFloatAsState(
        targetValue = if (option.isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = expansionDuration),
        label = "iconRotation"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, outlineColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleOption(questionId, option.id) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (option.isCorrect) {
                    OptionTag(text = "Correct", color = outlineColor)
                }
                if (option.isChosen) {
                    OptionTag(text = "Chosen", color = outlineColor)
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (option.isExpanded) "Collapse option" else "Expand option",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(iconRotation)
                )
            }

            AnimatedVisibility(
                visible = option.isExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = expansionDuration)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = fadeInDuration, delayMillis = expansionDuration - 100)
                ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = fadeOutDuration)
                ) + shrinkVertically(
                    animationSpec = tween(durationMillis = expansionDuration, delayMillis = fadeOutDuration - 100)
                ),
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = option.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
@Composable
private fun OptionTag(
    text: String,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, color),
        modifier = Modifier.innerShadow(
            shape = RoundedCornerShape(999.dp),
            shadow = androidx.compose.ui.graphics.shadow.Shadow(
                radius = 3.dp,
                spread = 1.dp,
                color = Color(0x14000000),
                offset = DpOffset(1.dp, 1.dp)
            )
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(0x33000000),
                    offset = Offset(1f, 1f),
                    blurRadius = 6f
                )
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
@Composable
private fun indexedTitleColor(index: Int): Color {
    val colors = listOf(
        LogoPink,
        LogoOrange,
        LogoTeal,
        LogoGreen,
    )
    return colors[index % colors.size]
}
