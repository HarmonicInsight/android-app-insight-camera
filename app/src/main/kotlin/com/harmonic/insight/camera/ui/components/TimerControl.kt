package com.harmonic.insight.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonic.insight.camera.ui.theme.InsightAccent
import com.harmonic.insight.camera.ui.theme.InsightWhite

enum class TimerDuration(val seconds: Int, val label: String) {
    OFF(0, "OFF"),
    THREE(3, "3s"),
    TEN(10, "10s"),
}

@Composable
fun TimerButton(
    timerDuration: TimerDuration,
    onCycleTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (timerDuration) {
        TimerDuration.OFF -> Icons.Filled.TimerOff
        TimerDuration.THREE -> Icons.Filled.Timer3
        TimerDuration.TEN -> Icons.Filled.Timer10
    }
    val tint = if (timerDuration == TimerDuration.OFF) {
        InsightWhite.copy(alpha = 0.6f)
    } else {
        InsightAccent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCycleTimer() }
            .padding(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Timer ${timerDuration.label}",
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = timerDuration.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun TimerCountdownOverlay(
    secondsRemaining: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
    ) {
        Text(
            text = secondsRemaining.toString(),
            color = InsightWhite,
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
