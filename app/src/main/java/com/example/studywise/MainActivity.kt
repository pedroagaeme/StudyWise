package com.example.studywise

import androidx.compose.ui.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import com.example.studywise.ui.navigation.NavigationRoot
import com.example.studywise.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Appwrite.init(applicationContext)
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            AppTheme() {
                NavigationRoot()
                StatusBarProtection()
            }
        }
    }
}

@Composable
private fun StatusBarProtection(
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    heightProvider: () -> Float = calculateGradientHeight(),
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

@Composable
fun calculateGradientHeight(): () -> Float {
    val statusBars = WindowInsets.statusBars
    val density = LocalDensity.current
    return { statusBars.getTop(density).times(1.2f) }
}


