package com.example.studywise.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

/**
 * Composable that adds a gradient protection overlay at the top of the screen
 * to ensure content is visible over the status bar area.
 */
@Composable
fun StatusBarProtection(
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    heightProvider: () -> Float = calculateStatusBarGradientHeight(),
) {
    Canvas(Modifier.fillMaxSize()) {
        val calculatedHeight = heightProvider()
        val gradient = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = 1f),
                color.copy(alpha = .8f),
                Color.Transparent
            ),
            startY = 0f,
            endY = calculatedHeight
        )
        drawRect(
            brush = gradient,
            size = Size(size.width, calculatedHeight),
        )
    }
}

/**
 * Composable that adds a gradient protection overlay at the bottom of the screen
 * to ensure content is visible over the navigation bar area.
 */
@Composable
fun BottomBarProtection(
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    heightProvider: () -> Float = calculateNavigationBarGradientHeight(),
) {
    Canvas(Modifier.fillMaxSize()) {
        val calculatedHeight = heightProvider()
        val gradient = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = .8f),
                color.copy(alpha = 1f)
            ),
            startY = size.height - calculatedHeight,
            endY = size.height
        )
        drawRect(
            brush = gradient,
            size = Size(size.width, calculatedHeight),
            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - calculatedHeight)
        )
    }
}

@Composable
fun calculateStatusBarGradientHeight(): () -> Float {
    val statusBars = WindowInsets.statusBars
    val density = LocalDensity.current
    return { statusBars.getTop(density).times(1.2f) }
}

@Composable
fun calculateNavigationBarGradientHeight(): () -> Float {
    val navigationBars = WindowInsets.navigationBars
    val density = LocalDensity.current
    return { navigationBars.getBottom(density).times(1.2f) }
}

