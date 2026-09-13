package com.example.readproplus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ReadProPalette.primary,
    onPrimary = Color.White,
    primaryContainer = ReadProPalette.primaryDeep,
    onPrimaryContainer = ReadProPalette.darkInk,
    secondary = ReadProPalette.warmStrong,
    onSecondary = Color.White,
    secondaryContainer = ReadProPalette.darkSurfaceMuted,
    onSecondaryContainer = ReadProPalette.darkInk,
    background = ReadProPalette.darkBackground,
    onBackground = ReadProPalette.darkInk,
    surface = ReadProPalette.darkSurface,
    onSurface = ReadProPalette.darkInk,
    surfaceVariant = ReadProPalette.darkSurfaceMuted,
    onSurfaceVariant = ReadProPalette.darkMuted,
    outline = ReadProPalette.darkOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = ReadProPalette.primary,
    onPrimary = Color.White,
    primaryContainer = ReadProPalette.primarySoft,
    onPrimaryContainer = ReadProPalette.primaryDeep,
    secondary = ReadProPalette.warmStrong,
    onSecondary = Color.White,
    secondaryContainer = ReadProPalette.warm,
    onSecondaryContainer = ReadProPalette.ink,
    background = ReadProPalette.background,
    onBackground = ReadProPalette.ink,
    surface = ReadProPalette.surface,
    onSurface = ReadProPalette.ink,
    surfaceVariant = ReadProPalette.surfaceMuted,
    onSurfaceVariant = ReadProPalette.muted,
    outline = ReadProPalette.outline,
)

@Composable
fun ReadProPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Keep the reading experience recognizable by default. Dynamic colors are
    // still available to callers that explicitly opt into system theming.
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
        content = content,
    )
}
