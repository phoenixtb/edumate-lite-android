package io.foxbird.edumate.feature.materials

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.doclibrary.data.local.entity.DocumentEntity
import io.foxbird.doclibrary.domain.processor.ProcessingMode
import io.foxbird.doclibrary.viewmodel.AddFlow
import io.foxbird.doclibrary.viewmodel.LibraryViewModel
import io.foxbird.doclibrary.viewmodel.SourceType
import io.foxbird.edumate.ui.components.IconContainer
import io.foxbird.edumate.ui.components.ProcessingCard
import io.foxbird.edumate.ui.components.SectionHeader
import io.foxbird.edumate.ui.components.deriveProcessingStep
import io.foxbird.edumate.ui.theme.EduAmber
import io.foxbird.edumate.ui.theme.EduPurple
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
    viewModel: LibraryViewModel = koinViewModel(),
    onMaterialClick: (Long) -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showAddSheet by viewModel.showAddSheet.collectAsStateWithLifecycle()
    val processingState by viewModel.processingState.collectAsStateWithLifecycle()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val name = context.resolveDisplayName(it)
            viewModel.onSourcePicked(SourceType.PDF, uri = it, suggestedTitle = name)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.onSourcePicked(SourceType.GALLERY, uris = uris)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) tempCameraUri?.let { viewModel.onSourcePicked(SourceType.CAMERA, uri = it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                AddMaterialBanner(onClick = { viewModel.openAddSheet() })
            }

            processingState?.let { ps ->
                item {
                    ProcessingCard(
                        materialName = ps.documentName,
                        progress = ps.progress,
                        currentStep = deriveProcessingStep(ps.stage),
                        statusText = ps.stage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            if (documents.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Your Materials",
                        action = "${documents.size} items",
                        onAction = {}
                    )
                }
                items(documents, key = { it.id }) { document ->
                    DocumentListCard(
                        document = document,
                        onClick = { onMaterialClick(document.id) },
                        onDelete = { viewModel.deleteDocument(document.id) },
                        onEdit = { editDoc ->
                            // Handled inline via EditDocumentDialog
                        },
                        viewModel = viewModel,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else if (processingState == null) {
                item {
                    EmptyMaterialsState(
                        modifier = Modifier.fillParentMaxHeight(0.6f).fillMaxWidth()
                    )
                }
            }
        }
    }

    // ---------- Add sheet ----------
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeAddSheet() },
            sheetState = rememberModalBottomSheetState()
        ) {
            AddMaterialSheet(
                onPdfClick = {
                    viewModel.closeAddSheet()
                    pdfLauncher.launch(arrayOf("application/pdf"))
                },
                onGalleryClick = {
                    viewModel.closeAddSheet()
                    galleryLauncher.launch("image/*")
                },
                onCameraClick = {
                    viewModel.closeAddSheet()
                    val imageDir = context.cacheDir.resolve("images").also { it.mkdirs() }
                    val imageFile = java.io.File.createTempFile("camera_", ".jpg", imageDir)
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", imageFile
                    )
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                onDismiss = { viewModel.closeAddSheet() }
            )
        }
    }

    // ---------- Details dialog (after source picked) ----------
    val addFlow = uiState.addFlow
    if (addFlow is AddFlow.SourcePicked) {
        DocumentDetailsDialog(
            suggestedTitle = addFlow.suggestedTitle,
            onConfirm = { title, subject, grade ->
                viewModel.confirmDetails(title, subject, grade)
            },
            onCancel = { viewModel.cancelAddFlow() }
        )
    }

    // ---------- Processing mode dialog ----------
    if (addFlow is AddFlow.Details) {
        ProcessingModeDialog(
            onSelect = { mode -> viewModel.confirmProcessing(mode) },
            onCancel = { viewModel.cancelAddFlow() }
        )
    }
}

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return ""
    return SimpleDateFormat("M/d", Locale.getDefault()).format(Date(timestamp))
}

// ---------- Banner ----------

