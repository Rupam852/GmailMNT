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
    primary = GrowwTeal,              // D0BCFF Lavender
    secondary = Color(0xFF4A4458),    // Dark eggplant slate accent
    tertiary = InfoBlue,              // C2E7FF Light Blue
    background = GrowwDarkBg,         // 0F0F0F Onyx Background
    surface = GrowwDarkSurface,       // 1D1B20 Eggplant slate card
    onPrimary = Color(0xFF381E72),    // Deep Purple text
    onSecondary = Slate200,           // Light gray mauve
    onTertiary = Color(0xFF003355),   // Deep Indigo Blue text
    onBackground = Slate200,          // Light gray mauve
    onSurface = Slate200,             // Light gray mauve
    error = AlertRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),      // Rich M3 Deep Purple
    secondary = Color(0xFF625B71),    // Slate Mauve
    tertiary = Color(0xFF00639B),     // Elegant light-mode Blue
    background = GrowwLightBg,        // F7F2FA Light Mauve Background
    surface = GrowwLightSurface,      // ECE6F0 Light Surface Card
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Slate900,          // 1D1B20 Dark eggplant text
    onSurface = Slate900,             // 1D1B20 Dark eggplant text
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
