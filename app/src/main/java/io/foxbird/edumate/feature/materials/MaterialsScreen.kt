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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.foxbird.edumate.ui.theme.appColors
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
        },
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
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
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF09090F),
            scrimColor = Color.Black.copy(alpha = 0.6f),
            tonalElevation = 0.dp
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
    val scheme = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(
        colors = listOf(scheme.primaryContainer, scheme.primary.copy(alpha = 0.9f))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(gradient)
            .clickable(onClick = onClick)
    ) {
        // Subtle glow overlay at top — uses appColors for consistency
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(scheme.onPrimaryContainer.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(52.dp).background(scheme.onPrimaryContainer.copy(alpha = 0.12f), RoundedCornerShape(16.dp)))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(scheme.primary)
                ) {
                    Icon(Icons.Filled.Add, null, tint = scheme.onPrimary, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Add New Material", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("PDF, Image, or Camera", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
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

    val (icon, iconColor) = documentTypeIcon(document.sourceType)
    val scheme = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    val stripeColor = iconColor
    val isReady = document.status == "completed"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, appColors.glassBorderDefault, RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            // Left type-color stripe drawn behind content
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(stripeColor, stripeColor.copy(alpha = 0.35f)),
                        startY = 0f, endY = size.height,
                    ),
                    size = Size(3.5.dp.toPx(), size.height),
                )
            }
    ) {
        // Subtle top glow tinted by document type
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(iconColor.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier.padding(start = 15.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon with glow halo
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(48.dp).background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp)))
                IconContainer(
                    icon = icon,
                    containerColor = iconColor.copy(alpha = 0.20f),
                    iconColor = iconColor,
                    size = 44.dp,
                    iconSize = 22.dp,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    document.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))

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

                // Status pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isReady) appColors.stepComplete.copy(alpha = 0.14f)
                                else scheme.surfaceContainerHighest
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isReady) Icons.Outlined.Lightbulb else Icons.Outlined.HourglassEmpty,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isReady) appColors.stepComplete else scheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isReady) "Ready to help" else "Processing…",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isReady) appColors.stepComplete else scheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    if (isReady) {
                        val dateStr = formatDate(document.processedAt)
                        if (dateStr.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, "Options", tint = scheme.onSurfaceVariant)
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
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = scheme.error) }
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
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Outer ambient glow
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(scheme.primary.copy(alpha = 0.15f), Color.Transparent)))
                )
                // Inner icon container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(scheme.primaryContainer.copy(alpha = 0.28f))
                        .border(1.dp, scheme.primary.copy(alpha = 0.20f), CircleShape)
                ) {
                    Icon(Icons.Filled.FolderOpen, null, Modifier.size(36.dp), tint = scheme.primary.copy(alpha = 0.75f))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("No materials yet", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Add your study materials to get started", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

// ---------- Add Material Sheet ----------

private val SheetBg = Color(0xFF09090F)
private val SheetSurface = Color(0xFF111118)

@Composable
private fun AddMaterialSheet(
    onPdfClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SheetBg)
            .navigationBarsPadding()
    ) {
        // Top glow accent line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SELECT SOURCE",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Choose where to pull study material from",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }

        // Source options
        AiSheetOption(
            icon = Icons.Filled.PictureAsPdf,
            glowColor = Color(0xFFFF453A),
            label = "PDF Document",
            description = "Textbooks, notes, worksheets",
            onClick = onPdfClick
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 20.dp)
                .background(Color.White.copy(alpha = 0.06f))
        )
        AiSheetOption(
            icon = Icons.Filled.Image,
            glowColor = Color(0xFF30D158),
            label = "Image Gallery",
            description = "Select study material images",
            onClick = onGalleryClick
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 20.dp)
                .background(Color.White.copy(alpha = 0.06f))
        )
        AiSheetOption(
            icon = Icons.Filled.CameraAlt,
            glowColor = Color(0xFFBF5AF2),
            label = "Camera Capture",
            description = "Photograph your materials",
            onClick = onCameraClick
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AiSheetOption(
    icon: ImageVector,
    glowColor: Color,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Layered glow icon
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(glowColor.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(SheetSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, glowColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            ) {
                Icon(icon, null, tint = glowColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.38f)
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }
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
