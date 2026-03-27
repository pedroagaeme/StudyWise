package com.example.studywise.ui.screens.tabs.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.studywise.ui.screens.tabs.home.components.CollectionHeader
import com.example.studywise.ui.components.quiz_card.QuizCard
import com.example.studywise.ui.screens.tabs.home.components.SectionHeader
import com.example.studywise.ui.components.model.Collection
import com.example.studywise.ui.components.model.Quiz
import com.example.studywise.ui.theme.AppTheme
import com.example.studywise.viewmodels.CreateQuizScreenViewModel
import com.example.studywise.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = hiltViewModel(),
    innerPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val onAction = viewModel::onAction

    HomeScreenContent(
        innerPadding = innerPadding,
        state = uiState,
        onAction = onAction
    )
}
@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    state: HomeScreenUiState,
    onAction: (HomeScreenAction) -> Unit
) {

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = innerPadding.calculateBottomPadding() + 8.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recent Section
            item {
                SectionHeader("Recent")
            }
            items(state.recentQuizzes) { quiz ->
                QuizCard(quiz)
            }

            // Collections Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Collections")
            }

            state.collections.forEachIndexed { index, collection ->
                // Animated expansion/collapse for quizzes
                item(key = "${collection.id}_quizzes") {
                    CollectionExpandableBlock(
                        collection = collection,
                        expanded = state.collectionsExpandableState[index],
                        onExpandClick = {
                            onAction(HomeScreenAction.ToggleCollectionExpandableState(index))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionExpandableBlock(
    collection: Collection,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardHeight: Int = 100,
    cardSpacing: Int = 12
) {
    val transition = updateTransition(targetState = expanded, label = "expandTransition")
    val progress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 500) },
        label = "expandProgress"
    ) { if (it) 1f else 0f }

    // Animate vertical space for quizzes
    val quizzes = collection.quizzes
    val maxHeight = (quizzes.size * cardHeight + (quizzes.size) * cardSpacing).dp
    val animatedHeight = if (progress > 0f) ((maxHeight * progress * 1.5f).coerceIn(0.dp, maxHeight)) else 0.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        CollectionHeader(
            collection = collection,
            progress = progress,
            onExpandClick = onExpandClick
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                quizzes.forEachIndexed { quizIndex, quiz ->
                    // Each quiz slides in/out horizontally based on progress
                    val slideProgress = (1.3f * progress - quizIndex * 0.15f).coerceIn(0f, 1f)
                    val offsetX = (1f - slideProgress) * 100f
                    Column(
                        modifier = Modifier
                            .offset(x = offsetX.dp)
                            .alpha(slideProgress)
                    ) {
                        QuizCard(
                            quiz.copy(
                                averageScore = (quiz.averageScore?:0f) * slideProgress
                            ),
                            Modifier.height(cardHeight.dp)
                        )
                        Spacer(modifier = Modifier.height(cardSpacing.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AppTheme {
        HomeScreenContent(state = HomeScreenUiState(), onAction = {})
    }
}
