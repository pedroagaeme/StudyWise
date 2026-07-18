package com.example.studywise.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studywise.ui.screens.tabs.home.HomeScreen
import com.example.studywise.ui.screens.tabs.components.MainNavigationItem
import com.example.studywise.ui.screens.tabs.search.SearchScreen
import com.example.studywise.ui.screens.tabs.settings.SettingsScreen
import com.example.studywise.ui.theme.AppTheme

@Composable
fun MainScreen(
    pushCreateQuizRoute: () -> Unit = {},
    pushQuizDetailsRoute: (String) -> Unit = {},
    pushAnswerQuizRoute: (String) -> Unit = {},
    replaceWithLoginRoute: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets.safeContent,
        bottomBar = {
            Column {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
                BottomAppBar(
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainNavigationItem(
                            icon = Icons.Rounded.Home,
                            label = "Home",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        MainNavigationItem(
                            icon = Icons.Rounded.Search,
                            label = "Search",
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        FloatingActionButton(
                            onClick = pushCreateQuizRoute,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Filled.Add, "Create")
                        }
                        MainNavigationItem(
                            icon = Icons.Rounded.BarChart,
                            label = "Stats",
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                        MainNavigationItem(
                            icon = Icons.Rounded.Settings,
                            label = "Settings",
                            isSelected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
        ) {

            when (selectedTab) {
                0 -> HomeScreen(innerPadding = innerPadding, pushQuizDetailsRoute = pushQuizDetailsRoute)
                1 -> SearchScreen(innerPadding = innerPadding, pushQuizDetailsRoute = pushQuizDetailsRoute)
                2 -> PlaceholderScreen()
                3 -> SettingsScreen(
                    innerPadding = innerPadding,
                    replaceWithLoginRoute = replaceWithLoginRoute
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Coming Soon", style = MaterialTheme.typography.headlineMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AppTheme {
        MainScreen()
    }
}
