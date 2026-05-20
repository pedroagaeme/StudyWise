package com.example.studywise.ui.screens.answer_quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studywise.ui.components.custom_progress_indicators.CustomLinearProgressIndicator
import com.example.studywise.ui.components.stack_screen.StackScreen
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.QuestionPile
import com.example.studywise.viewmodels.AnswerQuizScreenViewModel



@Composable
fun AnswerQuizScreen(
    modifier: Modifier = Modifier,
    viewModel: AnswerQuizScreenViewModel = hiltViewModel(),
    goBack: () -> Unit = {},
    onFinishQuiz: (quizId: String, attemptId: String) -> Unit = { _, _ -> },
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction

    LaunchedEffect(state.pendingEffect) {
        state.pendingEffect.let { effect ->
            when (effect) {
                is AnswerQuizUiEffect.FinishQuiz -> onFinishQuiz(effect.quizId, effect.attemptId)
                else -> Unit
            }
        }
    }
    AnswerQuizScreenContent(
        modifier = modifier,
        state = state,
        onAction = onAction,
        onScrollChanged = viewModel::onScrollChanged,
        goBack = goBack
    )
}

@Composable
fun AnswerQuizScreenContent(
    modifier: Modifier = Modifier,
    state: AnswerQuizScreenUiState,
    onAction: (AnswerQuizScreenAction) -> Unit,
    onScrollChanged: (Int) -> Unit = {},
    goBack: () -> Unit = {}
) {
    val questionCount = state.questionList.size
    val currentQuestionNumber = when {
        questionCount == 0 -> 0
        state.targetIndex >= questionCount -> questionCount
        else -> state.targetIndex + 1
    }
    val progress by animateFloatAsState(
        targetValue = if (questionCount == 0) 0f else currentQuestionNumber.toFloat() / questionCount.toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "answerQuizProgress"
    )

    // Provide a simple enter transition progress (0f -> 1f) so StackScreen animates on compose
    val enterProgressState = remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        enterProgressState.value = 1f
    }

    val scrollState = rememberScrollState(state.currentScroll)
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }.collect { currentScroll ->
            onScrollChanged(currentScroll)
        }
    }

    StackScreen(
        title = "Answer Quiz",
        modifier = modifier,
        onBackClick = goBack,
        transitionProgress = enterProgressState.value,
        currentScroll = state.currentScroll
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
                .padding(WindowInsets.navigationBars.asPaddingValues()),
        ) {
            Text(
                text = state.quizName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Question $currentQuestionNumber of $questionCount",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            CustomLinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = progress
            )
            Spacer(modifier = Modifier.height(40.dp))
            QuestionPile(
                state = state,
                onAction = onAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
