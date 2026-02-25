package io.foxbird.edumate.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edgeai.engine.EngineType
import io.foxbird.edgeai.engine.MemoryPressure
import io.foxbird.edgeai.engine.MemorySnapshot
import io.foxbird.edgeai.model.ModelConfig
import io.foxbird.edgeai.model.ModelPurpose
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.ui.components.SectionHeader
import io.foxbird.edumate.ui.components.StatusChip
import io.foxbird.edumate.ui.theme.StatusActive
import io.foxbird.edumate.ui.theme.StatusDownloading
import io.foxbird.edumate.ui.theme.StatusDownloadingContainer
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    val modelStates by viewModel.modelStates.collectAsStateWithLifecycle()
    val activeModelId by viewModel.activeInferenceModelId.collectAsStateWithLifecycle()
    val loadingModelId by viewModel.isLoadingModel.collectAsStateWithLifecycle()
    val memorySnapshot by viewModel.memorySnapshot.collectAsStateWithLifecycle()

    val statusColor by animateColorAsState(
        targetValue = when {
            activeModelId != null -> StatusActive
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "memIconColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI & Resources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Model management · Device health",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Live status dot — green when ready, grey otherwise
                    Icon(
                        Icons.Filled.Memory,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp).padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Device Resources ──────────────────────────────────────
            SectionHeader("Device Resources")
            DeviceResourceCard(
                memorySnapshot = memorySnapshot,
                storageGB = viewModel.modelManager.getAvailableStorageGB(),
                deviceSummary = viewModel.modelManager.getDeviceSummary(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Active Model Banner ───────────────────────────────────
            val activeModelConfig = activeModelId?.let { AppModelConfigs.findById(it) }
            if (activeModelConfig != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusActive.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StatusActive.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.CheckCircle, null,
                                    tint = StatusActive,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Active — Ready to help",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusActive
                            )
                            Text(
                                "${activeModelConfig.name} is loaded and running",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Models ────────────────────────────────────────────────
            SectionHeader("AI Models")

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
        }
    }
}

@Composable
internal fun ModelCard(
    config: ModelConfig,
    state: ModelState,
    canRun: Boolean,
    isLoading: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = state is ModelState.Ready
    val engineLabel = when (config.engineType) {
        EngineType.LITE_RT -> "LiteRT"
        EngineType.LLAMA_CPP -> "llama.cpp"
    }
    val engineChipColor = when (config.engineType) {
        EngineType.LITE_RT -> Color(0xFF00897B)
        EngineType.LLAMA_CPP -> Color(0xFF448AFF)
    }
    val purposeDescription = when (config.purpose) {
        ModelPurpose.INFERENCE -> "Reads your questions and writes helpful answers"
        ModelPurpose.EMBEDDING -> "Finds the most relevant parts of your study materials"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isActive -> StatusActive.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        purposeDescription,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            config.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusChip(
                            text = engineLabel,
                            containerColor = engineChipColor.copy(alpha = 0.15f),
                            textColor = engineChipColor
                        )
                        if (config.isBundled) {
                            StatusChip(
                                text = "Included",
                                containerColor = Color(0xFF1B3A4E),
                                textColor = Color(0xFF7C4DFF)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        config.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isActive) {
                    Surface(
                        shape = CircleShape,
                        color = StatusActive.copy(alpha = 0.18f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.CheckCircle, "Active",
                                tint = StatusActive,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ModelSpec("Size", config.fileSizeDisplay)
                ModelSpec("Min RAM", "${config.requiredRamGB} GB")
                ModelSpec("Context", "${config.contextLength / 1024}K tokens")
            }

            if (!canRun) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock, null,
                        Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                    Text(
                        "Needs ${config.requiredRamGB} GB total RAM — device may not support this model",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when {
                state is ModelState.NotDownloaded -> {
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download ${config.fileSizeDisplay}")
                    }
                }

                state is ModelState.Downloading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Downloading... ${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, "Cancel", Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = StatusDownloading,
                        trackColor = StatusDownloadingContainer,
                        strokeCap = StrokeCap.Round
                    )
                }

                state is ModelState.DownloadFailed -> {
                    Text(
                        "Download failed: ${state.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry Download")
                    }
                }

                state is ModelState.Loading || (isLoading && state !is ModelState.Ready) -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Loading model into memory...", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                state is ModelState.Ready -> {
                    FilledTonalButton(
                        onClick = onUnload,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StatusActive.copy(alpha = 0.15f),
                            contentColor = StatusActive
                        )
                    ) {
                        Icon(Icons.Filled.Stop, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Unload from Memory")
                    }
                }

                state is ModelState.Downloaded -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onLoad, modifier = Modifier.weight(1f), enabled = canRun) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Load into Memory")
                        }
                        if (!config.isBundled) {
                            OutlinedButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Filled.Delete, "Delete", Modifier.size(18.dp))
                            }
                        }
                    }
                }

                state is ModelState.LoadFailed -> {
                    Text(
                        "Load failed: ${state.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onLoad, modifier = Modifier.weight(1f), enabled = canRun) {
                            Text("Retry")
                        }
                        if (!config.isBundled) {
                            OutlinedButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Filled.Delete, "Delete", Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelSpec(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun DeviceResourceCard(
    memorySnapshot: MemorySnapshot,
    storageGB: Float,
    deviceSummary: String,
    modifier: Modifier = Modifier
) {
    val ramColor = when (memorySnapshot.pressure) {
        MemoryPressure.NORMAL -> Color(0xFF69F0AE)
        MemoryPressure.MODERATE -> Color(0xFFFFAB00)
        MemoryPressure.CRITICAL -> Color(0xFFFF5252)
    }
    val ramLabel = when (memorySnapshot.pressure) {
        MemoryPressure.NORMAL -> "Good"
        MemoryPressure.MODERATE -> "Getting low"
        MemoryPressure.CRITICAL -> "Very low — close other apps"
    }
    val usedPercent = memorySnapshot.usedPercent
    val availableRamGB = memorySnapshot.availableMb / 1024f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Memory, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        deviceSummary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Storage, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "%.1f GB storage free".format(storageGB),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Physical RAM bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "RAM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "· $ramLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = ramColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "%.1f GB free".format(availableRamGB),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { usedPercent },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color = ramColor,
                    trackColor = ramColor.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }

            // RAM Boost / Swap — only show if present
            if (memorySnapshot.hasSwap) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                val swapUsed = memorySnapshot.swapTotalMb - memorySnapshot.swapFreeMb
                val swapUsedPercent = if (memorySnapshot.swapTotalMb > 0)
                    swapUsed.toFloat() / memorySnapshot.swapTotalMb else 0f
                val swapFreeGB = memorySnapshot.swapFreeMb / 1024f
                val swapTotalGB = memorySnapshot.swapTotalMb / 1024f

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "RAM Boost (Swap)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "%.1f / %.1f GB".format(swapFreeGB, swapTotalGB),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { swapUsedPercent },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF7C4DFF).copy(alpha = 0.7f),
                        trackColor = Color(0xFF7C4DFF).copy(alpha = 0.12f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        "Swap helps load larger models but is slower than physical RAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
