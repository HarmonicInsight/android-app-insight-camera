package com.harmonic.insight.camera.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Insight Ivory & Gold カラーシステム（Dark Mode）
// カメラアプリはダーク専用のため、Dark パレットのみ使用
// brand/colors.json の darkMode セクションに基づく
// ============================================================

// --- Dark Theme (Camera-optimized) ---
val InsightPrimaryDark = Color(0xFFD4BC6A)            // Accent 400 (Gold)
val InsightOnPrimaryDark = Color(0xFF6B5518)
val InsightPrimaryContainerDark = Color(0xFF8C711E)   // Gold Hover
val InsightOnPrimaryContainerDark = Color(0xFFF0E6C8)

val InsightSecondaryDark = Color(0xFFF0E6C8)          // Accent 200
val InsightOnSecondaryDark = Color(0xFF6B5518)

val InsightBackgroundDark = Color(0xFF1C1917)         // Dark Bg Primary
val InsightOnBackgroundDark = Color(0xFFFAF8F5)       // Ivory
val InsightSurfaceDark = Color(0xFF292524)            // Dark Card
val InsightOnSurfaceDark = Color(0xFFFAF8F5)
val InsightSurfaceVariantDark = Color(0xFF3D3835)     // Dark Hover
val InsightOnSurfaceVariantDark = Color(0xFFD6D3D1)   // Text Secondary

val InsightErrorDark = Color(0xFFFF6B6B)
val InsightOnErrorDark = Color(0xFF1C1917)
val InsightOutlineDark = Color(0xFF3D3835)            // Dark Border

// --- Semantic ---
val InsightSuccess = Color(0xFF16A34A)
val InsightWarning = Color(0xFFCA8A04)
val InsightInfo = Color(0xFF2563EB)

// --- Convenience aliases (used by UI components) ---
val InsightAccent = InsightPrimaryDark           // Gold accent (#D4BC6A)
val InsightBlack = InsightBackgroundDark          // Dark background (#1C1917)
val InsightWhite = InsightOnBackgroundDark        // Ivory text/icons (#FAF8F5)
val InsightGray = InsightOnSurfaceVariantDark     // Secondary text (#D6D3D1)
val InsightError = InsightErrorDark               // Error red (#FF6B6B)

// --- Camera-specific ---
val FlashYellow = Color(0xFFFFD54F)
val RecordingRed = Color(0xFFEF5350)
