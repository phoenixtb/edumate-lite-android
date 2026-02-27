package io.foxbird.edumate.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.foxbird.edumate.ui.theme.appColors

/**
 * Full-width gradient header with brand color, used at top of Home and Library screens.
 */
@Composable
fun GradientHeader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        scheme.primaryContainer,
                        scheme.primary.copy(alpha = 0.85f),
                        scheme.primary,
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        content()
    }
}

/**
 * Rounded square/circle icon container with colored background.
 * Used in Settings rows, Quick Actions, etc.
 */
@Composable
fun IconContainer(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Pill-shaped stat badge with colored icon + value + label.
 * Used on Home screen stats row and Material Detail.
 */
@Composable
fun StatBadge(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Square stat card with icon, value, label — matches Flutter _StatCard.
 * Used on Home screen stats row.
 */
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Colored pill badge for status display (ACTIVE, EXPERIMENTAL, Bundled, etc.)
 */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Section header with colored label text and optional action.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

/**
 * Quick action card with icon and subtitle — used in the 2×2 grid on Home screen.
 */
@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.appColors.glassBorderDefault, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Icon with subtle glow behind it
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(46.dp).background(iconColor.copy(alpha = 0.10f), RoundedCornerShape(13.dp)))
                IconContainer(
                    icon = icon,
                    containerColor = iconColor.copy(alpha = 0.18f),
                    iconColor = iconColor,
                    size = 44.dp,
                    iconSize = 22.dp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class PipelineStepState { COMPLETED, ACTIVE, PENDING }

/**
 * Maps a raw processing stage string to one of the four canonical step names.
 * Shared by ProcessingCard, HomeScreen, and MaterialsScreen.
 */
fun deriveProcessingStep(stage: String): String {
    val lower = stage.lowercase()
    return when {
        "embed" in lower || "generat" in lower -> "Embed"
        "chunk" in lower                       -> "Chunk"
        "extract" in lower || "read" in lower || "pars" in lower || "load" in lower -> "Extract"
        "complete" in lower || "done" in lower || "finish" in lower -> "Done"
        else -> "Extract"
    }
}

/**
 * Pipeline step indicator with pulsing glow on the active step.
 */
@Composable
fun PipelineStep(
    label: String,
    stepNumber: Int,
    state: PipelineStepState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = EaseInOut),
            RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val appColors = MaterialTheme.appColors
    val circleBg = when (state) {
        PipelineStepState.COMPLETED -> appColors.stepComplete
        PipelineStepState.ACTIVE    -> appColors.stepActive
        PipelineStepState.PENDING   -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val labelColor = when (state) {
        PipelineStepState.COMPLETED -> appColors.stepComplete
        PipelineStepState.ACTIVE    -> MaterialTheme.colorScheme.primary
        PipelineStepState.PENDING   -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            // Pulsing glow ring — only visible when ACTIVE
            if (state == PipelineStepState.ACTIVE) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(appColors.stepActive.copy(alpha = pulseAlpha * 0.5f), CircleShape)
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp).clip(CircleShape).background(circleBg)
            ) {
                when (state) {
                    PipelineStepState.COMPLETED ->
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    PipelineStepState.ACTIVE ->
                        CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                    PipelineStepState.PENDING ->
                        Text("$stepNumber", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = labelColor, maxLines = 1)
    }
}

/**
 * Processing progress card — surface-based, purple glow, gradient progress bar.
 */
@Composable
fun ProcessingCard(
    materialName: String,
    progress: Float,
    currentStep: String,
    statusText: String,
    modifier: Modifier = Modifier,
    queueCount: Int = 1
) {
    val steps = listOf("Extract", "Chunk", "Embed", "Done")
    val currentStepIndex = steps.indexOfFirst { it.equals(currentStep, ignoreCase = true) }
        .coerceAtLeast(0)
    val pct = (progress * 100).toInt()

    val appColors = MaterialTheme.appColors
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(appColors.glassBorderAccent, appColors.glassBorderDefault)
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Box {
            // Ambient glow at top — derived from primary, very subtle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Brush.verticalGradient(listOf(appColors.primaryGlow, Color.Transparent)))
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // ── Header ──────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(44.dp).background(appColors.glassBorderAccent, CircleShape))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp).background(scheme.primary.copy(alpha = 0.28f), CircleShape)
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = scheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Processing Materials", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "$queueCount item${if (queueCount > 1) "s" else ""} in queue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // ── Document name + % badge ─────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = materialName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(appColors.glassBorderAccent, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text("$pct%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Pipeline steps with connector lines ─────────────
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    steps.forEachIndexed { index, label ->
                        val stepState = when {
                            index < currentStepIndex  -> PipelineStepState.COMPLETED
                            index == currentStepIndex -> PipelineStepState.ACTIVE
                            else                      -> PipelineStepState.PENDING
                        }
                        PipelineStep(label = label, stepNumber = index + 1, state = stepState, modifier = Modifier.weight(1f))
                        if (index < steps.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 14.dp)
                                    .height(2.dp)
                                    .weight(0.4f)
                                    .background(
                                        if (index < currentStepIndex) appColors.stepComplete.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Status text ──────────────────────────────────────
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Gradient progress bar ────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(scheme.surfaceContainerHighest)
                ) {
                    val p = progress.coerceIn(0f, 1f)
                    if (p > 0f) {
                        Box(
                            modifier = Modifier.fillMaxWidth(p).fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(appColors.progressStart, appColors.progressEnd)))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("$pct%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

/**
 * Storage breakdown bar with label, size, and colored progress.
 */
@Composable
fun StorageBar(
    label: String,
    icon: ImageVector,
    sizeText: String,
    percentage: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = sizeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
        )
    }
}
