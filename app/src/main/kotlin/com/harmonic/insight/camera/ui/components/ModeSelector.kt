package com.harmonic.insight.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonic.insight.camera.camera.CaptureMode
import com.harmonic.insight.camera.ui.theme.InsightAccent
import com.harmonic.insight.camera.ui.theme.InsightWhite

@Composable
fun ModeSelector(
    currentMode: CaptureMode,
    onModeChanged: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        ModeTab(
            label = "PHOTO",
            isSelected = currentMode == CaptureMode.PHOTO,
            onClick = { onModeChanged(CaptureMode.PHOTO) },
        )
        ModeTab(
            label = "VIDEO",
            isSelected = currentMode == CaptureMode.VIDEO,
            onClick = { onModeChanged(CaptureMode.VIDEO) },
        )
    }
}

@Composable
private fun ModeTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (isSelected) InsightAccent else InsightWhite.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "mode_tab_color",
    )

    Text(
        text = label,
        color = color,
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        letterSpacing = 1.sp,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
    )
}
