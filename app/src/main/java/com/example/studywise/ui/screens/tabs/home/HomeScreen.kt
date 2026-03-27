package com.example.studywise.ui.screens.tabs.home
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.studywise.ui.screens.tabs.home.components.CollectionHeader
import com.example.studywise.ui.components.quiz_card.QuizCard
import com.example.studywise.ui.screens.tabs.home.components.SectionHeader
import com.example.studywise.ui.components.model.Collection
import com.example.studywise.ui.components.model.Quiz
import com.example.studywise.ui.theme.AppTheme
import com.example.studywise.viewmodels.HomeScreenViewModel
import kotlin.math.max

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
    val listState = rememberLazyListState()
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        LazyColumn(
            state = listState,
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
            item {
                RecentQuizzesBlock(quizzes = state.recentQuizzes)
            }

            // Collections Section
            item {
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
                        modifier = Modifier.fillMaxWidth(),
                        listState = listState,
                        itemToScroll = index + 3,
                        mostRecentExpandedItemIndex =
                            if (state.mostRecentExpandedCollectionIndex != null)
                                state.mostRecentExpandedCollectionIndex + 3
                            else
                                null
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
    cardSpacing: Int = 12,
    expandingSpeed: Float = 1.5f,
    transitionSpeed: Int = 500,
    listState: LazyListState,
    itemToScroll: Int = 0,
    mostRecentExpandedItemIndex: Int? = null
) {
    val transition = updateTransition(targetState = expanded, label = "expandTransition")
    val progress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = transitionSpeed) },
        label = "expandProgress"
    ) { if (it) 1f else 0f }


    // Animate vertical space for quizzes
    val quizzes = collection.quizzes
    val maxHeight = (quizzes.size * cardHeight + (quizzes.size) * cardSpacing).dp
    val animatedHeight = if (progress > 0f) ((maxHeight * progress * expandingSpeed).coerceIn(0.dp, maxHeight)) else 0.dp

    LaunchedEffect(progress, mostRecentExpandedItemIndex) {
        if (progress > 0.75f && (itemToScroll == mostRecentExpandedItemIndex)) {
            listState.animateScrollToItem(itemToScroll)
        }
    }

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
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                quizzes.forEachIndexed { quizIndex, quiz ->
                    AnimatedQuizCard(
                        quiz = quiz,
                        progress = progress,
                        quizIndex = quizIndex,
                        totalQuizzes = quizzes.count(),
                        cardHeight = cardHeight
                    )
                    Spacer(modifier = Modifier.height(cardSpacing.dp))
                }
            }
        }
    }
}

@Composable
fun RecentQuizzesBlock(
    quizzes: List<Quiz>,
    modifier: Modifier = Modifier,
    cardHeight: Int = 100,
    cardSpacing: Int = 12,
    animationDuration: Int = 600
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(quizzes) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration)
        )
    }

    Column(modifier = modifier) {
        quizzes.forEachIndexed { quizIndex, quiz ->
            AnimatedQuizCard(
                quiz = quiz,
                progress = progress.value,
                quizIndex = quizIndex,
                totalQuizzes = quizzes.size,
                cardHeight = cardHeight
            )
            Spacer(modifier = Modifier.height(cardSpacing.dp))
        }
    }
}
@Composable
fun AnimatedQuizCard(
    modifier: Modifier = Modifier,
    quiz: Quiz,
    progress: Float,
    quizIndex: Int = 0,
    totalQuizzes: Int = 1,
    speedDifference: Float = 0.15f,
    cardHeight: Int = 100
) {
    val incrementalSpeed = (totalQuizzes - 1f) * speedDifference
    val slideProgress = ((1f + incrementalSpeed) * progress - quizIndex * speedDifference).coerceIn(0f, 1f)
    val offsetX = (1f - slideProgress) * 100f
    Column(
        modifier = modifier
            .offset(x = offsetX.dp)
            .alpha(slideProgress)
    ) {
        QuizCard(
            quiz.copy(
                averageScore = (quiz.averageScore ?: 0f) * slideProgress
            ),
            Modifier.height(cardHeight.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AppTheme {
        HomeScreenContent(state = HomeScreenUiState(), onAction = {})
    }
}
