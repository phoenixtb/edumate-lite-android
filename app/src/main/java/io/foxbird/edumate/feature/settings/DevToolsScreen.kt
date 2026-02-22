package io.foxbird.edumate.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.foxbird.edgeai.engine.EngineOrchestrator
import io.foxbird.edgeai.engine.ModelManager
import io.foxbird.edgeai.model.ModelDownloader
import io.foxbird.edgeai.util.DeviceInfo
import io.foxbird.edumate.data.local.dao.ChunkDao
import io.foxbird.edumate.data.local.dao.ConversationDao
import io.foxbird.edumate.data.local.dao.MaterialDao
import io.foxbird.edumate.data.local.entity.ChunkEntity
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.domain.service.AiTask
import io.foxbird.edumate.domain.service.TaskQueue
import io.foxbird.edumate.domain.service.TaskStatus
import io.foxbird.edumate.ui.components.SectionHeader
import io.foxbird.edumate.ui.components.StatusChip
import io.foxbird.edumate.ui.components.StorageBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsScreen(onBack: () -> Unit) {
    val chunkDao: ChunkDao = koinInject()
    val materialDao: MaterialDao = koinInject()
    val conversationDao: ConversationDao = koinInject()
    val deviceInfo: DeviceInfo = koinInject()
    val orchestrator: EngineOrchestrator = koinInject()
    val modelManager: ModelManager = koinInject()
    val taskQueue: TaskQueue = koinInject()
    val modelDownloader: ModelDownloader = koinInject()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }

    var chunks by remember { mutableStateOf<List<ChunkEntity>>(emptyList()) }
    var materials by remember { mutableStateOf<List<MaterialEntity>>(emptyList()) }
    var conversationCount by remember { mutableIntStateOf(0) }

    var cacheSize by remember { mutableLongStateOf(0L) }
    var modelsSize by remember { mutableLongStateOf(0L) }
    var dbSize by remember { mutableLongStateOf(0L) }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        withContext(Dispatchers.IO) {
            chunks = chunkDao.getAll()
            materials = materialDao.getAll()
            conversationCount = conversationDao.getCount()
            cacheSize = context.cacheDir.walkTopDown().sumOf { it.length() }
            dbSize = context.getDatabasePath("edumate.db")?.length() ?: 0L
            modelsSize = modelDownloader.getModelsDirectory().walkTopDown().sumOf { it.length() }
        }
    }

    val tasks by taskQueue.tasks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Console") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Flag, "Flag")
                    }
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Chunks") },
                    icon = { Icon(Icons.Filled.ViewModule, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Storage") },
                    icon = { Icon(Icons.Filled.Storage, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Processing") },
                    icon = { Icon(Icons.Filled.Memory, null) }
                )
            }

            when (selectedTab) {
                0 -> ChunksTab(
                    chunks = chunks,
                    materials = materials
                )
                1 -> StorageTab(
                    cacheSize = cacheSize,
                    modelsSize = modelsSize,
                    dbSize = dbSize,
                    chunkCount = chunks.size,
                    materialCount = materials.size,
                    conversationCount = conversationCount
                )
                2 -> ProcessingTab(tasks = tasks)
            }
        }
    }
}

// region Chunks Tab

