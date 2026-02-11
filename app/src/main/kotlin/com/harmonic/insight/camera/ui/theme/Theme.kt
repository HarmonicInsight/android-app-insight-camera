package com.harmonic.insight.camera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val InsightCameraDarkColorScheme = darkColorScheme(
    primary = InsightPrimaryDark,
    onPrimary = InsightOnPrimaryDark,
    primaryContainer = InsightPrimaryContainerDark,
    onPrimaryContainer = InsightOnPrimaryContainerDark,
    secondary = InsightSecondaryDark,
    onSecondary = InsightOnSecondaryDark,
    background = InsightBackgroundDark,
    onBackground = InsightOnBackgroundDark,
    surface = InsightSurfaceDark,
    onSurface = InsightOnSurfaceDark,
    surfaceVariant = InsightSurfaceVariantDark,
    onSurfaceVariant = InsightOnSurfaceVariantDark,
    error = InsightErrorDark,
    onError = InsightOnErrorDark,
    outline = InsightOutlineDark,
)

@Composable
fun InsightCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InsightCameraDarkColorScheme,
        typography = InsightTypography,
        content = content,
    )
}
