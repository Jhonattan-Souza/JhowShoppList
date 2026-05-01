package com.jhow.shopplist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = WheatGold,
    onPrimary = Charcoal,
    primaryContainer = MossGreen,
    onPrimaryContainer = Cream,
    secondary = MossGreen,
    onSecondary = Cream,
    secondaryContainer = MossGreen,
    onSecondaryContainer = Cream,
    tertiary = Fog,
    onTertiary = Charcoal,
    tertiaryContainer = Slate,
    onTertiaryContainer = Cream,
    background = Charcoal,
    onBackground = Cream,
    surface = Charcoal,
    onSurface = Cream,
    surfaceContainerLowest = Charcoal,
    surfaceContainerLow = Slate,
    surfaceContainer = Slate,
    surfaceContainerHigh = Fog,
    surfaceContainerHighest = Fog,
    surfaceVariant = Slate,
    onSurfaceVariant = Fog,
    error = WarmRed,
    onError = Cream,
    errorContainer = LightWarmRed,
    onErrorContainer = Charcoal,
    outline = Fog,
    outlineVariant = Slate,
    inverseSurface = Cream,
    inverseOnSurface = Charcoal,
    inversePrimary = WheatGold,
    scrim = Charcoal
)

private val LightColorScheme = lightColorScheme(
    primary = SpruceGreen,
    onPrimary = Cream,
    primaryContainer = MossGreen,
    onPrimaryContainer = Cream,
    secondary = WheatGold,
    onSecondary = Charcoal,
    secondaryContainer = MossGreen,
    onSecondaryContainer = Cream,
    tertiary = MossGreen,
    onTertiary = Charcoal,
    tertiaryContainer = Fog,
    onTertiaryContainer = Charcoal,
    background = Cream,
    onBackground = Charcoal,
    surface = Cream,
    onSurface = Charcoal,
    surfaceContainerLowest = Cream,
    surfaceContainerLow = Fog,
    surfaceContainer = Fog,
    surfaceContainerHigh = Fog,
    surfaceContainerHighest = Fog,
    surfaceVariant = Fog,
    onSurfaceVariant = Slate,
    error = WarmRed,
    onError = Cream,
    errorContainer = LightWarmRed,
    onErrorContainer = Charcoal,
    outline = Slate,
    outlineVariant = Fog,
    inverseSurface = Charcoal,
    inverseOnSurface = Cream,
    inversePrimary = WheatGold,
    scrim = Charcoal
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
