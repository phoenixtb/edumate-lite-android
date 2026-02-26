package io.foxbird.edumate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Indigo seed — matches Flutter's FlexScheme.indigo
private val IndigoSeed = Color(0xFF3F51B5)

// M3 light scheme generated from indigo seed
private val LightColorScheme = lightColorScheme(
    primary                = Color(0xFF3B5BC8),
    onPrimary              = Color(0xFFFFFFFF),
    primaryContainer       = Color(0xFFDDE1FF),
    onPrimaryContainer     = Color(0xFF001258),
    secondary              = Color(0xFF595D72),
    onSecondary            = Color(0xFFFFFFFF),
    secondaryContainer     = Color(0xFFDEE1F9),
    onSecondaryContainer   = Color(0xFF161B2C),
    tertiary               = Color(0xFF755573),
    onTertiary             = Color(0xFFFFFFFF),
    tertiaryContainer      = Color(0xFFFFD7F9),
    onTertiaryContainer    = Color(0xFF2D112E),
    error                  = Color(0xFFBA1A1A),
    onError                = Color(0xFFFFFFFF),
    errorContainer         = Color(0xFFFFDAD6),
    onErrorContainer       = Color(0xFF410002),
    background             = Color(0xFFFBF8FF),
    onBackground           = Color(0xFF1A1B23),
    surface                = Color(0xFFFBF8FF),
    onSurface              = Color(0xFF1A1B23),
    surfaceVariant         = Color(0xFFE2E1EC),
    onSurfaceVariant       = Color(0xFF45464F),
    outline                = Color(0xFF767680),
    outlineVariant         = Color(0xFFC6C5D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow    = Color(0xFFF5F2FF),
    surfaceContainer       = Color(0xFFEFECF9),
    surfaceContainerHigh   = Color(0xFFE9E7F3),
    surfaceContainerHighest= Color(0xFFE3E1EE),
)

// M3 dark scheme generated from indigo seed
private val DarkColorScheme = darkColorScheme(
    primary                = Color(0xFFBAC3FF),
    onPrimary              = Color(0xFF06218A),
    primaryContainer       = Color(0xFF2238AF),   // ← banner / card bg
    onPrimaryContainer     = Color(0xFFDDE1FF),   // ← white-ish text on card
    secondary              = Color(0xFFC2C5DD),
    onSecondary            = Color(0xFF2B2F42),
    secondaryContainer     = Color(0xFF414659),
    onSecondaryContainer   = Color(0xFFDEE1F9),
    tertiary               = Color(0xFFE2BBDF),
    onTertiary             = Color(0xFF432744),
    tertiaryContainer      = Color(0xFF5B3D5B),
    onTertiaryContainer    = Color(0xFFFFD7F9),
    error                  = Color(0xFFFFB4AB),
    onError                = Color(0xFF690005),
    errorContainer         = Color(0xFF93000A),
    onErrorContainer       = Color(0xFFFFDAD6),
    background             = Color(0xFF111318),
    onBackground           = Color(0xFFE3E1EE),
    surface                = Color(0xFF111318),
    onSurface              = Color(0xFFE3E1EE),
    surfaceVariant         = Color(0xFF191C26),   // card/tile bg
    onSurfaceVariant       = Color(0xFFC6C5D0),
    outline                = Color(0xFF90909A),
    outlineVariant         = Color(0xFF45464F),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow    = Color(0xFF191B22),
    surfaceContainer       = Color(0xFF1D2028),
    surfaceContainerHigh   = Color(0xFF272933),
    surfaceContainerHighest= Color(0xFF32343E),
)

@Composable
fun EduMateLiteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK  -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

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
        content = content
    )
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }
