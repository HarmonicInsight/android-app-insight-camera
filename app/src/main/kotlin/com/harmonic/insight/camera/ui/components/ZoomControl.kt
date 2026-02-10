package com.harmonic.insight.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonic.insight.camera.ui.theme.InsightAccent
import com.harmonic.insight.camera.ui.theme.InsightWhite
import kotlin.math.abs

data class ZoomPreset(
    val label: String,
    val ratio: Float,
)

@Composable
fun ZoomPresetBar(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = buildList {
        // Ultra-wide if available
        if (minZoom <= 0.6f) add(ZoomPreset("0.5x", 0.6f))
        add(ZoomPreset("1x", 1f))
        if (maxZoom >= 2f) add(ZoomPreset("2x", 2f))
        if (maxZoom >= 3f) add(ZoomPreset("3x", 3f))
        if (maxZoom >= 5f) add(ZoomPreset("5x", 5f))
        if (maxZoom >= 10f) add(ZoomPreset("10x", 10f))
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        presets.forEach { preset ->
            ZoomChip(
                label = preset.label,
                isSelected = isZoomPresetActive(currentZoom, preset.ratio, presets),
                onClick = { onZoomSelected(preset.ratio) },
            )
        }
    }
}

@Composable
private fun ZoomChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) InsightAccent else Color.Transparent,
        animationSpec = tween(150),
        label = "zoom_chip_bg",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else InsightWhite.copy(alpha = 0.7f),
        animationSpec = tween(150),
        label = "zoom_chip_text",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun ZoomIndicator(
    currentZoom: Float,
    modifier: Modifier = Modifier,
) {
    val displayZoom = "%.1fx".format(currentZoom)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = displayZoom,
            color = InsightWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun isZoomPresetActive(
    currentZoom: Float,
    presetRatio: Float,
    presets: List<ZoomPreset>,
): Boolean {
    // Find the closest preset
    val closest = presets.minByOrNull { abs(it.ratio - currentZoom) }
    return closest?.ratio == presetRatio
}
