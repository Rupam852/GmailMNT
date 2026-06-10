package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GrowwTealStatic,        // 8AB4F8 Light Blue
    secondary = Color(0xFF535F70),    // Slate Blue Accent
    tertiary = InfoBlue,              // C2E7FF Light Blue
    background = GrowwDarkBg,         // 0D1117 Space Dark Background
    surface = GrowwDarkSurface,       // 161B22 Space Dark Card
    onPrimary = Color(0xFF003062),    // Dark Blue text
    onSecondary = Slate200,           // Light gray blue
    onTertiary = Color(0xFF003355),   // Deep Indigo Blue text
    onBackground = Slate200,          // Light gray blue
    onSurface = Slate200,             // Light gray blue
    error = AlertRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),      // Vibrant Google/Groww Premium Blue for high contrast
    secondary = Color(0xFF535F70),    // Slate Blue
    tertiary = Color(0xFF00639B),     // Elegant light-mode Blue
    background = GrowwLightBg,        // F0F4F9 Light Blue Background
    surface = GrowwLightSurface,      // FFFFFF Pure White Card
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Slate900,          // 1A1C1E Dark text
    onSurface = Slate900,             // 1A1C1E Dark text
    error = Color(0xFFB3261E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to force our beautiful signature Groww theme!
    content: @Composable () -> Unit,
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
