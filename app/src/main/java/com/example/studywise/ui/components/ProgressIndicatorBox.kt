package com.example.studywise.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.studywise.ui.components.custom_progress_indicators.CustomCircularProgressIndicator

@Composable
fun ProgressIndicatorBox(
    progress: Float,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true,
    indicatorPadding: Dp = 0.dp,
    boxSize: Dp = 100.dp,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progressAnimation"
    )

    Box(
        modifier = modifier.size(boxSize),
        contentAlignment = Alignment.Center
    ) {
        CustomCircularProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.padding(indicatorPadding)
        )
        if (showPercentage) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}



