package com.example.studywise.ui.screens.answer_quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studywise.ui.screens.answer_quiz.components.question_pile.QuestionPile
import com.example.studywise.viewmodels.AnswerQuizScreenViewModel



@Composable
fun AnswerQuizScreen(
    modifier: Modifier = Modifier,
    viewModel: AnswerQuizScreenViewModel = hiltViewModel(),
    goBack: () -> Unit = {},
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction

    AnswerQuizScreenContent(
        modifier = modifier,
        state = state,
        onAction = onAction
    )
}

@Composable
fun AnswerQuizScreenContent(
    modifier: Modifier = Modifier,
    state: AnswerQuizScreenUiState,
    onAction: (AnswerQuizScreenAction) -> Unit
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
    ) {
        QuestionPile(
            state = state,
            onAction = onAction,
            modifier = modifier
        )
    }
}
