package com.jhow.shopplist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WheatGold,
    onPrimary = Charcoal,
    secondary = MossGreen,
    onSecondary = Cream,
    tertiary = Fog,
    background = Charcoal,
    onBackground = Cream,
    surface = Charcoal,
    onSurface = Cream,
    surfaceContainerLow = Slate,
    secondaryContainer = SpruceGreen,
    onSecondaryContainer = Cream,
    outline = Fog
)

private val LightColorScheme = lightColorScheme(
    primary = SpruceGreen,
    onPrimary = Cream,
    secondary = WheatGold,
    onSecondary = Charcoal,
    tertiary = MossGreen,
    background = Cream,
    onBackground = Charcoal,
    surface = Cream,
    onSurface = Charcoal,
    surfaceContainerLow = Fog,
    secondaryContainer = Color(LIGHT_SECONDARY_CONTAINER_HEX),
    onSecondaryContainer = Charcoal,
    outline = Slate
)

private const val LIGHT_SECONDARY_CONTAINER_HEX = 0xFFD6E4DA

@Composable
fun JhowShoppListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