@Composable
private fun ChunksTab(
    chunks: List<ChunkEntity>,
    materials: List<MaterialEntity>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMaterialId by remember { mutableStateOf<Long?>(null) }
    var expandedChunkIds by remember { mutableStateOf(emptySet<Long>()) }

    val materialMap = remember(materials) { materials.associateBy { it.id } }

    val filteredChunks = remember(chunks, searchQuery, selectedMaterialId) {
        chunks.filter { chunk ->
            val matchesSearch = searchQuery.isBlank() ||
                chunk.content.contains(searchQuery, ignoreCase = true)
            val matchesMaterial = selectedMaterialId == null ||
                chunk.materialId == selectedMaterialId
            matchesSearch && matchesMaterial
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search chunks...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            singleLine = true
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedMaterialId == null,
                    onClick = { selectedMaterialId = null },
                    label = { Text("All") },
                    leadingIcon = if (selectedMaterialId == null) {
                        {
                            Icon(
                                Icons.Filled.ViewModule,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }
            items(materials) { material ->
                FilterChip(
                    selected = selectedMaterialId == material.id,
                    onClick = {
                        selectedMaterialId = if (selectedMaterialId == material.id) null
                        else material.id
                    },
                    label = {
                        Text(
                            text = material.title.take(20),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Text(
            text = "${filteredChunks.size} chunks",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 8.dp
            )
        ) {
            items(filteredChunks, key = { it.id }) { chunk ->
                val isExpanded = expandedChunkIds.contains(chunk.id)
                ChunkCard(
                    chunk = chunk,
                    materialName = materialMap[chunk.materialId]?.title,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedChunkIds = if (isExpanded) {
                            expandedChunkIds - chunk.id
                        } else {
                            expandedChunkIds + chunk.id
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChunkCard(
    chunk: ChunkEntity,
    materialName: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        text = "ID: ${chunk.id}",
                        containerColor = Color(0xFF9C27B0).copy(alpha = 0.15f),
                        textColor = Color(0xFF9C27B0)
                    )
                    StatusChip(
                        text = chunk.chunkType,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isExpanded) {
                Text(
                    text = chunk.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = chunk.content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// endregion

// region Storage Tab

@Composable
private fun StorageTab(
    cacheSize: Long,
    modelsSize: Long,
    dbSize: Long,
    chunkCount: Int,
    materialCount: Int,
    conversationCount: Int
) {
    val totalSize = cacheSize + modelsSize + dbSize
    val safeDivisor = totalSize.coerceAtLeast(1).toFloat()

    val otherCacheSize = (cacheSize - modelsSize).coerceAtLeast(0)

    // Rough breakdown: treat DB size as split among chunks, materials, chat
    val chunkDbFraction = 0.5f
    val materialDbFraction = 0.3f
    val chatDbFraction = 0.2f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp, vertical = 16.dp
        )
    ) {
        // Hero card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = formatSize(totalSize),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Total App Storage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Cache & Models
        item {
            SectionHeader(title = "Cache & Models")
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StorageBar(
                        label = "ML Model Cache",
                        icon = Icons.Filled.Memory,
                        sizeText = formatSize(modelsSize),
                        percentage = (modelsSize / safeDivisor).coerceIn(0f, 1f),
                        barColor = Color(0xFFE91E63)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StorageBar(
                        label = "Other Cache",
                        icon = Icons.Filled.Folder,
                        sizeText = formatSize(otherCacheSize),
                        percentage = (otherCacheSize / safeDivisor).coerceIn(0f, 1f),
                        barColor = Color(0xFFFF9800)
                    )
                }
            }
        }

        // Database
        item {
            SectionHeader(title = "Database")
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StorageBar(
                        label = "Chunks Data",
                        icon = Icons.Filled.ViewModule,
                        sizeText = formatSize((dbSize * chunkDbFraction).toLong()),
                        percentage = (dbSize * chunkDbFraction / safeDivisor).coerceIn(0f, 1f),
                        barColor = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StorageBar(
                        label = "Materials Data",
                        icon = Icons.Filled.Description,
                        sizeText = formatSize((dbSize * materialDbFraction).toLong()),
                        percentage = (dbSize * materialDbFraction / safeDivisor).coerceIn(0f, 1f),
                        barColor = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StorageBar(
                        label = "Chat History",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        sizeText = formatSize((dbSize * chatDbFraction).toLong()),
                        percentage = (dbSize * chatDbFraction / safeDivisor).coerceIn(0f, 1f),
                        barColor = Color(0xFF9C27B0)
                    )
                }
            }
        }

        // Data Stats
        item {
            SectionHeader(title = "Data Stats")
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DataStatRow("Total Chunks", "$chunkCount")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DataStatRow("Total Materials", "$materialCount")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    DataStatRow("Total Conversations", "$conversationCount")
                }
            }
        }
    }
}

@Composable
private fun DataStatRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

// endregion

// region Processing Tab

@Composable
private fun ProcessingTab(tasks: List<AiTask>) {
    val activeTasks = tasks.filter {
        it.status == TaskStatus.PENDING || it.status == TaskStatus.RUNNING
    }

    if (activeTasks.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No active tasks",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 16.dp
            )
        ) {
            items(activeTasks, key = { it.id }) { task ->
                TaskCard(task)
            }
        }
    }
}

@Composable
private fun TaskCard(task: AiTask) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Status: ${task.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Progress: ${(task.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

// endregion
