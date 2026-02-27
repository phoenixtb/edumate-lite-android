package io.foxbird.edumate.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class Star(
    val x: Float,      // [0, 1] fraction of canvas width
    val y: Float,      // [0, 1] fraction of canvas height
    val radius: Float, // dp
    val alpha: Float,  // base alpha before the global multiplier
)

/**
 * Pre-computed star positions — seeded for determinism, computed once.
 * No per-frame work: positions and sizes are fixed.
 */
private val AppStars: List<Star> by lazy {
    val rng = Random(seed = 42)
    List(120) {
        val tier = rng.nextFloat()
        Star(
            x      = rng.nextFloat(),
            y      = rng.nextFloat(),
            // Three size tiers: small/medium/large
            radius = when {
                tier < 0.65f -> rng.nextFloat() * 0.6f + 0.5f   // 0.5 – 1.1 dp  (65 %)
                tier < 0.90f -> rng.nextFloat() * 0.8f + 1.1f   // 1.1 – 1.9 dp  (25 %)
                else         -> rng.nextFloat() * 0.8f + 1.9f   // 1.9 – 2.7 dp  (10 %)
            },
            // Matching alpha tiers: dimmer for small, brighter for large
            alpha  = when {
                tier < 0.65f -> rng.nextFloat() * 0.12f + 0.10f  // 10 – 22 %
                tier < 0.90f -> rng.nextFloat() * 0.12f + 0.22f  // 22 – 34 %
                else         -> rng.nextFloat() * 0.15f + 0.34f  // 34 – 49 %
            },
        )
    }
}

/**
 * Full-size [Canvas] that paints a static starfield on a transparent surface.
 *
 * Drop this as the *first* child of a [Box] so it sits behind your screen content.
 * Stars are white circles — visible in dark mode (alpha=1), hidden in light (alpha=0).
 *
 * [alpha] is a gate, not an intensity scaler. Per-star alpha (2–7 %) controls
 * the subtlety so the effect stays sophisticated without external multipliers.
 *
 * Performance: zero per-frame computation; the star list is a lazy singleton.
 */
@Composable
fun StarfieldBackground(
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0f) return
    val stars = remember { AppStars }
    Canvas(modifier = modifier) {
        val oneDp = 1.dp.toPx()
        stars.forEach { star ->
            drawCircle(
                color  = Color.White,
                radius = star.radius * oneDp,
                center = Offset(star.x * size.width, star.y * size.height),
                alpha  = star.alpha,  // per-star alpha already provides the subtlety
            )
        }
    }
}
