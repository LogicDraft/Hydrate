package com.gowtham.hydrate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFFA1A1A1),
    background = Color(0xFF0A0A0A),
    onBackground = Color.White,
    surface = Color(0xFF111111),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1B1B1B),
    outline = Color(0xFF2D2D2D),
)

@Composable
fun HydrateTheme(
    darkTheme: Boolean = true, // Force dark theme by default, or use isSystemInDarkTheme() if they want Light Mode too. Google Apps support both. We will force dark based on the previous monochrome setting, but allow dynamic.
    dynamicColor: Boolean = true, // True Google App feel uses dynamic color
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> lightColorScheme() // Add basic light scheme fallback if needed
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
