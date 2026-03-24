package com.example.studywise.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route: NavKey {
    @Serializable
    data object Intro : Route, NavKey
    @Serializable
    data object Login : Route, NavKey
    @Serializable
    data object Tabs : Route, NavKey
    @Serializable
    data object CreateQuiz : Route, NavKey

    @Serializable
    data class AnswerQuiz(val quizId: String) : Route, NavKey

}

