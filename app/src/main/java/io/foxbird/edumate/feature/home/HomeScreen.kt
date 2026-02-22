package io.foxbird.edumate.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.ui.components.GradientHeader
import io.foxbird.edumate.ui.components.QuickActionCard
import io.foxbird.edumate.ui.components.SectionHeader
import io.foxbird.edumate.ui.components.StatBadge
import io.foxbird.edumate.ui.theme.EduBlue
import io.foxbird.edumate.ui.theme.EduGreen
import io.foxbird.edumate.ui.theme.EduPurple
import io.foxbird.edumate.ui.theme.EduPurpleLight
import io.foxbird.edumate.ui.theme.EduTeal
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToWorksheet: () -> Unit = {},
    onNavigateToKnowledgeGraph: () -> Unit = {},
    onNavigateToMaterialDetail: (Long) -> Unit = {}
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val recentMaterials by viewModel.recentMaterials.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshStats() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader {
            Text(
                text = "EduMate",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = EduPurple,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Welcome back!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${stats.completedMaterials} materials ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBadge(
                icon = Icons.Filled.Folder,
                value = "${stats.completedMaterials}",
                label = "Materials",
                iconColor = EduBlue
            )
            StatBadge(
                icon = Icons.Filled.Widgets,
                value = "${stats.chunkCount}",
                label = "Chunks",
                iconColor = EduPurple
            )
            StatBadge(
                icon = Icons.Filled.SmartToy,
                value = if (stats.completedMaterials > 0) "Ready" else "Setup needed",
                label = "AI Status",
                iconColor = if (stats.completedMaterials > 0) EduGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                title = "Ask Question",
                subtitle = "Chat with AI",
                iconColor = EduPurple,
                onClick = onNavigateToChat,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Filled.Upload,
                title = "Add Material",
                subtitle = "PDF or Image",
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
                title = "Knowledge Graph",
                subtitle = "Explore concepts",
                iconColor = EduPurpleLight,
                onClick = onNavigateToKnowledgeGraph,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (recentMaterials.isNotEmpty()) {
            SectionHeader(
                title = "Recent Materials",
                action = "See All",
                onAction = onNavigateToLibrary
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(100.dp)
            ) {
                items(recentMaterials, key = { it.id }) { material ->
                    RecentMaterialCard(
                        material = material,
                        onClick = { onNavigateToMaterialDetail(material.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecentMaterialCard(
    material: MaterialEntity,
    onClick: () -> Unit
) {
    val typeIcon = when (material.sourceType.lowercase()) {
        "pdf" -> Icons.Filled.PictureAsPdf
        "image" -> Icons.Filled.Image
        else -> Icons.Filled.Description
    }

    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = EduPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = material.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
