// File: com.example.multinav.ui.theme/AppTheme.kt

package com.example.multinav.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * App-wide theme constants and utilities for maintaining consistent visual styles
 * throughout the application, especially for gradient-based designs.
 */
object AppTheme {
    /**
     * The primary gradient colors used throughout the app.
     * - First color (0xFF233992): Deep blue
     * - Second color (0xFFA030C7): Purple
     * - Third color (0xFF1C0090): Dark indigo
     */
    val gradientColors = listOf(
        Color(0xFF233992),
        Color(0xFFA030C7),
        Color(0xFF1C0090)
    )

    /**
     * Pre-defined gradient brushes for quick access
     */
    val horizontalGradient = Brush.horizontalGradient(colors = gradientColors)
    val verticalGradient = Brush.verticalGradient(colors = gradientColors)
    val radialGradient = Brush.radialGradient(colors = gradientColors)

    // For diagonal gradients (top-left to bottom-right)
    val diagonalGradient = Brush.linearGradient(colors = gradientColors)

    /**
     * Colors for text and icons that appear on gradient backgrounds
     */
    val onGradientColor = Color.White
    val onGradientColorMuted = Color.White.copy(alpha = 0.7f)
    val onGradientColorSubtle = Color.White.copy(alpha = 0.5f)

    /**
     * Overlay color for selected items (with slight transparency)
     */
    val selectionOverlay = Color.White.copy(alpha = 0.2f)

    /**
     * Returns appropriate text color based on background and emphasis level
     */
    @Composable
    fun textColorOnGradient(highEmphasis: Boolean = true): Color {
        return if (highEmphasis) onGradientColor else onGradientColorMuted
    }
}