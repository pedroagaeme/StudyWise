package com.example.studywise.ui.screens.tabs.home
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.studywise.data.QuizCollectionDto
import com.example.studywise.data.QuizDto
import com.example.studywise.ui.screens.tabs.home.components.CollectionHeader
import com.example.studywise.ui.components.quiz_card.QuizCard
import com.example.studywise.ui.screens.tabs.home.components.SectionHeader
import com.example.studywise.ui.theme.AppTheme
import com.example.studywise.viewmodels.HomeScreenViewModel
import kotlin.math.max

@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = hiltViewModel(),
    innerPadding: PaddingValues,
    pushQuizDetailsRoute: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val onAction = viewModel::onAction

    LaunchedEffect(uiState.pendingEffect) {
        uiState.pendingEffect?.let { effect ->
            when (effect) {
                is HomeScreenEffect.ScrollBy -> {
                    uiState.listState.animateScrollBy(
                        effect.offset,
                        tween(700)
                    ) }
                is HomeScreenEffect.NavigateToQuizDetails -> {
                    pushQuizDetailsRoute(effect.quizId)
                }
            }
            viewModel.effectConsumed()
        }
    }

    HomeScreenContent(
        innerPadding = innerPadding,
        state = uiState,
        onAction = onAction,
    )
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    state: HomeScreenUiState,
    onAction: (HomeScreenAction) -> Unit,
) {
    val horizontalPaddingModifier = Modifier.padding(horizontal = 16.dp)
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        LazyColumn(
            state = state.listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = innerPadding.calculateBottomPadding() + 8.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recent Section
            item {
                SectionHeader("Recent", modifier = horizontalPaddingModifier)
            }
            item {
                RecentQuizzesBlock(
                    quizzes = state.recentQuizzes,
                    onAction = onAction,
                )
            }

            // Collections Section
            item {
                SectionHeader("Collections", modifier = horizontalPaddingModifier)
            }

            state.collections.forEach { collection ->
                // Animated expansion/collapse for quizzes
                item(key = "${collection.id}_quizzes") {
                    CollectionExpandableBlock(
                        collection = collection,
                        expanded = state.collectionsExpandableState[collection.id] ?: false,
                        onExpandClick = {
                            onAction(HomeScreenAction.OnToggleCollectionExpandableState(collection.id))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionExpandableBlock(
    collection: QuizCollectionDto,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAction: (HomeScreenAction) -> Unit,
    cardHeight: Int = 100,
    expandingSpeed: Float = 1.5f,
    contentTransitionDuration: Int = 500
) {
    val transition = updateTransition(targetState = expanded, label = "expandTransition")
    val progress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = contentTransitionDuration) },
        label = "expandProgress"
    ) { if (it) 1f else 0f }
    val scrollOffset = remember { mutableFloatStateOf(0f) }

    // Animate vertical space for quizzes
    val cardVerticalPaddingValue = 6
    val quizzes = collection.quizzes
    val maxHeight = (quizzes.size * cardHeight + (quizzes.size) * 2 * cardVerticalPaddingValue).dp
    val animatedHeight = if (progress > 0f) ((maxHeight * progress * expandingSpeed).coerceIn(0.dp, maxHeight)) else 0.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned({
                val offset = it.positionInParent().y
                val newOffset = max(0f, offset - 100f)
                onAction(HomeScreenAction.OnScrollOffsetChanged(collection.id, newOffset))
            })
    ) {
        CollectionHeader(
            modifier = Modifier.padding(horizontal = 16.dp),
            collection = collection,
            progress = progress,
            onExpandClick = onExpandClick
        )
        Spacer(modifier = Modifier.height(8 .dp))
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
                        modifier = Modifier.padding(start = 8.dp),
                        quiz = quiz.copy(
                            collectionName = null
                        ),
                        progress = progress,
                        quizIndex = quizIndex,
                        totalQuizzes = quizzes.count(),
                        cardHeight = cardHeight,
                        cardPadding = PaddingValues(horizontal = 16.dp, vertical = cardVerticalPaddingValue.dp),
                        onQuizCardClick = {
                            onAction(HomeScreenAction.OnQuizCardClick(quiz.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentQuizzesBlock(
    quizzes: List<QuizDto>,
    modifier: Modifier = Modifier,
    cardHeight: Int = 100,
    animationDuration: Int = 600,
    onAction: (HomeScreenAction) -> Unit
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
                cardHeight = cardHeight,
                onQuizCardClick = {
                    onAction(HomeScreenAction.OnQuizCardClick(quiz.id))
                }
            )
        }
    }
}
@Composable
fun AnimatedQuizCard(
    modifier: Modifier = Modifier,
    quiz: QuizDto,
    progress: Float,
    quizIndex: Int = 0,
    totalQuizzes: Int = 1,
    speedDifference: Float = 0.15f,
    cardHeight: Int = 100,
    cardPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    onQuizCardClick: () -> Unit
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
                averageScore = if (quiz.averageScore == null) null else quiz.averageScore * slideProgress,
            ),
            Modifier
                .padding(cardPadding)
                .height(cardHeight.dp)
            ,
            onClick = onQuizCardClick
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
