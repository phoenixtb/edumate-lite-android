package io.foxbird.edumate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EduPrimary,
    onPrimary = EduOnPrimary,
    primaryContainer = EduPrimaryContainer,
    onPrimaryContainer = EduOnPrimaryContainer,
    secondary = EduSecondary,
    onSecondary = EduOnSecondary,
    secondaryContainer = EduSecondaryContainer,
    onSecondaryContainer = EduOnSecondaryContainer,
    tertiary = EduTertiary,
    onTertiary = EduOnTertiary,
    tertiaryContainer = EduTertiaryContainer,
    onTertiaryContainer = EduOnTertiaryContainer,
    error = EduError,
    onError = EduOnError,
    errorContainer = EduErrorContainer,
    onErrorContainer = EduOnErrorContainer,
    background = EduBackground,
    onBackground = EduOnBackground,
    surface = EduSurface,
    onSurface = EduOnSurface,
    surfaceVariant = EduSurfaceVariant,
    onSurfaceVariant = EduOnSurfaceVariant,
    outline = EduOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = EduPrimaryDark,
    onPrimary = EduOnPrimaryDark,
    primaryContainer = EduPrimaryContainerDark,
    onPrimaryContainer = EduOnPrimaryContainerDark,
    secondary = EduSecondaryDark,
    onSecondary = EduOnSecondaryDark,
    secondaryContainer = EduSecondaryContainerDark,
    onSecondaryContainer = EduOnSecondaryContainerDark,
    tertiary = EduTertiaryDark,
    onTertiary = EduOnTertiaryDark,
    tertiaryContainer = EduTertiaryContainerDark,
    onTertiaryContainer = EduOnTertiaryContainerDark,
    error = EduErrorDark,
    onError = EduOnErrorDark,
    errorContainer = EduErrorContainerDark,
    onErrorContainer = EduOnErrorContainerDark,
    background = EduBackgroundDark,
    onBackground = EduOnBackgroundDark,
    surface = EduSurfaceDark,
    onSurface = EduOnSurfaceDark,
    surfaceVariant = EduSurfaceVariantDark,
    onSurfaceVariant = EduOnSurfaceVariantDark,
    outline = EduOutlineDark,
    surfaceTint = Color.Transparent,
)

@Composable
fun EduMateLiteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
