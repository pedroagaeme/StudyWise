package com.example.studywise.ui.screens.tabs.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.studywise.ui.components.quiz_card.QuizCard
import com.example.studywise.ui.theme.AppTheme
import com.example.studywise.viewmodels.SearchScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchScreenViewModel = hiltViewModel(),
    innerPadding: PaddingValues,
    pushQuizDetailsRoute: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val onAction = viewModel::onAction

    LaunchedEffect(uiState.pendingEffect) {
        uiState.pendingEffect?.let { effect ->
            when (effect) {
                is SearchScreenEffect.NavigateToQuizDetails -> pushQuizDetailsRoute(effect.quizId)
            }
            viewModel.effectConsumed()
        }
    }

    SearchScreenContent(
        modifier = modifier,
        innerPadding = innerPadding,
        state = uiState,
        onAction = onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreenContent(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    state: SearchScreenUiState,
    onAction: (SearchScreenAction) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                CenterAlignedTopAppBar(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = maxOf(0.dp, innerPadding.calculateTopPadding() - 16.dp)),
                    title = {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { onAction(SearchScreenAction.OnSearchQueryChange(it)) },
                            modifier = Modifier
                                .fillMaxWidth(),
                            placeholder = { Text("Search your quizzes...") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null)
                            },
                            shape = MaterialTheme.shapes.medium,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
            }

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SearchStepTransition",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    SearchStep.LOADING -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    SearchStep.EMPTY -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = innerPadding.calculateBottomPadding()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                textAlign = TextAlign.Center,
                                text = if (state.searchQuery.isEmpty()) "Start searching for quizzes" else "No results found for \"${state.searchQuery}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SearchStep.HAS_CONTENT -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                bottom = innerPadding.calculateBottomPadding() + 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.filteredQuizzes, key = { it.id }) { quiz ->
                                QuizCard(
                                    quiz = quiz,
                                    onClick = { onAction(SearchScreenAction.OnQuizCardClick(quiz.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    AppTheme {
        SearchScreenContent(
            state = SearchScreenUiState(currentStep = SearchStep.HAS_CONTENT),
            onAction = {}
        )
    }
}
