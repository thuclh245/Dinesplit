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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType

@Composable
fun NotificationScreen(
    onBack: () -> Unit = {},
    onNotificationClick: (Notification) -> Unit = {},
) {
    val viewModel: NotificationViewModel = viewModel()
    val notifications by viewModel.notifications.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by rememberSaveable { mutableStateOf(NotificationFilter.ALL) }
    val filteredNotifications = notifications.filter { it.matches(selectedFilter) }
    val newNotifications = filteredNotifications.filter { !it.isRead }
    val earlierNotifications = filteredNotifications.filter { it.isRead }

    AppScaffold(
        title = "Notifications",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        },
        actions = {
            IconButton(onClick = viewModel::refreshNotifications) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh notifications",
                )
            }
            IconButton(
                onClick = viewModel::markAllAsRead,
                enabled = uiState.unreadCount > 0,
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Mark all notifications as read",
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppDimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
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
                            onRetryClick = { viewModel.refreshNotifications() },
                        )
                    }
                }

                item {
                    NotificationOverviewCard(
                        unreadCount = uiState.unreadCount,
                        totalCount = notifications.size,
                        personalCount = notifications.count { it.isPersonalAlert() },
                        splitCount = notifications.count { it.isSplitAlert() },
                        activityCount = notifications.count { it.type == NotificationType.ACTIVITY_UPDATE },
                    )
                }

                item {
                    NotificationFilterBar(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it },
                        notifications = notifications,
                    )
                }

                if (filteredNotifications.isEmpty()) {
                    item {
                        EmptyStateBlock(
                            title = emptyTitleFor(selectedFilter),
                            subtitle = "Personal alerts, split events, and social updates will appear here when they are generated.",
                        )
                    }
                } else {
                    if (newNotifications.isNotEmpty()) {
                        notificationSection(
                            title = "New",
                            notifications = newNotifications,
                            viewModel = viewModel,
                            onNotificationClick = onNotificationClick,
                        )
                    }

                    if (earlierNotifications.isNotEmpty()) {
                        notificationSection(
                            title = "Earlier",
                            notifications = earlierNotifications,
                            viewModel = viewModel,
                            onNotificationClick = onNotificationClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationOverviewCard(
    unreadCount: Int,
    totalCount: Int,
    personalCount: Int,
    splitCount: Int,
    activityCount: Int,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                ) {
                    Text(
                        text = "Inbox status",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "$unreadCount unread out of $totalCount notifications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                NotificationMetric(
                    modifier = Modifier.weight(1f),
                    label = "Personal",
                    value = personalCount.toString(),
                    color = MaterialTheme.colorScheme.secondary,
                )
                NotificationMetric(
                    modifier = Modifier.weight(1f),
                    label = "Split",
                    value = splitCount.toString(),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                NotificationMetric(
                    modifier = Modifier.weight(1f),
                    label = "Social",
                    value = activityCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                )
                NotificationMetric(
                    modifier = Modifier.weight(1f),
                    label = "Unread",
                    value = unreadCount.toString(),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun NotificationMetric(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color,
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.large,
        color = color.copy(alpha = 0.10f),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color,
            )
        }
    }
}

@Composable
private fun NotificationFilterBar(
    selectedFilter: NotificationFilter,
    onFilterSelected: (NotificationFilter) -> Unit,
    notifications: List<Notification>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        Text(
            text = "Filter",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        ) {
            items(NotificationFilter.entries) { filter ->
                NotificationFilterChip(
                    filter = filter,
                    selected = selectedFilter == filter,
                    count = notifications.count { it.matches(filter) },
                    onClick = { onFilterSelected(filter) },
                )
            }
        }
    }
}

@Composable
private fun NotificationFilterChip(
    filter: NotificationFilter,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("${filter.label} $count") },
    )
}

private fun LazyListScope.notificationSection(
    title: String,
    notifications: List<Notification>,
    viewModel: NotificationViewModel,
    onNotificationClick: (Notification) -> Unit,
) {
    item(key = "${title}_header") {
        NotificationSectionHeader(title = title, count = notifications.size)
    }

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
            },
        )
    }
}

