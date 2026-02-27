package io.foxbird.edumate.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended AI-specific colour tokens, always derived from the active M3 [ColorScheme].
 *
 * Design contract
 * ───────────────
 * • All alpha values are intentionally low (≤ 20 %) — subtle, not garish.
 * • Every field is computed from the dynamic/seeded M3 scheme, so swapping a
 *   seed colour (or enabling dynamic colours) automatically refreshes every token.
 * • The only fixed values are semantic status colours (stepComplete green) whose
 *   meaning must be colour-neutral.
 *
 * Reuse in other apps
 * ───────────────────
 * 1. Depend on this package.
 * 2. Call EduMateLiteTheme (or your own wrapper) with a different seed / dynamic flag.
 * 3. All components reading [MaterialTheme.appColors] adapt automatically.
 */
data class AppColors(
    // ── Ambient glow — used as a subtle tinted overlay at the top of cards ──
    val primaryGlow: Color,         // primary @ 7 %
    val tertiaryGlow: Color,        // tertiary @ 5 %

    // ── Glass card system ────────────────────────────────────────────────────
    val glassBorderAccent: Color,   // primary @ 18 % — highlighted edge (e.g. top-left)
    val glassBorderDefault: Color,  // outlineVariant @ 35 % — neutral card border

    // ── Gradient pair — progress bars, banners ───────────────────────────────
    val progressStart: Color,       // = primary
    val progressEnd: Color,         // = tertiary

    // ── Processing pipeline ──────────────────────────────────────────────────
    val stepActive: Color,          // = primary
    val stepComplete: Color,        // semantic green; fixed so success is always green

    // ── Starfield ────────────────────────────────────────────────────────────
    // 1.0 in dark mode, 0.0 in light. Acts as an on/off gate; per-star alpha
    // (1–3.5 %) already controls the subtlety — do not multiply by this value.
    val starfieldAlpha: Float,

    // ── Metadata ─────────────────────────────────────────────────────────────
    val isDark: Boolean,
)

/**
 * Derive [AppColors] from any M3 [ColorScheme] — works with dynamic and seeded schemes.
 * Called once per theme change; cheap to recompute.
 */
fun ColorScheme.toAppColors(isDark: Boolean): AppColors = AppColors(
    primaryGlow          = primary.copy(alpha = 0.07f),
    tertiaryGlow         = tertiary.copy(alpha = 0.05f),
    glassBorderAccent    = primary.copy(alpha = 0.18f),
    glassBorderDefault   = outlineVariant.copy(alpha = 0.35f),
    progressStart        = primary,
    progressEnd          = tertiary,
    stepActive           = primary,
    stepComplete         = if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32),
    starfieldAlpha       = if (isDark) 1.0f else 0.0f,
    isDark               = isDark,
)

internal val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors in composition — wrap content with EduMateLiteTheme")
}

/** Retrieve [AppColors] from the nearest theme provider. */
val MaterialTheme.appColors: AppColors
    @Composable @ReadOnlyComposable get() = LocalAppColors.current
