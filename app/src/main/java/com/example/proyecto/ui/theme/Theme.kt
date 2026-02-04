package com.example.proyecto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColorScheme(
    primary = SafeGreen,
    onPrimary = Color.White,
    secondary = SlateGray ,
    background = BackgroundMint,
    surface = CardWhite,
    error = WarningRed,
    onSurface = SlateGray ,
    onBackground = SlateGray
)

private val DarkColorPalette = darkColorScheme(
    primary = SafeGreen,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = WarningRed
)

@Composable
fun ProyectoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}