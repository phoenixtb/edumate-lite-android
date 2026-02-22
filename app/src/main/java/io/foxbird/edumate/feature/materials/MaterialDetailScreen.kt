package io.foxbird.edumate.feature.materials

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edumate.data.local.entity.ConceptEntity
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.ui.components.IconContainer
import io.foxbird.edumate.ui.components.StatusChip
import io.foxbird.edumate.ui.theme.EduBlue
import io.foxbird.edumate.ui.theme.EduGreen
import io.foxbird.edumate.ui.theme.EduPurple
import io.foxbird.edumate.ui.theme.EduTeal
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDetailScreen(
    materialId: Long,
    onBack: () -> Unit = {}
) {
    val viewModel: MaterialDetailViewModel = koinViewModel { parametersOf(materialId) }
    val material by viewModel.material.collectAsStateWithLifecycle()
    val chunkCount by viewModel.chunkCount.collectAsStateWithLifecycle()
    val concepts by viewModel.concepts.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val tabs = listOf("Overview", "Concepts", "Related")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        material?.title ?: "Material Detail",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.deleteMaterial()
                                    onBack()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> OverviewTab(material, chunkCount, concepts)
                    1 -> ConceptsTab(concepts)
                    2 -> RelatedTab()
                }
            }
        }
    }
}

// -- Overview Tab --

@Composable
private fun OverviewTab(
    material: MaterialEntity?,
    chunkCount: Int,
    concepts: List<ConceptEntity>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OverviewStatCard(
                icon = Icons.Filled.Dashboard,
                label = "Chunks",
                value = "$chunkCount",
                iconColor = EduBlue,
                modifier = Modifier.weight(1f)
            )
            OverviewStatCard(
                icon = Icons.Filled.Lightbulb,
                label = "Concepts",
                value = "${concepts.size}",
                iconColor = Color(0xFFFFAB00),
                modifier = Modifier.weight(1f)
            )
            OverviewStatCard(
                icon = Icons.Filled.AccountTree,
                label = "Related",
                value = "0",
                iconColor = EduTeal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(
                    "Source Type",
                    material?.sourceType?.replaceFirstChar { it.uppercase() } ?: "Unknown"
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                DetailRow("Subject", material?.subject ?: "General")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                DetailRow("Grade Level", "N/A")
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                DetailRow(
                    "Status",
                    material?.status?.replaceFirstChar { it.uppercase() } ?: "Unknown"
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                DetailRow("Processed", formatDetailDate(material?.processedAt))
            }
        }

        if (concepts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Top Concepts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                concepts.take(5).forEach { concept ->
                    StatusChip(
                        text = concept.name,
                        containerColor = EduPurple.copy(alpha = 0.12f),
                        textColor = EduPurple
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// -- Concepts Tab --

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConceptsTab(concepts: List<ConceptEntity>) {
    var selectedFilter by remember { mutableStateOf("all") }

    val termCount = concepts.count { it.type == "term" }
    val filteredConcepts = when (selectedFilter) {
        "terms" -> concepts.filter { it.type == "term" }
        else -> concepts
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "all",
                onClick = { selectedFilter = "all" },
                label = { Text("All ${concepts.size}") }
            )
            FilterChip(
                selected = selectedFilter == "terms",
                onClick = { selectedFilter = "terms" },
                label = { Text("Terms $termCount") }
            )
        }

        if (filteredConcepts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No concepts found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredConcepts, key = { it.id }) { concept ->
                    ConceptGridCard(concept)
                }
            }
        }
    }
}

@Composable
private fun ConceptGridCard(concept: ConceptEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconContainer(
                icon = Icons.Filled.Lightbulb,
                containerColor = Color(0xFFFFAB00).copy(alpha = 0.15f),
                iconColor = Color(0xFFFFAB00),
                size = 32.dp,
                iconSize = 16.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    concept.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "×${concept.frequency}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -- Related Tab --

@Composable
private fun RelatedTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.LinkOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No related materials yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -- Utility --

private fun formatDetailDate(timestamp: Long?): String {
    if (timestamp == null) return "N/A"
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