@Composable
private fun AddMaterialBanner(onClick: () -> Unit) {
    val gradient = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(Color(0xFF1A0E50), Color(0xFF2D1F90))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(gradient)
            .clickable(onClick = onClick)
    ) {
        // Subtle glow overlay at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(EduPurple.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon with glow halo
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF3D2AB5))
                ) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Add New Material", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("PDF, Image, or Camera", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.7f))
        }
    }
}

// ---------- Document card (Track 2: subject/grade chips, M/d date, icons) ----------

@Composable
private fun DocumentListCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (DocumentEntity) -> Unit,
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val (icon, iconColor) = documentTypeIcon(document.sourceType)
            IconContainer(icon = icon, containerColor = iconColor.copy(alpha = 0.25f), iconColor = iconColor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(document.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))

                // Subject / grade chips
                if (document.subject != null || document.gradeLevel != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        document.subject?.let { subj ->
                            SuggestionChip(onClick = {}, label = { Text(subj, style = MaterialTheme.typography.labelSmall) })
                        }
                        document.gradeLevel?.let { grade ->
                            SuggestionChip(onClick = {}, label = { Text("Grade $grade", style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (document.status == "completed") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lightbulb, null, Modifier.size(14.dp), tint = Color(0xFFFFB300))
                        Spacer(Modifier.width(4.dp))
                        Text("Ready to help", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                        val dateStr = formatDate(document.processedAt)
                        if (dateStr.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.HourglassEmpty, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("Processing…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showMenu = false; showEditDialog = true },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditDocumentDialog(
            document = document,
            onSave = { title, subject, grade ->
                viewModel.updateDocument(document.id, title, subject, grade)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

private fun documentTypeIcon(sourceType: String): Pair<ImageVector, Color> = when (sourceType) {
    "pdf" -> Icons.Filled.PictureAsPdf to Color(0xFFEF5350)
    "image" -> Icons.Filled.Image to Color(0xFF42A5F5)
    else -> Icons.Filled.FolderOpen to Color(0xFF9E9E9E)
}

@Composable
private fun EmptyMaterialsState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.FolderOpen, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text("No materials yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("Add your study materials to get started", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

// ---------- Add Material Sheet ----------

@Composable
private fun AddMaterialSheet(
    onPdfClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    Icons.Filled.Add,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Add Study Material",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Choose a source to upload",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        SheetOption(
            icon = Icons.Filled.PictureAsPdf,
            iconColor = Color(0xFFEF5350),
            containerColor = Color(0xFFEF5350).copy(alpha = 0.15f),
            title = "PDF Document",
            subtitle = "Upload textbooks, notes, or worksheets",
            onClick = onPdfClick
        )
        SheetOption(
            icon = Icons.Filled.Image,
            iconColor = Color(0xFF42A5F5),
            containerColor = Color(0xFF42A5F5).copy(alpha = 0.15f),
            title = "From Gallery",
            subtitle = "Select images of study materials",
            onClick = onGalleryClick
        )
        SheetOption(
            icon = Icons.Filled.CameraAlt,
            iconColor = EduAmber,
            containerColor = EduAmber.copy(alpha = 0.15f),
            title = "Take Photo",
            subtitle = "Capture pages with your camera",
            onClick = onCameraClick
        )

        Spacer(Modifier.height(8.dp))
        TextButton(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) {
            Text("Cancel")
        }
    }
}

@Composable
private fun SheetOption(
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(containerColor)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------- Document Details Dialog ----------

private val SUBJECTS = listOf("None", "Math", "Science", "English", "History", "Art", "PE", "Other")
private val GRADES = listOf("None") + (1..12).map { "Grade $it" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsDialog(
    suggestedTitle: String,
    onConfirm: (title: String, subject: String?, gradeLevel: Int?) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(suggestedTitle) }
    var selectedSubject by remember { mutableStateOf("None") }
    var selectedGrade by remember { mutableStateOf("None") }
    var subjectExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Document Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(expanded = subjectExpanded, onExpandedChange = { subjectExpanded = it }) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subjectExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                        SUBJECTS.forEach { subj ->
                            DropdownMenuItem(text = { Text(subj) }, onClick = { selectedSubject = subj; subjectExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = gradeExpanded, onExpandedChange = { gradeExpanded = it }) {
                    OutlinedTextField(
                        value = selectedGrade,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grade") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gradeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = gradeExpanded, onDismissRequest = { gradeExpanded = false }) {
                        GRADES.forEach { grade ->
                            DropdownMenuItem(text = { Text(grade) }, onClick = { selectedGrade = grade; gradeExpanded = false })
                        }
                    }
                }

                AssistChip(
                    onClick = {},
                    label = { Text("You'll choose processing mode next", style = MaterialTheme.typography.labelSmall) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val subject = if (selectedSubject == "None") null else selectedSubject
                    val grade = if (selectedGrade == "None") null else selectedGrade.removePrefix("Grade ").toIntOrNull()
                    onConfirm(title.trim().ifBlank { "Untitled" }, subject, grade)
                },
                enabled = title.isNotBlank()
            ) { Text("Next") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

// ---------- Processing Mode Dialog ----------

@Composable
fun ProcessingModeDialog(
    onSelect: (ProcessingMode) -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Column {
                Text("Processing Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("How should we analyse this document?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProcessingModeOption(
                    icon = Icons.Outlined.Speed,
                    iconColor = Color(0xFF00BCD4),
                    containerColor = Color(0xFF00BCD4).copy(alpha = 0.12f),
                    title = "Fast",
                    badge = "Recommended",
                    badgeColor = Color(0xFF00BCD4),
                    description = "Direct text extraction — ideal for digital PDFs and clean typed notes.",
                    onClick = { onSelect(ProcessingMode.FAST) }
                )
                ProcessingModeOption(
                    icon = Icons.Outlined.AutoAwesome,
                    iconColor = Color(0xFF7C4DFF),
                    containerColor = Color(0xFF7C4DFF).copy(alpha = 0.12f),
                    title = "Thorough  ·  AI Vision",
                    badge = "Slower",
                    badgeColor = Color(0xFFFF7043),
                    description = "AI reads each page image — best for scanned docs, handwriting, equations, diagrams.",
                    onClick = { onSelect(ProcessingMode.THOROUGH) }
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

@Composable
private fun ProcessingModeOption(
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------- Edit Document Dialog ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDocumentDialog(
    document: DocumentEntity,
    onSave: (title: String, subject: String?, gradeLevel: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(document.title) }
    var selectedSubject by remember { mutableStateOf(document.subject ?: "None") }
    var selectedGrade by remember { mutableStateOf(document.gradeLevel?.let { "Grade $it" } ?: "None") }
    var subjectExpanded by remember { mutableStateOf(false) }
    var gradeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Document") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(expanded = subjectExpanded, onExpandedChange = { subjectExpanded = it }) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subjectExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                        SUBJECTS.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = { selectedSubject = s; subjectExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = gradeExpanded, onExpandedChange = { gradeExpanded = it }) {
                    OutlinedTextField(
                        value = selectedGrade,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grade") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gradeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = gradeExpanded, onDismissRequest = { gradeExpanded = false }) {
                        GRADES.forEach { g ->
                            DropdownMenuItem(text = { Text(g) }, onClick = { selectedGrade = g; gradeExpanded = false })
                        }
                    }
                }

                // Read-only info
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow("Source", document.sourceType.replaceFirstChar { it.uppercase() })
                        InfoRow("Chunks", document.chunkCount.toString())
                        InfoRow("Status", document.status.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val subject = if (selectedSubject == "None") null else selectedSubject
                    val grade = if (selectedGrade == "None") null else selectedGrade.removePrefix("Grade ").toIntOrNull()
                    onSave(title.trim().ifBlank { document.title }, subject, grade)
                },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Context.resolveDisplayName(uri: Uri): String {
    val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        else null
    }?.substringBeforeLast(".") ?: uri.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".") ?: "Untitled"
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
