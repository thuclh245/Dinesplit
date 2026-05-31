package com.example.dinesplit.presentation.notification

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * UI model for notification items in the notification feed.
 * Provides a stable contract for tuần 1 of the Notification flow.
 */
data class NotificationItemUi(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val trailingText: String,
    val isRead: Boolean = false,
)

/**
 * Notification list state for Personal + Notification owner.
 * Tuần 2 sẽ connect this to ViewModel + repository.
 */
data class NotificationListState(
    val notifications: List<NotificationItemUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
