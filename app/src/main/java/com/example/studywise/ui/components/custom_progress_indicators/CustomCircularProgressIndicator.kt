package com.example.studywise.ui.components.custom_progress_indicators

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CustomCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    strokeWidth: Dp = 4.dp
) {
    val safeProgress = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1.0f, true)
    ) {
        drawArc(
            color = backgroundColor,
            startAngle = -90f, // -90f starts the arc at the top (12 o'clock)
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(
                width = strokeWidth.toPx()
            )
        )

        // Calculate the sweep angle (360 degrees * percentage)
        val sweepAngle = 360f * safeProgress

        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Preview
@Composable
fun CustomCircularProgressIndicatorPreview() {
    CustomCircularProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = 0.7f,
    )
}
