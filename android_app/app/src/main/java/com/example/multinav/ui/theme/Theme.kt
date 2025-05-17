package com.example.multinav.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext



//private val DarkColorScheme = darkColorScheme(
//    primary = Purple80,
//    secondary = PurpleGrey80,
//    tertiary = Pink80
//)
//
//private val LightColorScheme = lightColorScheme(
//    primary = Purple40,
//    secondary = PurpleGrey40,
//    tertiary = Pink40
//
//    /* Other default colors to override
//    background = Color(0xFFFFFBFE),
//    surface = Color(0xFFFFFBFE),
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
//    */
//)


val LocalGradientColors = staticCompositionLocalOf { AppTheme.gradientColors }

/**
 * Light theme color scheme that complements our gradient colors
 */
private val LightColorScheme = lightColorScheme(
    // Using our primary gradient color as the primary color
    primary = AppTheme.gradientColors[0],
    // Using the middle gradient color as secondary
    secondary = AppTheme.gradientColors[1],
    // Using the last gradient color as tertiary
    tertiary = AppTheme.gradientColors[2],
    // Other colors can be adjusted to match your design
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
    // Using our primary gradient color as the primary color
    primary = AppTheme.gradientColors[0],
    // Using the middle gradient color as secondary
    secondary = AppTheme.gradientColors[1],
    // Using the last gradient color as tertiary
    tertiary = AppTheme.gradientColors[2],
    // Dark mode uses dark backgrounds
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
   // darkTheme: Boolean = isSystemInDarkTheme(),
    darkTheme :Boolean=false,
    // Dynamic color is available on Android 12+
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}