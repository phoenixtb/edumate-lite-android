package io.foxbird.edumate.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.foxbird.edumate.data.local.entity.ConversationEntity
import io.foxbird.edumate.domain.service.ConversationManager
import io.foxbird.edumate.ui.components.IconContainer
import io.foxbird.edumate.ui.theme.appColors
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
    onChatClick: (Long) -> Unit = {},
    onNewChat: () -> Unit = {}
) {
    val conversationManager: ConversationManager = koinInject()
    val conversations by conversationManager.getAllConversations()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Chat History") },
                actions = {
                    IconButton(onClick = onNewChat) {
                        Icon(Icons.Filled.Add, contentDescription = "New chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            EmptyHistoryState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    ConversationCard(
                        conversation = conv,
                        onClick = { onChatClick(conv.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Outer ambient glow
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(scheme.secondary.copy(alpha = 0.15f), androidx.compose.ui.graphics.Color.Transparent)))
                )
                // Inner icon container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(scheme.secondaryContainer.copy(alpha = 0.28f))
                        .border(1.dp, scheme.secondary.copy(alpha = 0.22f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = scheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "No conversations yet",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Start chatting with your study materials",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())

    val scheme = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    val accentColor = scheme.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, appColors.glassBorderDefault, RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.3f)),
                        startY = 0f, endY = size.height,
                    ),
                    size = Size(3.5.dp.toPx(), size.height),
                )
            }
    ) {
        // Top glow tinted by secondary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.07f), androidx.compose.ui.graphics.Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconContainer(
                icon = Icons.AutoMirrored.Filled.Chat,
                containerColor = scheme.secondaryContainer.copy(alpha = 0.6f),
                iconColor = scheme.onSecondaryContainer,
                size = 44.dp,
                iconSize = 22.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    conversation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    "${conversation.messageCount} messages · ${dateFormat.format(Date(conversation.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
