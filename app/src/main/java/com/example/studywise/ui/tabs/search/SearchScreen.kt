package com.example.studywise.ui.tabs.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studywise.ui.components.quiz_card.QuizCard
import com.example.studywise.ui.components.model.Quiz
import com.example.studywise.ui.theme.AppTheme

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val allQuizzes = remember {
        listOf(
            Quiz("1", "Organic Chemistry", "2m ago", "University"),
            Quiz("2", "Ancient History", "1h ago", "Personal"),
            Quiz("3", "Advanced Math", "Yesterday", "University"),
            Quiz("4", "Quantum Physics", "3 days ago", "University"),
            Quiz("5", "World Geography", "Last week", "Personal"),
            Quiz("6", "Cooking Basics", "2 weeks ago", "Personal")
        )
    }

    val filteredQuizzes = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            allQuizzes
        } else {
            allQuizzes.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search your quizzes...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredQuizzes) { quiz ->
                    QuizCard(quiz = quiz)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    AppTheme {
        SearchScreen()
    }
}
