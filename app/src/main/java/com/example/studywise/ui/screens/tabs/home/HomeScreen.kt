package com.example.studywise.ui.screens.tabs.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studywise.ui.screens.tabs.home.components.CollectionHeader
import com.example.studywise.ui.components.quiz_card.QuizCard
import com.example.studywise.ui.screens.tabs.home.components.SectionHeader
import com.example.studywise.ui.components.model.Collection
import com.example.studywise.ui.components.model.Quiz
import com.example.studywise.ui.theme.AppTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val recentQuizzes = remember {
        listOf(
            Quiz("1", "Organic Chemistry", "2m ago", "University", questionCount = 18, averageScore = 14f),
            Quiz("2", "Spanish Vocabulary - Unit 5", "47m ago", "Languages", questionCount = 25, averageScore = 20f),
            Quiz("7", "System Design Fundamentals", "3h ago", "Career Prep", questionCount = 12, averageScore = 8f)
        )
    }

    val collections = remember {
        listOf(
            Collection("c1", "University", listOf(
                Quiz("3", "Advanced Math", "Yesterday", questionCount = 30, averageScore = 24f),
                Quiz("4", "Quantum Physics", "3 days ago", questionCount = 16, averageScore = 11f),
                Quiz("8", "Cell Biology Lab Prep", "5 days ago", questionCount = 10, averageScore = 9f)
            )),
            Collection("c2", "Personal", listOf(
                Quiz("5", "World Geography", "Last week", questionCount = 22, averageScore = 15f),
                Quiz("6", "Cooking Basics", "2 weeks ago", questionCount = 14, averageScore = 6f),
                Quiz("9", "Chess Openings", "Mar 12", questionCount = 9, averageScore = 7f)
            ))
        )
    }

    var expandedCollectionIds by remember { mutableStateOf(setOf<String>()) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recent Section
            item {
                SectionHeader("Recent")
            }
            items(recentQuizzes) { quiz ->
                QuizCard(quiz)
            }

            // Collections Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Collections")
            }

            collections.forEach { collection ->
                item(key = collection.id) {
                    CollectionHeader(
                        collection = collection,
                        isExpanded = expandedCollectionIds.contains(collection.id),
                        onExpandClick = {
                            expandedCollectionIds = if (expandedCollectionIds.contains(collection.id)) {
                                expandedCollectionIds - collection.id
                            } else {
                                expandedCollectionIds + collection.id
                            }
                        }
                    )
                }
                
                item {
                    AnimatedVisibility(visible = expandedCollectionIds.contains(collection.id)) {
                        Column(
                            modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            collection.quizzes.forEach { quiz ->
                                QuizCard(quiz)
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
fun HomeScreenPreview() {
    AppTheme {
        HomeScreen()
    }
}
