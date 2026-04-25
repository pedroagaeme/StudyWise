@file:Suppress("FunctionName")

package com.example.studywise.ui.components.icon_button_with_offset

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class IconButtonOffsetDirection {
    Left,
    Right,
    None
}

@Composable
fun IconButtonWithOffset(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    offsetDirection: IconButtonOffsetDirection = IconButtonOffsetDirection.None,
    iconHorizontalInset: Dp = 12.dp,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val visualAlignmentOffset = iconHorizontalInset
    val horizontalOffset = when (offsetDirection) {
        IconButtonOffsetDirection.Left -> -visualAlignmentOffset
        IconButtonOffsetDirection.Right -> visualAlignmentOffset
        IconButtonOffsetDirection.None -> 0.dp
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.offset(x = horizontalOffset),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}