@Composable
private fun NotificationSectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.spaceXs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotificationItemCard(
    notification: Notification,
    onToggleRead: () -> Unit,
    onClick: () -> Unit,
) {
    val isUnread = !notification.isRead
    val icon = notificationIcon(notification.type)
    val accentColor = notificationAccentColor(notification)
    val containerColor =
        if (isUnread) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    val titleColor =
        if (isUnread) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        color = containerColor,
        tonalElevation = if (isUnread) 2.dp else 0.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
        ) {
            if (isUnread) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(4.dp)
                            .height(72.dp)
                            .clip(AppShapes.full)
                            .background(accentColor),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = AppDimens.spaceLg,
                            top = AppDimens.spaceMd,
                            end = AppDimens.spaceMd,
                            bottom = AppDimens.spaceMd,
                        ),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (isUnread) 0.14f else 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = titleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatTimeAgo(notification.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = notification.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NotificationTypePill(
                            label = notificationTypeLabel(notification),
                            isUnread = isUnread,
                            color = accentColor,
                        )
                        IconButton(
                            onClick = onToggleRead,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector =
                                    if (notification.isRead) {
                                        Icons.Default.MarkEmailUnread
                                    } else {
                                        Icons.Default.MarkEmailRead
                                    },
                                contentDescription =
                                    if (notification.isRead) {
                                        "Mark notification as unread"
                                    } else {
                                        "Mark notification as read"
                                    },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationTypePill(
    label: String,
    isUnread: Boolean,
    color: Color,
) {
    Surface(
        shape = AppShapes.full,
        color = color.copy(alpha = 0.10f),
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceSm),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isUnread) {
                Box(
                    modifier =
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(color),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun notificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.PAYMENT_COMPLETED -> Icons.Filled.CheckCircle
        NotificationType.PAYMENT_PENDING -> Icons.Filled.WarningAmber
        NotificationType.BILL_CREATED -> Icons.Filled.Payments
        NotificationType.SPLIT_COMPLETED -> Icons.Filled.CheckCircle
        NotificationType.TRANSACTION_ALERT -> Icons.Filled.Savings
        NotificationType.ACTIVITY_UPDATE -> Icons.Filled.NotificationsActive
        NotificationType.OTHER -> Icons.Filled.NotificationsActive
    }
}

@Composable
private fun notificationAccentColor(notification: Notification): Color {
    if (notification.isRead) return MaterialTheme.colorScheme.outline
    return when {
        notification.isPersonalAlert() -> MaterialTheme.colorScheme.secondary
        notification.isSplitAlert() -> MaterialTheme.colorScheme.tertiary
        notification.type == NotificationType.ACTIVITY_UPDATE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun notificationTypeLabel(notification: Notification): String {
    return when {
        notification.isPersonalAlert() -> "Personal"
        notification.isSplitAlert() -> "Split"
        notification.type == NotificationType.ACTIVITY_UPDATE -> "Social"
        else -> "System"
    }
}

private enum class NotificationFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    PERSONAL("Personal"),
    SPLIT("Split"),
    SOCIAL("Social"),
}

private fun Notification.matches(filter: NotificationFilter): Boolean {
    return when (filter) {
        NotificationFilter.ALL -> true
        NotificationFilter.UNREAD -> !isRead
        NotificationFilter.PERSONAL -> isPersonalAlert()
        NotificationFilter.SPLIT -> isSplitAlert()
        NotificationFilter.SOCIAL -> type == NotificationType.ACTIVITY_UPDATE
    }
}

private fun Notification.isPersonalAlert(): Boolean {
    return type == NotificationType.TRANSACTION_ALERT
}

private fun Notification.isSplitAlert(): Boolean {
    return type in
        setOf(
            NotificationType.PAYMENT_COMPLETED,
            NotificationType.PAYMENT_PENDING,
            NotificationType.BILL_CREATED,
            NotificationType.SPLIT_COMPLETED,
        )
}

private fun emptyTitleFor(filter: NotificationFilter): String {
    return when (filter) {
        NotificationFilter.ALL -> "No notifications yet"
        NotificationFilter.UNREAD -> "No unread notifications"
        NotificationFilter.PERSONAL -> "No personal alerts"
        NotificationFilter.SPLIT -> "No split updates"
        NotificationFilter.SOCIAL -> "No social updates"
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
