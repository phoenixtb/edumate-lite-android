package io.foxbird.edumate.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edgeai.engine.EngineType
import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.ui.components.IconContainer
import io.foxbird.edumate.ui.components.SectionHeader
import io.foxbird.edumate.ui.components.StatusChip
import io.foxbird.edumate.ui.theme.StatusActive
import io.foxbird.edumate.ui.theme.StatusActiveContainer
import io.foxbird.edumate.ui.theme.StatusDownloading
import io.foxbird.edumate.ui.theme.StatusDownloadingContainer
import io.foxbird.edumate.ui.theme.ThemeMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateToDevTools: () -> Unit = {}
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val modelStates by viewModel.modelStates.collectAsStateWithLifecycle()
    val activeModelId by viewModel.activeInferenceModelId.collectAsStateWithLifecycle()
    val materialCount by viewModel.materialCount.collectAsStateWithLifecycle()
    val chunkCount by viewModel.chunkCount.collectAsStateWithLifecycle()
    val loadingModelId by viewModel.isLoadingModel.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data") },
            text = {
                Text(
                    "This will permanently delete all materials, chunks, conversations, " +
                        "and messages. This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearDialog = false
                }) {
                    Text("Delete", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 1. Appearance ──────────────────────────────────────────
        SectionHeader("Appearance")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconContainer(
                        icon = Icons.Filled.Palette,
                        containerColor = Color(0xFF2A2040),
                        iconColor = Color(0xFFFFAB00)
                    )
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))

                val themeIcons = listOf(
                    Icons.Filled.LightMode,
                    Icons.Filled.SettingsBrightness,
                    Icons.Filled.DarkMode
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = prefs.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size
                            ),
                            icon = {
                                Icon(
                                    themeIcons[index],
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        ) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 2. Processing ──────────────────────────────────────────
        SectionHeader("Processing")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconContainer(
                    icon = Icons.Filled.AutoAwesome,
                    containerColor = Color(0xFF1B3A4E),
                    iconColor = Color(0xFF7C4DFF)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Extract Concepts During Processing",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Uses AI to identify key concepts while importing materials. " +
                            "Slower but automatic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = prefs.extractConcepts,
                    onCheckedChange = { viewModel.setExtractConcepts(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 3. Developer ───────────────────────────────────────────
        SectionHeader("Developer")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconContainer(
                        icon = Icons.Filled.Code,
                        containerColor = Color(0xFF1B2A1B),
                        iconColor = Color(0xFF69F0AE)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Developer Mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Enable dev tools and diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.developerMode,
                        onCheckedChange = { viewModel.setDeveloperMode(it) }
                    )
                }

                if (prefs.developerMode) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDevTools() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Debug Console",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "View chunks, embeddings & logs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 4. Data ────────────────────────────────────────────────
        SectionHeader("Data")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconContainer(
                        icon = Icons.Filled.Storage,
                        containerColor = Color(0xFF1B2A4E),
                        iconColor = Color(0xFF448AFF)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Materials", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$materialCount materials · $chunkCount chunks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconContainer(
                        icon = Icons.Filled.DeleteForever,
                        containerColor = Color(0xFF3A1B1B),
                        iconColor = Color(0xFFFF5252)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear All Data", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Delete all materials and chats",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 5. AI Models ───────────────────────────────────────────
        SectionHeader("AI Models")

        val activeModelName = activeModelId?.let { AppModelConfigs.findById(it)?.name } ?: "None"
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Active for text: $activeModelName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppModelConfigs.ALL.forEach { config ->
            val state = modelStates[config.id] ?: ModelState.NotDownloaded
            val canRun = viewModel.modelManager.canRunModel(config)
            val isThisLoading = loadingModelId == config.id

            ModelCard(
                config = config,
                state = state,
                canRun = canRun,
                isLoading = isThisLoading,
                onDownload = { viewModel.downloadModel(config) },
                onLoad = { viewModel.loadModel(config) },
                onUnload = { viewModel.unloadModel(config) },
                onDelete = { viewModel.deleteModel(config) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 6. About ───────────────────────────────────────────────
        SectionHeader("About")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("EduMate Lite", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Powered by", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "On-device AI models",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ModelCard(
    config: ModelConfig,
    state: ModelState,
    canRun: Boolean,
    isLoading: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit
) {
    val engineIcon = when (config.engineType) {
        EngineType.LITE_RT -> Icons.Filled.Memory
        EngineType.LLAMA_CPP -> Icons.Filled.Terminal
    }
    val engineLabel = when (config.engineType) {
        EngineType.LITE_RT -> "LiteRT"
        EngineType.LLAMA_CPP -> "llama.cpp"
    }
    val engineChipColor = when (config.engineType) {
        EngineType.LITE_RT -> Color(0xFF00897B)
        EngineType.LLAMA_CPP -> Color(0xFF448AFF)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconContainer(
                    icon = engineIcon,
                    containerColor = engineChipColor.copy(alpha = 0.15f),
                    iconColor = engineChipColor
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(config.name, style = MaterialTheme.typography.titleSmall)
                        StatusChip(
                            text = engineLabel,
                            containerColor = engineChipColor.copy(alpha = 0.15f),
                            textColor = engineChipColor
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        config.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        config.fileSizeDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                ModelStateAction(
                    config = config,
                    state = state,
                    canRun = canRun,
                    isLoading = isLoading,
                    onDownload = onDownload,
                    onLoad = onLoad,
                    onUnload = onUnload,
                    onDelete = onDelete
                )
            }

            if (state is ModelState.Downloading) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = StatusDownloading,
                        trackColor = StatusDownloadingContainer,
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        "${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusDownloading
                    )
                }
            }

            if (!canRun) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Insufficient RAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelStateAction(
    config: ModelConfig,
    state: ModelState,
    canRun: Boolean,
    isLoading: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit
) {
    when {
        state is ModelState.Downloading -> {
            StatusChip(
                text = "Downloading",
                containerColor = StatusDownloadingContainer,
                textColor = StatusDownloading
            )
        }

        state is ModelState.Loading || (isLoading && state !is ModelState.Ready) -> {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        state is ModelState.Ready -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusChip(
                    text = "Active",
                    containerColor = StatusActiveContainer,
                    textColor = StatusActive
                )
                OverflowMenu(
                    showUnload = true,
                    showDelete = false,
                    onUnload = onUnload,
                    onDelete = onDelete
                )
            }
        }

        state is ModelState.Downloaded -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (canRun) {
                    IconButton(onClick = onLoad, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Load model",
                            tint = StatusActive
                        )
                    }
                } else {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Cannot load",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                OverflowMenu(
                    showUnload = false,
                    showDelete = !config.isBundled,
                    onUnload = onUnload,
                    onDelete = onDelete
                )
            }
        }

        state is ModelState.NotDownloaded -> {
            IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Download model",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        state is ModelState.DownloadFailed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusChip(
                    text = "Failed",
                    containerColor = Color(0xFF3A1B1B),
                    textColor = Color(0xFFFF5252)
                )
                IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "Retry download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        state is ModelState.LoadFailed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusChip(
                    text = "Load failed",
                    containerColor = Color(0xFF3A1B1B),
                    textColor = Color(0xFFFF5252)
                )
                if (canRun) {
                    IconButton(onClick = onLoad, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Retry load",
                            tint = StatusActive
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(
    showUnload: Boolean,
    showDelete: Boolean,
    onUnload: () -> Unit,
    onDelete: () -> Unit
) {
    if (!showUnload && !showDelete) return

    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (showUnload) {
                DropdownMenuItem(
                    text = { Text("Unload") },
                    onClick = { onUnload(); expanded = false },
                    leadingIcon = {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                )
            }
            if (showDelete) {
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFFF5252)) },
                    onClick = { onDelete(); expanded = false },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}
