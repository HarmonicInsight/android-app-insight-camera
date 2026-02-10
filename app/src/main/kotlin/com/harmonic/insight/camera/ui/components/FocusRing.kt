package com.harmonic.insight.camera.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.harmonic.insight.camera.ui.theme.InsightAccent

data class FocusPoint(val x: Float, val y: Float, val id: Long)

@Composable
fun FocusRingOverlay(
    focusPoint: FocusPoint?,
    modifier: Modifier = Modifier,
) {
    if (focusPoint == null) return

    val scale = remember(focusPoint.id) { Animatable(1.6f) }
    val alpha = remember(focusPoint.id) { Animatable(1f) }

    LaunchedEffect(focusPoint.id) {
        // Animate scale down
        scale.animateTo(1f, animationSpec = tween(250))
        // Hold briefly then fade out
        kotlinx.coroutines.delay(800)
        alpha.animateTo(0f, animationSpec = tween(300))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val radius = 36.dp.toPx() * scale.value
        drawCircle(
            color = InsightAccent.copy(alpha = alpha.value),
            radius = radius,
            center = Offset(focusPoint.x, focusPoint.y),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
