package io.foxbird.edumate.feature.materials

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.doclibrary.data.local.entity.ConceptEntity
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import io.foxbird.doclibrary.viewmodel.DocumentDetailViewModel
import io.foxbird.edumate.ui.components.IconContainer
import io.foxbird.edumate.ui.components.StatusChip
import io.foxbird.edumate.ui.theme.appColors
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
    val viewModel: DocumentDetailViewModel = koinViewModel { parametersOf(materialId) }
    val document by viewModel.document.collectAsStateWithLifecycle()
    val chunkCount by viewModel.chunkCount.collectAsStateWithLifecycle()
    val concepts by viewModel.concepts.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(document?.title ?: "Document Detail", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "More options")
                        }
                        DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { showOverflowMenu = false; viewModel.deleteDocument(); onBack() },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Tab row with badges
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    BadgedBox(badge = {
                        if (concepts.isNotEmpty()) Badge { Text("${concepts.size}") }
                    }) {
                        Text("Concepts", modifier = Modifier.padding(end = if (concepts.isNotEmpty()) 8.dp else 0.dp))
                    }
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    BadgedBox(badge = { Badge { Text("0") } }) {
                        Text("Related", modifier = Modifier.padding(end = 8.dp))
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> OverviewTab(document, chunkCount, concepts)
                    1 -> ConceptsTab(concepts)
                    2 -> RelatedTab()
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    document: DocumentEntity?,
    chunkCount: Int,
    concepts: List<ConceptEntity>
) {
    val displayedConcepts = concepts.take(5)
    val remainingCount = (concepts.size - 5).coerceAtLeast(0)

    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // Stat cards
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OverviewStatCard(Icons.Filled.Dashboard, "Chunks", "$chunkCount", scheme.primary, Modifier.weight(1f))
            OverviewStatCard(Icons.Filled.Lightbulb, "Concepts", "${concepts.size}", Color(0xFFFFAB00), Modifier.weight(1f))
            OverviewStatCard(Icons.Filled.AccountTree, "Related", "0", scheme.tertiary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Text("Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = scheme.primary)
        Spacer(Modifier.height(8.dp))

        // Glass details card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.appColors.glassBorderDefault, RoundedCornerShape(14.dp))
                .background(scheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Source Type", document?.sourceType?.replaceFirstChar { it.uppercase() } ?: "Unknown")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.outline.copy(alpha = 0.15f))
                DetailRow("Subject", document?.subject ?: "Not set")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.outline.copy(alpha = 0.15f))
                DetailRow("Grade Level", document?.gradeLevel?.let { "Grade $it" } ?: "Not set")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.outline.copy(alpha = 0.15f))
                DetailRow("Status", document?.status?.replaceFirstChar { it.uppercase() } ?: "Unknown")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.outline.copy(alpha = 0.15f))
                DetailRow("Processed", formatDetailDate(document?.processedAt))
            }
        }

        if (displayedConcepts.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Top Concepts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = scheme.primary)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                displayedConcepts.forEach { concept ->
                    StatusChip(
                        text = "${concept.name}  ${concept.frequency}",
                        containerColor = scheme.primary.copy(alpha = 0.12f),
                        textColor = scheme.primary
                    )
                }
            }

            if (remainingCount > 0) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = {}) {
                    Text("View all ${concepts.size} concepts")
                }
            }
        }
    }
}

@Composable
private fun OverviewStatCard(icon: ImageVector, label: String, value: String, iconColor: Color, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, appColors.glassBorderDefault, RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerLow)
    ) {
        // Top tinted glow from the icon color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Brush.verticalGradient(listOf(iconColor.copy(alpha = 0.10f), Color.Transparent)))
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconColor.copy(alpha = 0.14f))
            ) {
                Icon(icon, null, Modifier.size(20.dp), tint = iconColor)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            FilterChip(selected = selectedFilter == "all", onClick = { selectedFilter = "all" }, label = { Text("All ${concepts.size}") })
            FilterChip(selected = selectedFilter == "terms", onClick = { selectedFilter = "terms" }, label = { Text("Terms $termCount") })
        }
        if (filteredConcepts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No concepts found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredConcepts, key = { it.id }) { concept -> ConceptGridCard(concept) }
            }
        }
    }
}

@Composable
private fun ConceptGridCard(concept: ConceptEntity) {
    val amberColor = Color(0xFFFFAB00)
    val appColors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .border(1.dp, appColors.glassBorderDefault, RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Subtle amber top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Brush.verticalGradient(listOf(amberColor.copy(alpha = 0.07f), Color.Transparent)))
        )
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconContainer(
                icon = Icons.Filled.Lightbulb,
                containerColor = amberColor.copy(alpha = 0.15f),
                iconColor = amberColor,
                size = 32.dp,
                iconSize = 16.dp
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${concept.name}  ${concept.frequency}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(concept.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RelatedTab() {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Atmospheric glow halo
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(scheme.tertiary.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(scheme.tertiaryContainer.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Filled.LinkOff, null, Modifier.size(32.dp), tint = scheme.onTertiaryContainer.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("No related materials yet", style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("Related documents will appear here", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

private fun formatDetailDate(timestamp: Long?): String {
    if (timestamp == null) return "Not set"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}
