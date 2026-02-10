package com.harmonic.insight.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val innerColor by animateColorAsState(
        targetValue = Color.Red,
        label = "record_color",
    )

    val pulseTransition = rememberInfiniteTransition(label = "record_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Canvas(
        modifier = modifier
            .size(76.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { onClick() }
            },
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.minDimension / 2

        // Outer ring
        drawCircle(
            color = Color.White,
            radius = outerRadius,
            center = center,
            style = Stroke(width = 4.dp.toPx()),
        )

        if (isRecording) {
            // Stop icon: rounded square with pulse
            val squareSize = outerRadius * 0.7f
            drawRoundRect(
                color = innerColor.copy(alpha = pulseAlpha),
                topLeft = Offset(center.x - squareSize / 2, center.y - squareSize / 2),
                size = Size(squareSize, squareSize),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
        } else {
            // Record icon: red circle
            drawCircle(
                color = if (enabled) innerColor else innerColor.copy(alpha = 0.4f),
                radius = outerRadius * 0.7f,
                center = center,
            )
        }
    }
}
