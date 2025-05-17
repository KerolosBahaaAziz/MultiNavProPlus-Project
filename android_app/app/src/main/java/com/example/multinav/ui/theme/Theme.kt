package com.example.multinav.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Define basic colors at the file level
val gradientColors = listOf(
    Color(0xFF233992),
    Color(0xFFA030C7),
    Color(0xFF1C0090)
)

val onGradientColor = Color.White
val onGradientColorMuted = Color.White.copy(alpha = 0.7f)

// Use compositionLocalOf instead of staticCompositionLocalOf for button colors
// since they need to be created inside a @Composable context
val LocalGradientColors = compositionLocalOf { gradientColors }
val LocalButtonShape = compositionLocalOf { RoundedCornerShape(24.dp) }

/**
 * Light theme color scheme that complements our gradient colors
 */
private val LightColorScheme = lightColorScheme(
    primary = gradientColors[0],
    secondary = gradientColors[1],
    tertiary = gradientColors[2],
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

/**
 * Dark theme color scheme that complements our gradient colors
 */
private val DarkColorScheme = darkColorScheme(
    primary = gradientColors[0],
    secondary = gradientColors[1],
    tertiary = gradientColors[2],
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun MultiNavTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Create button colors inside the @Composable function
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = onGradientColor
    )

    // Provide all the theme values through CompositionLocal
    CompositionLocalProvider(
        LocalGradientColors provides gradientColors,
        LocalButtonShape provides RoundedCornerShape(24.dp)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Utility function to create a gradient brush from the theme colors
@Composable
fun gradientBrush(): Brush {
    return Brush.horizontalGradient(colors = LocalGradientColors.current)
}