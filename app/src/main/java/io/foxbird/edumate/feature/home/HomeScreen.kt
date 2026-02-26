package io.foxbird.edumate.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edgeai.model.ModelState
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import io.foxbird.edumate.ui.components.ProcessingCard
import io.foxbird.edumate.ui.components.deriveProcessingStep
import io.foxbird.edumate.ui.components.QuickActionCard
import io.foxbird.edumate.ui.components.SectionHeader
import io.foxbird.edumate.ui.theme.EduBlue
import io.foxbird.edumate.ui.theme.EduPurple
import io.foxbird.edumate.ui.theme.EduPurpleLight
import io.foxbird.edumate.ui.theme.EduTeal
import io.foxbird.edumate.ui.theme.StatusActive
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToWorksheet: () -> Unit = {},
    onNavigateToKnowledgeGraph: () -> Unit = {},
    onNavigateToMaterialDetail: (Long) -> Unit = {},
    onNavigateToModelManager: () -> Unit = {}
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val recentMaterials by viewModel.recentDocuments.collectAsStateWithLifecycle()
    val inferenceModelState by viewModel.inferenceModelState.collectAsStateWithLifecycle()
    val activeModelName by viewModel.activeModelName.collectAsStateWithLifecycle()
    val processingState by viewModel.processingState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshStats() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = EduPurple
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "EduMate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "On-device AI study companion",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Live status color — green=ready, purple=loading, red=error
                    IconButton(onClick = onNavigateToModelManager) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = "AI & Resources",
                            tint = when (inferenceModelState) {
                                is ModelState.Ready -> StatusActive
                                is ModelState.Loading -> EduPurpleLight
                                is ModelState.LoadFailed -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── AI Status Card (conditional — hidden when model ready + materials present) ──
            val isModelReady = inferenceModelState is ModelState.Ready
            val hasMaterials = stats.completedDocuments > 0
            val showAiCard = !isModelReady || !hasMaterials

            if (showAiCard) {
                AiStatusCard(
                    stats = stats,
                    modelState = inferenceModelState,
                    activeModelName = activeModelName,
                    onGoToLibrary = onNavigateToLibrary,
                    onStartChat = onNavigateToChat,
                    onGoToSettings = onNavigateToModelManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Active processing card ─────────────────────────────────
            processingState?.let { ps ->
                ProcessingCard(
                    materialName = ps.documentName,
                    progress = ps.progress,
                    currentStep = deriveProcessingStep(ps.stage),
                    statusText = ps.stage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Stats Strip (when ready + materials) ──────────────────
            if (isModelReady && hasMaterials) {
                StatsStrip(
                    materials = stats.completedDocuments,
                    chunks = stats.chunkCount,
                    sessions = stats.conversationCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Quick Actions ──────────────────────────────────────────
            SectionHeader(title = "Quick Actions")

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "Ask AI",
                    subtitle = "Start a conversation",
                    iconColor = EduPurple,
                    onClick = onNavigateToChat,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.Upload,
                    title = "Add Material",
                    subtitle = "PDF or image",
                    iconColor = EduBlue,
                    onClick = onNavigateToLibrary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Filled.Description,
                    title = "Worksheet",
                    subtitle = "Generate practice",
                    iconColor = EduTeal,
                    onClick = onNavigateToWorksheet,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.AccountTree,
                    title = "Knowledge",
                    subtitle = "Explore concepts",
                    iconColor = EduPurpleLight,
                    onClick = onNavigateToKnowledgeGraph,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Recent Materials ───────────────────────────────────────
            if (recentMaterials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                SectionHeader(
                    title = "Recent Materials",
                    action = "See All",
                    onAction = onNavigateToLibrary
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentMaterials, key = { it.id }) { material ->
                        RecentMaterialCard(
                            material = material,
                            onClick = { onNavigateToMaterialDetail(material.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Subcomponents ──────────────────────────────────────────────────────────────

@Composable
private fun AiStatusCard(
    stats: HomeStats,
    modelState: ModelState,
    activeModelName: String?,
    onGoToLibrary: () -> Unit,
    onStartChat: () -> Unit,
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isModelReady = modelState is ModelState.Ready
    val isModelLoading = modelState is ModelState.Loading
    val isModelFailed = modelState is ModelState.LoadFailed
    val isModelNotLoaded = !isModelReady && !isModelLoading && !isModelFailed
    val hasMaterials = stats.completedDocuments > 0

    val bgColor by animateColorAsState(
        targetValue = when {
            isModelFailed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            isModelNotLoaded -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        },
        label = "aiCardBg"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon area
                Surface(
                    shape = CircleShape,
                    color = when {
                        isModelFailed -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        isModelNotLoaded -> EduPurple.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isModelLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    isModelNotLoaded -> Icons.Filled.RocketLaunch
                                    else -> Icons.Filled.SmartToy
                                },
                                contentDescription = null,
                                tint = when {
                                    isModelFailed -> MaterialTheme.colorScheme.error
                                    isModelNotLoaded -> EduPurple
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isModelLoading -> "Starting up your tutor..."
                            isModelReady && hasMaterials -> "Tutor Ready"
                            isModelReady -> "Tutor Ready"
                            isModelFailed -> "Tutor Offline"
                            else -> "Tutor on Standby"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            isModelLoading -> "Loading ${activeModelName ?: "AI model"} — hang tight!"
                            isModelReady && hasMaterials ->
                                "${stats.completedDocuments} material${if (stats.completedDocuments > 1) "s" else ""} · ${stats.chunkCount} chunks indexed"
                            isModelReady -> "Add your study materials to get started"
                            isModelFailed -> modelState.error
                            else -> "Your AI tutor isn't loaded yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isModelFailed)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action button
                when {
                    isModelReady && hasMaterials -> {
                        FilledTonalButton(
                            onClick = onStartChat,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) { Text("Chat") }
                    }
                    isModelReady -> {
                        FilledTonalButton(
                            onClick = onGoToLibrary,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) { Text("Add") }
                    }
                    isModelNotLoaded || isModelFailed -> {
                        Button(
                            onClick = onGoToSettings,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) { Text("Manage") }
                    }
                }
            }

            if (isModelLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun StatsStrip(
    materials: Int,
    chunks: Int,
    sessions: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                value = "$materials",
                label = "Materials",
                tint = EduBlue
            )
            VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            StatItem(
                icon = Icons.Filled.Widgets,
                value = "$chunks",
                label = "Chunks",
                tint = EduPurple
            )
            VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            StatItem(
                icon = Icons.AutoMirrored.Filled.Chat,
                value = "$sessions",
                label = "Sessions",
                tint = EduTeal
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentMaterialCard(
    material: DocumentEntity,
    onClick: () -> Unit
) {
    val typeIcon = when (material.sourceType.lowercase()) {
        "pdf" -> Icons.Filled.PictureAsPdf
        "image" -> Icons.Filled.Image
        else -> Icons.Filled.Description
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EduPurple.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = EduPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = material.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = material.sourceType.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
