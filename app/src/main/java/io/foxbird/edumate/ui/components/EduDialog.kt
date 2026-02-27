package io.foxbird.edumate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.foxbird.edumate.ui.theme.appColors
import androidx.compose.material3.ExperimentalMaterial3Api

// ── Shared dialog styling ────────────────────────────────────────────────────

private val DialogShape = RoundedCornerShape(20.dp)
private val SheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

/**
 * AlertDialog with glass treatment: semi-transparent surface, glass border, rounded corners.
 * Use for confirmation, form, or selection dialogs.
 */
@Composable
fun EduAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = DialogShape,
            color = scheme.surfaceContainerLow.copy(alpha = 0.90f),
            border = BorderStroke(1.dp, appColors.glassBorderDefault),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                icon?.invoke()
                if (icon != null) Spacer(Modifier.height(16.dp))
                title?.invoke()
                if (title != null) Spacer(Modifier.height(8.dp))
                text?.invoke()
                if (text != null) Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()
                    if (dismissButton != null) Spacer(Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

/**
 * Title + optional subtitle for dialogs.
 */
@Composable
fun EduDialogTitle(
    title: String,
    subtitle: String? = null,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * ModalBottomSheet with glass treatment: scheme surface, top glow line, rounded top corners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduModalBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        shape = SheetShape,
        containerColor = scheme.surfaceContainerLow.copy(alpha = 0.88f),
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(scheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        },
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top glow accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                scheme.primary.copy(alpha = 0.45f),
                                scheme.tertiary.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
            )
            content()
        }
    }
}

/**
 * Selectable option card for dialogs (e.g. ProcessingMode). Glass treatment, scheme-based accent.
 */
@Composable
fun EduSelectOption(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    badge: String? = null,
    badgeColor: Color = iconColor,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, appColors.glassBorderDefault, RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerLow.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.14f))
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}
