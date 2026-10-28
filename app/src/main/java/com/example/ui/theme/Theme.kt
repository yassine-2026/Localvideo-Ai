package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LuxuryMonochromeColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = OnyxBackground,
    primaryContainer = GraphiteSurfaceVariant,
    onPrimaryContainer = PureWhite,
    secondary = AccentSilver,
    onSecondary = OnyxBackground,
    secondaryContainer = CharcoalSurface,
    onSecondaryContainer = PlatinumText,
    tertiary = SilverMutedText,
    onTertiary = OnyxBackground,
    background = OnyxBackground,
    onBackground = PlatinumText,
    surface = CharcoalSurface,
    onSurface = PlatinumText,
    surfaceVariant = GraphiteSurfaceVariant,
    onSurfaceVariant = SilverMutedText,
    outline = SlateCardBorder,
    outlineVariant = CharcoalSurface,
    error = StatusRed,
    onError = PureWhite
)

@Composable
fun LocalVideoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxuryMonochromeColorScheme,
        typography = Typography,
        content = content
    )
}
