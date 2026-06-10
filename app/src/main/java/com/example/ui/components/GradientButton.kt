package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight

/**
 * GradientButton – a reusable button with an animated gradient background.
 *
 * The gradient colors are derived from the app's primary palette to keep the design
 * cohesive. The button uses a subtle elevation on press to provide a tactile feel.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // You can optionally override the gradient colors; defaults use primary & secondary.
    gradientColors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    cornerRadius: Dp = 12.dp,
    minHeight: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .drawBehind {
                // Draw a linear gradient background that fills the composable bounds.
                drawRect(
                    brush = Brush.horizontalGradient(colors = gradientColors),
                    size = size,
                    topLeft = Offset.Zero
                )
            }
            .background(Color.Transparent) // ensures click ripple is visible.
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
        )
    }
}
