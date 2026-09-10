package com.albumcatalog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VinylGold,
    onPrimary = Color.Black,
    primaryContainer = VinylGoldDark,
    onPrimaryContainer = Color.White,
    secondary = VinylBlue,
    onSecondary = Color.Black,
    background = VinylBlack,
    onBackground = TextPrimary,
    surface = VinylDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = VinylCard,
    onSurfaceVariant = TextSecondary,
    outline = VinylBorder
)

@Composable
fun VinylCatalogTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
