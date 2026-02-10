package com.harmonic.insight.camera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val InsightDarkColorScheme = darkColorScheme(
    primary = InsightAccent,
    onPrimary = InsightWhite,
    primaryContainer = InsightAccentDark,
    onPrimaryContainer = InsightWhite,
    secondary = InsightGray,
    onSecondary = InsightWhite,
    background = InsightBlack,
    onBackground = InsightWhite,
    surface = InsightDarkSurface,
    onSurface = InsightWhite,
    surfaceVariant = InsightDarkGray,
    onSurfaceVariant = InsightLightGray,
    error = InsightError,
    onError = InsightWhite,
)

@Composable
fun InsightCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InsightDarkColorScheme,
        typography = InsightTypography,
        content = content,
    )
}
