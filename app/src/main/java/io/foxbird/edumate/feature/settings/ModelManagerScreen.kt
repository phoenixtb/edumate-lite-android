package io.foxbird.edumate.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import io.foxbird.edumate.ui.theme.appColors
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
        containerColor = Color.Transparent,
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
                    Icon(
                        Icons.Filled.Memory,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp).padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, StatusActive.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    // Subtle green glow at top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Brush.verticalGradient(listOf(StatusActive.copy(alpha = 0.10f), Color.Transparent)))
                    )
                    // Left active stripe
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawRect(
                                    brush = Brush.verticalGradient(listOf(StatusActive, StatusActive.copy(alpha = 0.4f))),
                                    size = Size(3.5.dp.toPx(), size.height),
                                )
                            }
                    )
                    Row(
                        modifier = Modifier.padding(start = 15.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StatusActive.copy(alpha = 0.18f))
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = StatusActive, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Active — Ready to help", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = StatusActive)
                            Text("${activeModelConfig.name} is loaded and running", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val scheme = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    val isActive = state is ModelState.Ready

    val engineLabel = when (config.engineType) {
        EngineType.LITE_RT -> "LiteRT"
        EngineType.LLAMA_CPP -> "llama.cpp"
    }
    val engineChipColor = when (config.engineType) {
        EngineType.LITE_RT -> scheme.tertiary
        EngineType.LLAMA_CPP -> scheme.primary
    }
    val purposeDescription = when (config.purpose) {
        ModelPurpose.INFERENCE -> "Inference · Chat & answers"
        ModelPurpose.EMBEDDING -> "Embedding · Semantic search"
    }
    val accentColor = if (isActive) StatusActive else scheme.primary
    val cardBorder = when {
        isActive -> StatusActive.copy(alpha = 0.30f)
        else -> appColors.glassBorderDefault
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerLow)
    ) {
        // Top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.09f), Color.Transparent)))
        )

        Column(modifier = Modifier.padding(14.dp)) {
            // ── Header: name (primary) + purpose tag + active dot ───────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    // Model name as primary title
                    Text(config.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    Spacer(Modifier.height(3.dp))
                    // Purpose + engine chips on one row
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusChip(text = purposeDescription, containerColor = scheme.secondaryContainer.copy(alpha = 0.5f), textColor = scheme.onSecondaryContainer)
                        StatusChip(text = engineLabel, containerColor = engineChipColor.copy(alpha = 0.14f), textColor = engineChipColor)
                        if (config.isBundled) {
                            StatusChip(text = "Bundled", containerColor = scheme.tertiaryContainer.copy(alpha = 0.5f), textColor = scheme.onTertiaryContainer)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(config.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                if (isActive) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(StatusActive.copy(alpha = 0.18f))
                    ) {
                        Icon(Icons.Filled.CheckCircle, "Active", tint = StatusActive, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Spec pills ───────────────────────────────────────────────────
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModelSpecPill(label = "Size", value = config.fileSizeDisplay, accentColor = scheme.primary)
                ModelSpecPill(label = "Min RAM", value = "${config.requiredRamGB} GB", accentColor = scheme.secondary)
                ModelSpecPill(label = "Context", value = "${config.contextLength / 1024}K", accentColor = scheme.tertiary)
            }

            if (!canRun) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.errorContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(12.dp), tint = scheme.error)
                    Text(
                        "Needs ${config.requiredRamGB} GB total RAM — device may not support this model",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.error
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Actions ───────────────────────────────────────────────────────
            when {
                state is ModelState.NotDownloaded -> {
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download ${config.fileSizeDisplay}")
                    }
                }

                state is ModelState.Downloading -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Downloading… ${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = StatusDownloading)
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, "Cancel", Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = StatusDownloading,
                        trackColor = StatusDownloadingContainer,
                        strokeCap = StrokeCap.Round
                    )
                }

                state is ModelState.DownloadFailed -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(scheme.errorContainer.copy(alpha = 0.3f))
                            .padding(10.dp)
                    ) {
                        Text("Download failed: ${state.error}", style = MaterialTheme.typography.bodySmall, color = scheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry Download")
                    }
                }

                state is ModelState.Loading || (isLoading && state !is ModelState.Ready) -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = scheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Loading into memory…", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                state is ModelState.Ready -> {
                    FilledTonalButton(
                        onClick = onUnload,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = StatusActive.copy(alpha = 0.14f), contentColor = StatusActive)
                    ) {
                        Icon(Icons.Filled.Stop, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Unload from Memory")
                    }
                }

                state is ModelState.Downloaded -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onLoad, modifier = Modifier.weight(1f), enabled = canRun) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Load into Memory")
                        }
                        if (!config.isBundled) {
                            OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error)) {
                                Icon(Icons.Filled.Delete, "Delete", Modifier.size(18.dp))
                            }
                        }
                    }
                }

                state is ModelState.LoadFailed -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(scheme.errorContainer.copy(alpha = 0.3f))
                            .padding(10.dp)
                    ) {
                        Text("Load failed: ${state.error}", style = MaterialTheme.typography.bodySmall, color = scheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onLoad, modifier = Modifier.weight(1f), enabled = canRun) { Text("Retry") }
                        if (!config.isBundled) {
                            OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error)) {
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
private fun ModelSpecPill(label: String, value: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accentColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
internal fun DeviceResourceCard(
    memorySnapshot: MemorySnapshot,
    storageGB: Float,
    deviceSummary: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    val ramColor = when (memorySnapshot.pressure) {
        MemoryPressure.NORMAL -> Color(0xFF4CAF50)
        MemoryPressure.MODERATE -> Color(0xFFFFB300)
        MemoryPressure.CRITICAL -> scheme.error
    }
    val ramLabel = when (memorySnapshot.pressure) {
        MemoryPressure.NORMAL -> "Good"
        MemoryPressure.MODERATE -> "Getting low"
        MemoryPressure.CRITICAL -> "Very low"
    }
    val usedPercent = memorySnapshot.usedPercent
    val availableRamGB = memorySnapshot.availableMb / 1024f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, appColors.glassBorderDefault, RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerLow)
    ) {
        // Top glow from primary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Brush.verticalGradient(listOf(scheme.primary.copy(alpha = 0.09f), Color.Transparent)))
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Device summary row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(scheme.primary.copy(alpha = 0.13f))
                ) {
                    Icon(Icons.Filled.Memory, null, tint = scheme.primary, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(deviceSummary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Storage, null, Modifier.size(11.dp), tint = scheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Text("%.1f GB storage free".format(storageGB), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                }
                // RAM pressure badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ramColor.copy(alpha = 0.13f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(ramLabel, style = MaterialTheme.typography.labelSmall, color = ramColor, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.25f))

            // Physical RAM bar
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RAM", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text("%.1f GB free".format(availableRamGB), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
                LinearProgressIndicator(
                    progress = { usedPercent },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = ramColor,
                    trackColor = ramColor.copy(alpha = 0.13f),
                    strokeCap = StrokeCap.Round
                )
            }

            // Swap bar — only if present
            if (memorySnapshot.hasSwap) {
                val swapUsed = memorySnapshot.swapTotalMb - memorySnapshot.swapFreeMb
                val swapUsedPercent = if (memorySnapshot.swapTotalMb > 0) swapUsed.toFloat() / memorySnapshot.swapTotalMb else 0f
                val swapFreeGB = memorySnapshot.swapFreeMb / 1024f
                val swapTotalGB = memorySnapshot.swapTotalMb / 1024f

                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.25f))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("RAM Boost (Swap)", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        Text("%.1f / %.1f GB".format(swapFreeGB, swapTotalGB), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { swapUsedPercent },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = scheme.tertiary,
                        trackColor = scheme.tertiary.copy(alpha = 0.13f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        "Swap helps load larger models but is slower than physical RAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
