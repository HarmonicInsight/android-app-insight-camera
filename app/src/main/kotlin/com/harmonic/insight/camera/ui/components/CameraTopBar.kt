package com.harmonic.insight.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.harmonic.insight.camera.camera.CaptureMode
import com.harmonic.insight.camera.camera.FlashMode
import com.harmonic.insight.camera.camera.InsightAspectRatio
import com.harmonic.insight.camera.ui.theme.InsightWhite

@Composable
fun CameraTopBar(
    flashMode: FlashMode,
    onCycleFlash: () -> Unit,
    timerDuration: TimerDuration,
    onCycleTimer: () -> Unit,
    aspectRatio: InsightAspectRatio,
    onToggleAspectRatio: () -> Unit,
    onSwitchCamera: () -> Unit,
    captureMode: CaptureMode,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Transparent,
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Flash control (photo mode only)
            if (captureMode == CaptureMode.PHOTO) {
                FlashModeButton(
                    flashMode = flashMode,
                    onCycleFlash = onCycleFlash,
                )
            }

            // Timer (photo mode only)
            if (captureMode == CaptureMode.PHOTO) {
                TimerButton(
                    timerDuration = timerDuration,
                    onCycleTimer = onCycleTimer,
                )
            }

            // Aspect ratio
            AspectRatioButton(
                aspectRatio = aspectRatio,
                onToggle = onToggleAspectRatio,
            )

            // Camera switch
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onSwitchCamera() },
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = InsightWhite,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
