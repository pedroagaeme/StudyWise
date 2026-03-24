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
import com.example.studywise.ui.screens.tabs.MainScreen
import com.example.studywise.viewmodels.AnswerQuizScreenViewModel

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
                        LoginScreen(replaceWithTabsRoute = { backStack.replaceTop(Route.Tabs) })
                    }
                }
                is Route.Tabs -> {
                    NavEntry(key) {
                        MainScreen(pushCreateQuizRoute = { backStack.add(Route.CreateQuiz)})
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
                            creationCallback = { factory -> factory.create(key.quizId) }
                        )
                        AnswerQuizScreen(
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