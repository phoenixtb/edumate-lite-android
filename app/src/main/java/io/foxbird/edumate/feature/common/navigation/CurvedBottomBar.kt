package io.foxbird.edumate.feature.common.navigation

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NAV_BAR_HEIGHT = 64.dp
private val FAB_SIZE = 56.dp
private val FAB_RADIUS = 28.dp

@Composable
fun CurvedBottomBar(
    selectedRoute: String?,
    onNavigate: (route: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { 20.dp.toPx() }
    val dockRadiusPx = with(density) { 32.dp.toPx() }
    val scheme = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NAV_BAR_HEIGHT)
                .clip(BottomNavShape(cornerRadiusPx, dockRadiusPx))
        ) {
            // ── Glass background ──────────────────────────────────────────────
            // RenderEffect blur (API 31+) is applied to this layer only — it softens
            // the gradient itself, giving a frosted appearance at the clipped edges.
            // The nav items (drawn after this box) remain sharp.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // BlurEffect in Compose uses RenderEffect under the hood (API 31+).
                        // Applied only to this background layer; nav items above stay sharp.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = BlurEffect(18f, 18f, TileMode.Clamp)
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                scheme.surfaceContainerHigh.copy(alpha = 0.58f),
                                scheme.surfaceContainer.copy(alpha = 0.78f),
                            )
                        )
                    )
            )

            // ── Subtle gradient overlay: slightly lighter at top (catches "light") ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            // ── Glass top-edge line ───────────────────────────────────────────
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                scheme.primary.copy(alpha = 0.30f),
                                scheme.outlineVariant.copy(alpha = 0.50f),
                                scheme.primary.copy(alpha = 0.30f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            // ── Nav items (sharp — not inside the blurred layer) ──────────────
            Row(
                modifier = Modifier.fillMaxWidth().height(NAV_BAR_HEIGHT),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Outlined.Home,
                    selectedIcon = Icons.Filled.Home,
                    label = "Home",
                    selected = selectedRoute == NavRoutes.HOME,
                    onClick = { onNavigate(NavRoutes.HOME) },
                    modifier = Modifier.weight(1f)
                )
                NavBarItem(
                    icon = Icons.Outlined.Schedule,
                    selectedIcon = Icons.Filled.Schedule,
                    label = "History",
                    selected = selectedRoute == NavRoutes.HISTORY,
                    onClick = { onNavigate(NavRoutes.HISTORY) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
                NavBarItem(
                    icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                    selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
                    label = "Library",
                    selected = selectedRoute == NavRoutes.LIBRARY,
                    onClick = { onNavigate(NavRoutes.LIBRARY) },
                    modifier = Modifier.weight(1f)
                )
                NavBarItem(
                    icon = Icons.Outlined.Settings,
                    selectedIcon = Icons.Filled.Settings,
                    label = "Settings",
                    selected = selectedRoute == NavRoutes.SETTINGS,
                    onClick = { onNavigate(NavRoutes.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── FAB — floats above nav bar via negative offset ────────────────────
        FloatingActionButton(
            onClick = { onNavigate("new_chat") },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -FAB_RADIUS)
                .size(FAB_SIZE)
        ) {
            Icon(
                imageVector = Icons.Filled.AddComment,
                contentDescription = "New Chat",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = tint,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/**
 * Custom Shape that carves a centered semicircular cutout from the top edge of the nav bar,
 * with smooth bezier transitions on both sides of the cutout.
 */
private class BottomNavShape(
    private val cornerRadius: Float,
    private val dockRadius: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f

        val baseRect = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset.Zero, Offset(size.width, size.height)),
                    topLeft = CornerRadius(cornerRadius),
                    topRight = CornerRadius(cornerRadius),
                )
            )
        }

        val rect1 = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset.Zero, Offset(cx - dockRadius + 4f, size.height)),
                    topLeft = CornerRadius(cornerRadius),
                )
            )
        }
        val rect1A = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset.Zero, Offset(cx - dockRadius + 4f, size.height)),
                    topLeft = CornerRadius(cornerRadius),
                    topRight = CornerRadius(32f),
                )
            )
        }
        val rect1B = Path.combine(PathOperation.Difference, rect1, rect1A)

        val rect2 = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset(cx + dockRadius - 4f, 0f), Offset(size.width, size.height)),
                    topRight = CornerRadius(cornerRadius),
                )
            )
        }
        val rect2A = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset(cx + dockRadius - 4f, 0f), Offset(size.width, size.height)),
                    topRight = CornerRadius(cornerRadius),
                    topLeft = CornerRadius(32f),
                )
            )
        }
        val rect2B = Path.combine(PathOperation.Difference, rect2, rect2A)

        val circle = Path().apply {
            addOval(
                Rect(
                    Offset(cx - dockRadius, -dockRadius),
                    Offset(cx + dockRadius, dockRadius),
                )
            )
        }

        val path = Path.combine(
            PathOperation.Difference,
            Path.combine(PathOperation.Difference, Path.combine(PathOperation.Difference, baseRect, circle), rect1B),
            rect2B
        )

        return Outline.Generic(path)
    }
}
