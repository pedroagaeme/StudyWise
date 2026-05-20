package com.example.studywise.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.studywise.ui.screens.InitialScreen
import com.example.studywise.ui.screens.answer_quiz.AnswerQuizScreen
import com.example.studywise.ui.screens.create_quiz.CreateQuizScreen
import com.example.studywise.ui.screens.login.LoginScreen
import com.example.studywise.ui.screens.quiz_details.QuizDetailsScreen
import com.example.studywise.ui.screens.review_attempt.ReviewAttemptScreen
import com.example.studywise.ui.screens.tabs.MainScreen
import com.example.studywise.viewmodels.AnswerQuizScreenViewModel
import com.example.studywise.viewmodels.QuizDetailsViewModel
import com.example.studywise.viewmodels.ReviewAttemptViewModel

private fun <T> MutableList<T>.replaceTop(item: T) {
    if (isNotEmpty()) removeAt(lastIndex)
    add(item)
}

private fun <T> MutableList<T>.pop() {
    removeAt(lastIndex)
}


@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
){
    val backStack = rememberNavBackStack(Route.Intro)

    NavDisplay(
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = backStack,
        entryProvider = { key ->
            when(key) {
                is Route.Intro -> {
                    NavEntry(key) {
                        InitialScreen(replaceWithLoginRoute = {  backStack.replaceTop(Route.Login)})
                    }
                }
                is Route.Login -> {
                    NavEntry(key) {
                        LoginScreen(
                            replaceWithTabsRoute = { backStack.replaceTop(Route.Tabs) },
                        )
                    }
                }
                is Route.Tabs -> {
                    NavEntry(key) {
                        MainScreen(
                            pushCreateQuizRoute = { backStack.add(Route.CreateQuiz)},
                            pushQuizDetailsRoute = { quizId: String ->
                                backStack.add(Route.QuizDetails(quizId))
                            },
                            pushAnswerQuizRoute = { quizId: String ->
                                backStack.add(Route.AnswerQuiz(quizId))
                            }
                        )
                    }
                }
                is Route.QuizDetails -> {
                    NavEntry(key) {
                        val viewModel = hiltViewModel<QuizDetailsViewModel, QuizDetailsViewModel.Factory>(
                            creationCallback = { factory -> factory.create(key.quizId) }
                        )
                        QuizDetailsScreen(
                            goBack = { backStack.pop() },
                            viewModel = viewModel,
                            onContinueAttemptClick = { quizId ->
                                backStack.add(Route.AnswerQuiz(quizId = quizId, forceNewAttempt = false))
                            },
                            onCreateNewAttemptClick = { quizId ->
                                backStack.add(Route.AnswerQuiz(quizId = quizId, forceNewAttempt = true))
                            },
                            onReviewAttemptClick = { quizId, attemptId ->
                                backStack.add(Route.ReviewAttempt(quizId = quizId, attemptId = attemptId))
                            }
                        )
                    }
                }
                is Route.CreateQuiz -> {
                    NavEntry(key) {
                        CreateQuizScreen(
                            goBack = { backStack.pop() },
                            replaceWithAnswerQuizRoute = { quizId ->
                                backStack.replaceTop(Route.AnswerQuiz(quizId))
                            }
                        )
                    }
                }
                is Route.AnswerQuiz -> {
                    NavEntry(key) {
                        val viewModel = hiltViewModel<AnswerQuizScreenViewModel, AnswerQuizScreenViewModel.Factory>(
                            creationCallback = { factory -> factory.create(key.quizId, key.forceNewAttempt) }
                        )
                        AnswerQuizScreen(
                            goBack = { backStack.pop() },
                            viewModel = viewModel,
                            onFinishQuiz = { quizId, attemptId ->
                                backStack.replaceTop(Route.ReviewAttempt(quizId = quizId, attemptId = attemptId))
                            }
                        )
                    }
                }
                is Route.ReviewAttempt -> {
                    NavEntry(key) {
                        val viewModel = hiltViewModel<ReviewAttemptViewModel, ReviewAttemptViewModel.Factory>(
                            creationCallback = { factory -> factory.create(key.quizId, key.attemptId) }
                        )
                        ReviewAttemptScreen(
                            goBack = { backStack.pop() },
                            viewModel = viewModel
                        )
                    }
                }
                else -> error("Unknown route: $key")
            }
        }
    )
}