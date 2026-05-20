package com.example.studywise.ui.components.stack_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StackScreen(
    title: String,
    modifier: Modifier = Modifier,
    transitionProgress: Float = 1f,
    currentScroll: Int = 0,
    onBackClick: () -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    // Animate the provided progress so callers can pass 0f/1f and get the
    // same tween/FastOutSlowInEasing animation used by QuizDetailsScreenContent
    val animatedProgress by animateFloatAsState(
        targetValue = transitionProgress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "stackScreenEnter"
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = 0.92f + (0.08f * animatedProgress)
                            scaleY = 0.92f + (0.08f * animatedProgress)
                        }
                        .alpha(animatedProgress),
                    navigationIcon = {
                        if (navigationIcon != null) {
                            navigationIcon()
                        } else {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Go back"
                                )
                            }
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                AnimatedVisibility(
                    visible = currentScroll > 0,
                    enter = fadeIn(tween(durationMillis = 500)),
                    exit = fadeOut(tween(durationMillis = 500))
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        content(
            Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .graphicsLayer {
                    scaleX = 0.92f + (0.08f * animatedProgress)
                    scaleY = 0.92f + (0.08f * animatedProgress)
                }
                .alpha(animatedProgress)
        )
    }
}


