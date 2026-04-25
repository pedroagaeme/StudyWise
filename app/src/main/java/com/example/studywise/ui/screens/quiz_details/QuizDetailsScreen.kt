package com.example.studywise.ui.screens.quiz_details

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studywise.ui.components.icon_button_with_offset.IconButtonOffsetDirection
import com.example.studywise.ui.components.icon_button_with_offset.IconButtonWithOffset
import com.example.studywise.ui.components.ProgressIndicatorBox
import com.example.studywise.ui.components.custom_progress_indicators.CustomLinearProgressIndicator
import com.example.studywise.viewmodels.QuizDetailsViewModel

@Composable
fun QuizDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizDetailsViewModel,
    goBack: () -> Unit = {},
    onContinueAttemptClick: (String) -> Unit,
    onCreateNewAttemptClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    QuizDetailsScreenContent(
        modifier = modifier,
        state = state,
        goBack = goBack,
        onContinueAttemptClick = onContinueAttemptClick,
        onCreateNewAttemptClick = onCreateNewAttemptClick
    )
}

@Composable
fun QuizDetailsScreenContent(
    modifier: Modifier = Modifier,
    state: QuizDetailsUiState,
    goBack: () -> Unit = {},
    onContinueAttemptClick: (String) -> Unit,
    onCreateNewAttemptClick: (String) -> Unit,
) {
    val transitionProgress by animateFloatAsState(
        targetValue = if (state.isLoading) 0f else 1f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "quizDetailsEnter"
    )
    var selectedAttemptIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(state.attempts.size) {
        if (state.attempts.isEmpty()) {
            selectedAttemptIndex = 0
        } else if (selectedAttemptIndex > state.attempts.lastIndex) {
            selectedAttemptIndex = 0
        }
    }
    val currentAttemptPosition = if (state.attempts.isEmpty()) 0 else selectedAttemptIndex + 1
    val selectedAttempt = state.attempts.getOrNull(selectedAttemptIndex)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    scaleX = 0.92f + (0.08f * transitionProgress)
                    scaleY = 0.92f + (0.08f * transitionProgress)
                }
                .alpha(transitionProgress),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonWithOffset(
                    onClick = goBack,
                    offsetDirection = IconButtonOffsetDirection.Left
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Quiz Details",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                IconButtonWithOffset(
                    onClick = {},
                    offsetDirection = IconButtonOffsetDirection.Right
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options"
                    )
                }
            }

            // Header Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = state.quizName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = buildAnnotatedString {
                        append("from ")
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                            append(state.collectionName)
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Attempts Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButtonWithOffset(
                        onClick = {
                            if (state.attempts.isNotEmpty()) {
                                selectedAttemptIndex = (selectedAttemptIndex - 1 + state.attempts.size) % state.attempts.size
                            }
                        },
                        offsetDirection = IconButtonOffsetDirection.None
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous attempt",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "attempt $currentAttemptPosition of ${state.attempts.size}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButtonWithOffset(
                        onClick = {
                            if (state.attempts.isNotEmpty()) {
                                selectedAttemptIndex = (selectedAttemptIndex + 1) % state.attempts.size
                            }
                        },
                        offsetDirection = IconButtonOffsetDirection.None
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next attempt",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Attempt Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedAttempt == null) {
                        Text(
                            text = "No attempts yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedAttempt.hasCompletedAttempt) "Finished attempt" else "Current attempt",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = selectedAttempt.timeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        if (selectedAttempt.hasCompletedAttempt) {
                            val finishedScoreProgress = if (state.questionCount > 0) {
                                ((selectedAttempt.score ?: 0).toFloat() / state.questionCount.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ProgressIndicatorBox(
                                    progress = finishedScoreProgress,
                                    boxSize = 84.dp
                                )
                                Column {
                                    Text(
                                        text = "Score",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "${selectedAttempt.score ?: 0}/${state.questionCount}",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CustomLinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    progress = state.attemptProgress(selectedAttempt)
                                )
                                Text(
                                    text = "${selectedAttempt.remainingQuestions} question(s) left",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (selectedAttempt.hasRemainingQuestions) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onContinueAttemptClick(state.quizId) },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Continue attempt", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCreateNewAttemptClick(state.quizId) },
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Start new attempt", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            Text(
                text = "Quiz Informations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )

            // Info Area: one row with two columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    InfoItem(
                        title = "Number of questions",
                        value = "${state.questionCount} questions"
                    )
                    ScoreItem(
                        title = "Average Score",
                        progress = state.averageProgress ?: 0f,
                        value = state.averageScore?.toInt()?.toString() ?: "0"
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    InfoItem(
                        title = "Creation date",
                        value = state.createdAtLabel
                    )
                    ScoreItem(
                        title = "Best Score",
                        progress = state.bestProgress ?: 0f,
                        value = state.bestScore?.toString() ?: "0"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun InfoItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoTileCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun ScoreItem(
    modifier: Modifier = Modifier,
    title: String,
    progress: Float,
    value: String
) {
    InfoTileCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(title)
                    }
                    append(": ")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(value)
                    }
                },
                style = MaterialTheme.typography.titleSmall
            )
            ProgressIndicatorBox(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                progress = progress,
                boxSize = 84.dp
            )
        }

    }
}
