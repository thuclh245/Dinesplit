package com.example.dinesplit.presentation.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.NotificationType

@Composable
fun NotificationScreen(
    onNotificationClick: (Notification) -> Unit = {}
) {
    val viewModel: NotificationViewModel = viewModel()
    val notifications = viewModel.notifications.collectAsState().value
    val uiState = viewModel.uiState.collectAsState().value

    AppScaffold(title = "Notifications") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            if (uiState.isLoading) {
                LoadingBlock(message = "Loading notifications...")
            } else {
                uiState.errorMessage?.let { message ->
                    ErrorStateBlock(
                        title = "Cannot load notifications",
                        subtitle = message,
                        onRetryClick = { viewModel.refreshNotifications() }
                    )
                }

                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "Activity feed", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${uiState.unreadCount} unread · ${notifications.size} total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (notifications.isEmpty()) {
                    EmptyStateBlock(
                        title = "No notifications yet",
                        subtitle = "You'll see payment updates, split changes, and activity here."
                    )
                } else {
                    notifications.forEach { notification ->
                        NotificationItemCard(
                            notification = notification,
                            onMarkAsRead = {
                                if (!notification.isRead) {
                                    viewModel.markAsRead(notification.id)
                                }
                            },
                            onClick = {
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
private fun NotificationItemCard(
    notification: com.example.dinesplit.domain.model.Notification,
    onMarkAsRead: () -> Unit,
    onClick: () -> Unit = {}
) {
    val icon = when (notification.type) {
        NotificationType.PAYMENT_COMPLETED -> Icons.Filled.CheckCircle
        NotificationType.PAYMENT_PENDING -> Icons.Filled.WarningAmber
        NotificationType.BILL_CREATED -> Icons.Filled.Payments
        NotificationType.SPLIT_COMPLETED -> Icons.Filled.CheckCircle
        NotificationType.TRANSACTION_ALERT -> Icons.Filled.WarningAmber
        NotificationType.ACTIVITY_UPDATE -> Icons.Filled.NotificationsActive
        NotificationType.OTHER -> Icons.Filled.NotificationsActive
    }

    AppCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (notification.isRead) {
                    MaterialTheme.colorScheme.outlineVariant
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(0.82f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (notification.isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = notification.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatTimeAgo(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    if (!notification.isRead) {
        onMarkAsRead()
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
