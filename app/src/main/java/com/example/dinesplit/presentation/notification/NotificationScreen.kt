package com.example.dinesplit.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType

@Composable
fun NotificationScreen(
    onNotificationClick: (Notification) -> Unit = {}
) {
    val viewModel: NotificationViewModel = viewModel()
    val notifications = viewModel.notifications.collectAsState().value
    val uiState = viewModel.uiState.collectAsState().value

    AppScaffold(title = "Notifications") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppDimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            if (uiState.isLoading) {
                item {
                    LoadingBlock(message = "Loading notifications...")
                }
            } else {
                uiState.errorMessage?.let { message ->
                    item {
                        ErrorStateBlock(
                            title = "Cannot load notifications",
                            subtitle = message,
                            onRetryClick = { viewModel.refreshNotifications() }
                        )
                    }
                }

                item {
                    NotificationSummaryCard(
                        unreadCount = uiState.unreadCount,
                        totalCount = notifications.size,
                        onRefresh = viewModel::refreshNotifications
                    )
                }

                if (notifications.isEmpty()) {
                    item {
                        EmptyStateBlock(
                            title = "No notifications yet",
                            subtitle = "Payment updates, split changes, and spending alerts from Firebase will appear here."
                        )
                    }
                } else {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationItemCard(
                            notification = notification,
                            onToggleRead = {
                                if (notification.isRead) {
                                    viewModel.markAsUnread(notification.id)
                                } else {
                                    viewModel.markAsRead(notification.id)
                                }
                            },
                            onClick = {
                                if (!notification.isRead) {
                                    viewModel.markAsRead(notification.id)
                                }
                                onNotificationClick(notification)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSummaryCard(
    unreadCount: Int,
    totalCount: Int,
    onRefresh: () -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
                Text(
                    text = "Activity feed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$unreadCount unread - $totalCount total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: Notification,
    onToggleRead: () -> Unit,
    onClick: () -> Unit = {}
) {
    val icon = notificationIcon(notification.type)
    val iconColor = if (notification.isRead) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.primary
    }

    AppCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        color = if (notification.isRead) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = formatTimeAgo(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = notification.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(if (notification.isRead) "Read" else "Unread")
                        }
                    )
                    TextButton(onClick = onToggleRead) {
                        Text(if (notification.isRead) "Mark unread" else "Mark read")
                    }
                }
            }
        }
    }
}

private fun notificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.PAYMENT_COMPLETED -> Icons.Filled.CheckCircle
        NotificationType.PAYMENT_PENDING -> Icons.Filled.WarningAmber
        NotificationType.BILL_CREATED -> Icons.Filled.Payments
        NotificationType.SPLIT_COMPLETED -> Icons.Filled.CheckCircle
        NotificationType.TRANSACTION_ALERT -> Icons.Filled.WarningAmber
        NotificationType.ACTIVITY_UPDATE -> Icons.Filled.NotificationsActive
        NotificationType.OTHER -> Icons.Filled.NotificationsActive
    }
}

private fun formatTimeAgo(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - epochMillis
    val diffMinutes = diffMillis / (1000 * 60)
    val diffHours = diffMillis / (1000 * 60 * 60)
    val diffDays = diffMillis / (1000 * 60 * 60 * 24)

    return when {
        diffMinutes < 1 -> "now"
        diffMinutes < 60 -> "${diffMinutes}m"
        diffHours < 24 -> "${diffHours}h"
        diffDays < 7 -> "${diffDays}d"
        else -> "${diffDays / 7}w"
    }
}
