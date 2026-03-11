package io.foxbird.edumate.feature.chat

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.doclibrary.domain.rag.SearchResult
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val prompt: String
)

private val ragQuickActions = listOf(
    QuickAction("Explain", Icons.Filled.Lightbulb, "Explain this topic"),
    QuickAction("Summarize", Icons.Filled.Description, "Summarize the key points"),
    QuickAction("Quiz me", Icons.Filled.School, "Quiz me on this material"),
    QuickAction("Examples", Icons.Filled.Code, "Give me examples")
)

private val agentQuickActions = listOf(
    QuickAction("Help me study", Icons.Filled.Psychology, "Help me study this topic thoroughly"),
    QuickAction("Prep for exam", Icons.Filled.School, "Prepare me for an exam on this material"),
    QuickAction("Deep dive", Icons.Filled.Lightbulb, "Do a deep dive on this topic with examples and a quiz"),
    QuickAction("Quiz + explain", Icons.Filled.ModelTraining, "Quiz me and explain any wrong answers")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onBack: () -> Unit,
    onNewConversation: () -> Unit = {},
    onOpenModelManager: () -> Unit = {},
    viewModel: ChatViewModel = koinViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.initConversation(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (uiState.isAgentMode) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = uiState.isAgentMode,
                            onCheckedChange = { viewModel.toggleAgentMode() },
                            modifier = Modifier.scale(0.75f)
                        )
                    }
                    IconButton(onClick = { /* TODO: open filter sheet */ }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Conversation") },
                                onClick = {
                                    showMenu = false
                                    onNewConversation()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Chat") },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearChat()
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.imePadding()) {
                QuickActionChips(
                    isAgentMode = uiState.isAgentMode,
                    onChipClick = { prompt -> inputText = prompt }
                )
                InputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    isAgentMode = uiState.isAgentMode,
                    isGenerating = uiState.isGenerating,
                    onSend = {
                        if (inputText.isNotBlank() && !uiState.isGenerating) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onStop = { viewModel.cancelGeneration() }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Agent mode active banner
            AnimatedVisibility(
                visible = uiState.isAgentMode,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                AgentModeBanner(
                    isModelReady = uiState.isModelReady,
                    onOpenModelManager = onOpenModelManager
                )
            }

            // Agent mode disabled notification banner
            AnimatedVisibility(
                visible = uiState.agentModeOffBanner,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                AgentModeOffBanner()
            }

            if (messages.isEmpty() && !uiState.isGenerating) {
                EmptyChatState(
                    isAgentMode = uiState.isAgentMode,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            onRegenerate = { viewModel.regenerateLastResponse() }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Agent Mode Banners
// -------------------------------------------------------------------------

@Composable
private fun AgentModeBanner(
    isModelReady: Boolean,
    onOpenModelManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isModelReady)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)

    val contentColor = if (isModelReady)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onErrorContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .then(if (!isModelReady) Modifier.clickable { onOpenModelManager() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isModelReady) Icons.Filled.Psychology else Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isModelReady) "Agent mode" else "Agent mode — no model loaded",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = if (isModelReady)
                    "EduMate will plan and execute multi-step study tasks"
                else
                    "Load a model in Model Manager to use agent mode",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun AgentModeOffBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Agent mode disabled",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Switched back to RAG mode — answers sourced from your documents",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}

// -------------------------------------------------------------------------
// Empty state
// -------------------------------------------------------------------------

@Composable
private fun EmptyChatState(isAgentMode: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Icon(
                    if (isAgentMode) Icons.Filled.Psychology else Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isAgentMode) "What's your study goal?" else "Start a conversation",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isAgentMode)
                    "Describe what you want to learn — EduMate will plan it out"
                else
                    "Ask questions about your study materials",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------------------
// Quick action chips
// -------------------------------------------------------------------------

@Composable
private fun QuickActionChips(isAgentMode: Boolean, onChipClick: (String) -> Unit) {
    val actions = if (isAgentMode) agentQuickActions else ragQuickActions
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        items(actions) { action ->
            AssistChip(
                onClick = { onChipClick(action.prompt) },
                label = { Text(action.label) },
                leadingIcon = {
                    Icon(
                        action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

// -------------------------------------------------------------------------
// Input bar
// -------------------------------------------------------------------------

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isAgentMode: Boolean,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        IconButton(onClick = { /* TODO: attachment picker */ }) {
            Icon(
                Icons.Filled.AttachFile,
                contentDescription = "Attach file",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = if (isAgentMode) "Describe your study goal…" else "Ask a question...",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isAgentMode) Icons.Filled.Psychology else Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = { if (isGenerating) onStop() else onSend() },
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isGenerating) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

// -------------------------------------------------------------------------
// Message bubble
// -------------------------------------------------------------------------

@Composable
private fun MessageBubble(message: ChatMessage, onRegenerate: () -> Unit) {
    val isUser = message.role == "user"
    val dateFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val onCopy = { clipboardManager.setText(AnnotatedString(message.content)) }
    val onShare = {
        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message.content)
            },
            null
        )
        context.startActivity(intent)
        Unit
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            // Assistant header row: label on left, actions on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (message.agentSteps.isNotEmpty()) Icons.Filled.Psychology else Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (message.agentSteps.isNotEmpty()) "EduMate Agent" else "EduMate",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                // Confidence warning icon in header
                val confidence = message.confidenceScore
                if (confidence != null && confidence < 0.65f) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Low confidence",
                        modifier = Modifier.size(13.dp),
                        tint = if (confidence < 0.50f)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Action buttons — only after streaming ends
                if (!message.isStreaming && message.content.isNotEmpty()) {
                    BubbleActionRow(
                        onCopy = onCopy,
                        onShare = onShare,
                        onRegenerate = onRegenerate
                    )
                }
            }
        } else {
            // User bubble: action icons right-aligned above the bubble
            if (!message.isStreaming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    BubbleActionRow(
                        onCopy = onCopy,
                        onShare = onShare,
                        onRegenerate = null
                    )
                }
            }
        }

        // Confidence banner (assistant, non-agent, before bubble)
        val confidence = message.confidenceScore
        if (!isUser && confidence != null && !message.isStreaming) {
            ConfidenceBanner(score = confidence)
        }

        // Thinking card — for models with think tags (e.g. Qwen3.5)
        if (!isUser && message.thinkingContent != null) {
            ThinkingCard(
                content = message.thinkingContent,
                isThinking = message.isThinking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        // Agent steps card
        if (message.agentSteps.isNotEmpty()) {
            AgentStepsCard(
                steps = message.agentSteps,
                isStreaming = message.isStreaming,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        val displayContent = when {
            message.isStreaming && message.content.isEmpty() -> ""
            else -> message.content
        }

        // Pulsing dots while waiting for first token
        if (message.isStreaming && message.content.isEmpty() && message.agentSteps.isEmpty()) {
            ThinkingDots()
        }

        if (displayContent.isNotEmpty() || (!message.isStreaming)) {
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            )
                        )
                        .background(
                            if (isUser) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = displayContent + if (message.isStreaming && displayContent.isNotEmpty()) " ▍" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sources panel (RAG assistant, after streaming)
        if (!isUser && message.sourceChunks.isNotEmpty() && !message.isStreaming) {
            SourcesPanel(sources = message.sourceChunks)
        }

        // Footer: timestamp / tool count / source count
        if (isUser) {
            Text(
                text = dateFormat.format(Date()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else if (message.agentSteps.isNotEmpty() && !message.isStreaming) {
            Text(
                text = "${message.agentSteps.count { it.type == AgentStepType.TOOL_CALL }} tool(s) used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// -------------------------------------------------------------------------
// Bubble action row (copy / share / regenerate)
// -------------------------------------------------------------------------

@Composable
private fun BubbleActionRow(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: (() -> Unit)?
) {
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onShare,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Filled.Share,
                contentDescription = "Share",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onRegenerate != null) {
            IconButton(
                onClick = onRegenerate,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Regenerate",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Confidence banner
// -------------------------------------------------------------------------

@Composable
private fun ConfidenceBanner(score: Float, modifier: Modifier = Modifier) {
    val isLow = score < 0.50f
    val isMedium = score in 0.50f..0.64f
    if (!isLow && !isMedium) return

    val color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val containerColor = if (isLow)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    else
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
    val message = if (isLow)
        "Low confidence — answer may not be fully accurate"
    else
        "Moderate confidence — verify with your materials"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = color
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

// -------------------------------------------------------------------------
// Sources panel
// -------------------------------------------------------------------------

@Composable
private fun SourcesPanel(sources: List<SearchResult>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Collapsed pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "${sources.size} source${if (sources.size > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sources.forEach { result ->
                    SourceCard(result)
                }
            }
        }
    }
}

@Composable
private fun SourceCard(result: SearchResult, modifier: Modifier = Modifier) {
    val chunk = result.chunk
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: document title + badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = result.documentTitle ?: "Document #${chunk.documentId}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Badges row: chunk type + page number
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourceBadge(text = chunk.chunkType)
                if (chunk.pageNumber != null) {
                    SourceBadge(text = "p. ${chunk.pageNumber}")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Content preview
            Text(
                text = chunk.content.take(150).let { if (chunk.content.length > 150) "$it…" else it },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight
            )
        }
    }
}

@Composable
private fun SourceBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// -------------------------------------------------------------------------
// Thinking card (chain-of-thought from models with think tags)
// -------------------------------------------------------------------------

@Composable
private fun ThinkingCard(
    content: String,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    // Expand while the model is actively thinking; collapse once done.
    var expanded by remember { mutableStateOf(isThinking) }

    LaunchedEffect(isThinking) {
        if (isThinking) expanded = true
        else expanded = false          // auto-collapse when thinking finishes
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header row — always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Icon(
                        Icons.Filled.BlurOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isThinking) "Thinking…" else "Reasoning",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isThinking) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isThinking) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            // Expandable content — scrollable, max 200dp
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SelectionContainer {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Agent steps card
// -------------------------------------------------------------------------

@Composable
private fun AgentStepsCard(
    steps: List<AgentStepDisplay>,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(isStreaming) }

    LaunchedEffect(isStreaming) {
        if (isStreaming) expanded = true
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isStreaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Icon(
                        Icons.Filled.Build,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isStreaming) "Working…" else "${steps.size} step(s)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isStreaming) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isStreaming) MaterialTheme.colorScheme.tertiary
                           else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    steps.forEach { step -> AgentStepRow(step) }
                }
            }
        }
    }
}

@Composable
private fun ThinkingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val dotAlphas = (0..2).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = i * 200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$i"
        )
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            dotAlphas.forEach { alpha ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alpha.value)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun AgentStepRow(step: AgentStepDisplay) {
    val icon = when (step.type) {
        AgentStepType.TOOL_CALL -> Icons.Filled.Build
        AgentStepType.TOOL_RESULT -> Icons.Filled.Description
        AgentStepType.THINKING -> Icons.Filled.Lightbulb
    }
    val color = when (step.type) {
        AgentStepType.TOOL_CALL -> MaterialTheme.colorScheme.primary
        AgentStepType.TOOL_RESULT -> MaterialTheme.colorScheme.tertiary
        AgentStepType.THINKING -> MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = color)
        Spacer(modifier = Modifier.width(4.dp))
        SelectionContainer {
            Column {
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                if (step.detail.isNotBlank()) {
                    Text(
                        text = step.detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
