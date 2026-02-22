package io.foxbird.edumate.feature.materials

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import io.foxbird.edumate.data.local.entity.MaterialEntity
import io.foxbird.edumate.ui.components.IconContainer
import io.foxbird.edumate.ui.components.ProcessingCard
import io.foxbird.edumate.ui.components.SectionHeader
import io.foxbird.edumate.ui.theme.EduPurple
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
    viewModel: MaterialsViewModel = koinViewModel(),
    onMaterialClick: (Long) -> Unit = {}
) {
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingPdfUri by remember { mutableStateOf<Uri?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingPdfUri = it
            val title = uri.lastPathSegment
                ?.substringAfterLast("/")
                ?.substringBeforeLast(".")
                ?: "Untitled PDF"
            viewModel.addPdf(it, title)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                AddMaterialBanner(onClick = { viewModel.showAddSheet() })
            }

            uiState.processingStage?.let { stage ->
                item {
                    val progress = uiState.processingProgress?.let { (current, total) ->
                        if (total > 0) current.toFloat() / total else 0f
                    } ?: 0f
                    ProcessingCard(
                        materialName = uiState.processingMaterialName ?: "Material",
                        progress = progress,
                        currentStep = deriveCurrentStep(stage),
                        statusText = stage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            if (materials.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Your Materials",
                        action = "${materials.size} items",
                        onAction = {}
                    )
                }

                items(materials, key = { it.id }) { material ->
                    MaterialListCard(
                        material = material,
                        onClick = { onMaterialClick(material.id) },
                        onDelete = { viewModel.deleteMaterial(material.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else if (uiState.processingStage == null) {
                item {
                    EmptyMaterialsState(
                        modifier = Modifier
                            .fillParentMaxHeight(0.6f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    if (uiState.showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideAddSheet() },
            sheetState = rememberModalBottomSheetState()
        ) {
            AddMaterialSheet(
                onPdfClick = {
                    viewModel.hideAddSheet()
                    pdfLauncher.launch(arrayOf("application/pdf"))
                },
                onTextClick = {
                    viewModel.hideAddSheet()
                    viewModel.addText("Sample study material text for testing.", "Sample Text")
                }
            )
        }
    }
}

private fun deriveCurrentStep(stage: String): String {
    val lower = stage.lowercase()
    return when {
        "embed" in lower -> "Embed"
        "chunk" in lower -> "Chunk"
        "extract" in lower || "read" in lower || "pars" in lower -> "Extract"
        "complete" in lower || "done" in lower || "finish" in lower -> "Done"
        else -> "Extract"
    }
}

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun AddMaterialBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconContainer(
                icon = Icons.Filled.Add,
                containerColor = EduPurple.copy(alpha = 0.15f),
                iconColor = EduPurple
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Add New Material",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "PDF, Image, or Camera",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MaterialListCard(
    material: MaterialEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, iconColor) = materialTypeIcon(material.sourceType)
            IconContainer(
                icon = icon,
                containerColor = iconColor.copy(alpha = 0.15f),
                iconColor = iconColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    material.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (material.status == "completed") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Ready to help",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        val dateStr = formatDate(material.processedAt)
                        if (dateStr.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Processing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
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
    }
}

private fun materialTypeIcon(sourceType: String): Pair<ImageVector, Color> = when (sourceType) {
    "pdf" -> Icons.Filled.PictureAsPdf to Color(0xFFEF5350)
    "image" -> Icons.Filled.Image to Color(0xFF42A5F5)
    else -> Icons.Filled.Description to Color(0xFF9E9E9E)
}

@Composable
private fun EmptyMaterialsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No materials yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Add your study materials to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AddMaterialSheet(
    onPdfClick: () -> Unit,
    onTextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Add Material", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        SheetOption(
            icon = Icons.Filled.PictureAsPdf,
            title = "PDF Document",
            subtitle = "Import a PDF file",
            onClick = onPdfClick
        )
        SheetOption(
            icon = Icons.Filled.TextFields,
            title = "Text",
            subtitle = "Enter or paste text",
            onClick = onTextClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SheetOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
