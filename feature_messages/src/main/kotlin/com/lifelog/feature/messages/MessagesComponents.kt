package com.lifelog.feature.messages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifelog.domain.model.MessageChannel
import com.lifelog.domain.model.MessagesOverviewStats
import com.lifelog.domain.model.UniversalChatMessage
import com.lifelog.domain.model.UniversalConversation
import com.lifelog.ui.components.AppIcon
import com.lifelog.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesTopAppBar(
    title: String,
    subtitle: String? = null,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            actions()
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            ),
    )
}

@Composable
fun MessagesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "Search by sender, message, or app...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessagesFilterChips(
    selected: MessageChannel,
    onSelected: (MessageChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MessageChannel.entries.forEach { channel ->
            FilterChip(
                selected = selected == channel,
                onClick = { onSelected(channel) },
                label = { Text(channel.displayName) },
                leadingIcon =
                    if (selected == channel) {
                        {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        null
                    },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = channelColor(channel).copy(alpha = 0.22f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        }
    }
}

@Composable
fun MessagesStatsHeader(
    stats: MessagesOverviewStats,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val gradient =
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
            ),
        )

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .background(gradient)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Messaging Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AnimatedStat(label = "Conversations", target = stats.totalConversations, animate = animate)
                AnimatedStat(label = "Messages", target = stats.totalMessages, animate = animate)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AnimatedStat(label = "Telegram", target = stats.telegramCount, animate = animate)
                AnimatedStat(label = "WhatsApp", target = stats.whatsappCount, animate = animate)
                AnimatedStat(label = "Instagram", target = stats.instagramCount, animate = animate)
                AnimatedStat(label = "SMS", target = stats.smsCount, animate = animate)
            }
        }
    }
}

@Composable
private fun AnimatedStat(
    label: String,
    target: Int,
    animate: Boolean,
) {
    val animated = remember { Animatable(if (animate) 0f else target.toFloat()) }
    LaunchedEffect(target, animate) {
        if (animate) {
            animated.animateTo(
                targetValue = target.toFloat(),
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            )
        } else {
            animated.snapTo(target.toFloat())
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = animated.value.toInt().toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ConversationRow(
    conversation: UniversalConversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarColor = avatarColorFor(conversation.displayName)
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 4 },
    ) {
        Surface(
            onClick = onClick,
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Box(
                        modifier =
                            Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        conversation.packageName?.let { packageName ->
                            AppIcon(
                                packageName = packageName,
                                size = 54.dp,
                                modifier = Modifier.clip(CircleShape),
                            )
                        } ?: run {
                            Text(
                                text = conversation.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                    ChannelBadge(
                        channel = conversation.channel,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = conversation.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = DateTimeUtils.formatRelativeTime(conversation.lastTimestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (conversation.isLastOutgoing) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = conversation.lastMessage.ifBlank { "No preview" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (conversation.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier =
                                    Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (conversation.unreadCount > 9) "9+" else conversation.unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelBadge(
    channel: MessageChannel,
    modifier: Modifier = Modifier,
) {
    if (channel == MessageChannel.ALL) return
    Box(
        modifier =
            modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(channelColor(channel)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = channel.displayName.take(1),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ChatDateSeparator(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(label) },
            colors =
                AssistChipDefaults.assistChipColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
    }
}

@Composable
fun UniversalChatBubble(
    message: UniversalChatMessage,
    showSender: Boolean,
    modifier: Modifier = Modifier,
) {
    val outgoing = message.isOutgoing
    val alignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor =
        if (outgoing) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        }
    val contentColor =
        if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    AnimatedContent(
        targetState = message.id,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        modifier = modifier.fillMaxWidth(),
        label = "chatBubble",
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
            Column(
                horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start,
                modifier = Modifier.fillMaxWidth(0.84f),
            ) {
                if (showSender && !outgoing) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                }
                Surface(
                    shape =
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (outgoing) 18.dp else 6.dp,
                            bottomEnd = if (outgoing) 6.dp else 18.dp,
                        ),
                    color = bubbleColor,
                    shadowElevation = 1.dp,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = message.text.ifBlank { "(empty message)" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChannelMiniBadge(channel = message.channel)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = DateTimeUtils.formatTime(message.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelMiniBadge(channel: MessageChannel) {
    Text(
        text = channel.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = channelColor(channel),
        fontWeight = FontWeight.SemiBold,
    )
}

fun channelColor(channel: MessageChannel): Color =
    when (channel) {
        MessageChannel.TELEGRAM -> Color(0xFF229ED9)
        MessageChannel.WHATSAPP -> Color(0xFF25D366)
        MessageChannel.INSTAGRAM -> Color(0xFFE4405F)
        MessageChannel.SMS -> Color(0xFF5C6BC0)
        MessageChannel.ALL -> Color(0xFF607D8B)
    }

fun channelIcon(channel: MessageChannel): ImageVector =
    when (channel) {
        MessageChannel.SMS -> Icons.Filled.Chat
        MessageChannel.INSTAGRAM -> Icons.Filled.CameraAlt
        else -> Icons.Filled.Chat
    }

private fun avatarColorFor(name: String): Color {
    val palette =
        listOf(
            Color(0xFF5C6BC0),
            Color(0xFF26A69A),
            Color(0xFFEF5350),
            Color(0xFFAB47BC),
            Color(0xFF42A5F5),
            Color(0xFFFF7043),
            Color(0xFF66BB6A),
        )
    val index = name.hashCode().mod(palette.size).let { if (it < 0) it + palette.size else it }
    return palette[index]
}
