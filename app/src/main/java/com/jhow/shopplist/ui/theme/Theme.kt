package com.jhow.shopplist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
    secondaryContainer = MossGreen,
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
    secondaryContainer = MossGreen,
    onSecondaryContainer = Cream,
    outline = Slate
)

internal fun lightAppColorScheme() = LightColorScheme

internal fun darkAppColorScheme() = DarkColorScheme

@Composable
fun JhowShoppListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkAppColorScheme() else lightAppColorScheme(),
        typography = Typography,
        content = content
    )
}
