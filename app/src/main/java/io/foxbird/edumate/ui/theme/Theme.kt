package io.foxbird.edumate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Root theme for EduMate Lite — and the template for all sibling apps.
 *
 * Theming model
 * ─────────────
 * • Targets Android 12+ → dynamic colours from the user's wallpaper are always
 *   active; no static seed colour is required.
 * • [AppColors] (see AppColors.kt) extends the M3 colour scheme with AI-specific
 *   semantic tokens (glow, glass borders, gradient pairs, starfield alpha).
 *   Every token is derived from the dynamic M3 scheme so it adapts automatically.
 *
 * Reuse in another app
 * ────────────────────
 * 1. Copy ui/theme/ into the new project (AppColors, Starfield, Theme, Type).
 * 2. Rename [EduMateLiteTheme] and adjust [Typography] if needed.
 * 3. Done — all components using [MaterialTheme.appColors] pick up the new palette.
 */
@Composable
fun EduMateLiteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    // Dynamic colours — always on for Android 12+. The `remember` prevents
    // rebuilding the scheme on every recomposition unless the key changes.
    val colorScheme = remember(darkTheme, context) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    val appColors = remember(colorScheme, darkTheme) { colorScheme.toAppColors(darkTheme) }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content,
        )
    }
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }
