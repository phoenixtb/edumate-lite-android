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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edgeai.engine.MemoryPressure
import io.foxbird.edgeai.engine.MemorySnapshot
import io.foxbird.edgeai.model.ModelState
import io.foxbird.edumate.core.model.AppModelConfigs
import io.foxbird.edumate.ui.components.IconContainer
import io.foxbird.edumate.ui.components.SectionHeader
import io.foxbird.edumate.ui.theme.StatusActive
import io.foxbird.edumate.ui.theme.ThemeMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateToDevTools: () -> Unit = {},
    onNavigateToModelManager: () -> Unit = {}
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val modelStates by viewModel.modelStates.collectAsStateWithLifecycle()
    val activeModelId by viewModel.activeInferenceModelId.collectAsStateWithLifecycle()
    val materialCount by viewModel.materialCount.collectAsStateWithLifecycle()
    val chunkCount by viewModel.chunkCount.collectAsStateWithLifecycle()
    val memorySnapshot by viewModel.memorySnapshot.collectAsStateWithLifecycle()

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

    val settingsStatusColor = when {
        modelStates.values.any { it is ModelState.Ready } -> StatusActive
        modelStates.values.any { it is ModelState.Loading } -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = onNavigateToModelManager) {
                Icon(
                    Icons.Filled.Memory,
                    contentDescription = "AI & Resources",
                    tint = settingsStatusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        AiModelsSummaryCard(
            modelStates = modelStates,
            activeModelId = activeModelId,
            memorySnapshot = memorySnapshot,
            onManage = onNavigateToModelManager,
            modifier = Modifier.fillMaxWidth()
        )

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
private fun AiModelsSummaryCard(
    modelStates: Map<String, ModelState>,
    activeModelId: String?,
    memorySnapshot: MemorySnapshot,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeModelConfig = activeModelId?.let { AppModelConfigs.findById(it) }
    val isAnyReady = modelStates.values.any { it is ModelState.Ready }
    val isAnyLoading = modelStates.values.any { it is ModelState.Loading }
    val availableRamGB = memorySnapshot.availableMb / 1024f
    val ramColor = when (memorySnapshot.pressure) {
        MemoryPressure.NORMAL -> Color(0xFF69F0AE)
        MemoryPressure.MODERATE -> Color(0xFFFFAB00)
        MemoryPressure.CRITICAL -> Color(0xFFFF5252)
    }

    Card(
        modifier = modifier.clickable(onClick = onManage),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAnyReady -> StatusActive.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isAnyReady -> StatusActive.copy(alpha = 0.2f)
                    isAnyLoading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isAnyLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            if (isAnyReady) Icons.Filled.CheckCircle else Icons.Filled.Memory,
                            contentDescription = null,
                            tint = if (isAnyReady) StatusActive else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isAnyReady -> "Tutor Ready · ${activeModelConfig?.name ?: "Model"}"
                        isAnyLoading -> "Setting up your AI tutor..."
                        else -> "No model loaded"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAnyReady) StatusActive else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Memory, null, Modifier.size(10.dp), tint = ramColor)
                    Text(
                        "%.1f GB RAM free  ·  Tap to manage".format(availableRamGB),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

