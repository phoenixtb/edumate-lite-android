package io.foxbird.edumate.feature.chat

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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edumate.ui.components.IconContainer
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

    // Also scroll when the keyboard opens so the last message stays visible
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    LaunchedEffect(imeBottom) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(), // imePadding on Scaffold — not inside content
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Agent mode toggle: icon + compact Switch
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Agent mode banner — slides in below TopAppBar
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
                        MessageBubble(message)
                    }
                }
            }

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
}

// -------------------------------------------------------------------------
// Agent Mode Banner
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

// -------------------------------------------------------------------------
// Empty state
// -------------------------------------------------------------------------

@Composable
private fun EmptyChatState(isAgentMode: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
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
                Text(if (isAgentMode) "Describe your study goal…" else "Ask a question...")
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isAgentMode) Icons.Filled.Psychology else Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
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
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val dateFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
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
            }
        }

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

        // Pulsing dots while waiting for first token (both RAG and agent mode)
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

        Spacer(modifier = Modifier.height(2.dp))

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
        } else if (message.sourceChunks.isNotEmpty()) {
            Text(
                text = "${message.sourceChunks.size} source(s) used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
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
