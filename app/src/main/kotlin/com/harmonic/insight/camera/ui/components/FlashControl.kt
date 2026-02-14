package com.harmonic.insight.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonic.insight.camera.R
import com.harmonic.insight.camera.camera.FlashMode
import com.harmonic.insight.camera.camera.LightMode
import com.harmonic.insight.camera.ui.theme.FlashYellow
import com.harmonic.insight.camera.ui.theme.InsightWhite

@Composable
fun FlashModeButton(
    flashMode: FlashMode,
    onCycleFlash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (flashMode) {
        FlashMode.OFF -> Icons.Filled.FlashOff
        FlashMode.ON -> Icons.Filled.FlashOn
        FlashMode.AUTO -> Icons.Filled.FlashAuto
    }
    val label = when (flashMode) {
        FlashMode.OFF -> stringResource(R.string.flash_off)
        FlashMode.ON -> stringResource(R.string.flash_on)
        FlashMode.AUTO -> stringResource(R.string.flash_auto)
    }
    val tint = when (flashMode) {
        FlashMode.OFF -> InsightWhite.copy(alpha = 0.6f)
        FlashMode.ON -> FlashYellow
        FlashMode.AUTO -> InsightWhite
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCycleFlash() }
            .padding(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Flash $label",
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun LightToggleButton(
    lightMode: LightMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isOn = lightMode == LightMode.ON
    val bgColor by animateColorAsState(
        targetValue = if (isOn) FlashYellow else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(200),
        label = "light_bg",
    )
    val contentColor = if (isOn) Color.Black else InsightWhite.copy(alpha = if (enabled) 0.8f else 0.3f)
    val icon = if (isOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff
    val lightLabel = if (isOn) stringResource(R.string.light_on) else stringResource(R.string.light_off)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(enabled = enabled) { onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = lightLabel,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = lightLabel,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}
