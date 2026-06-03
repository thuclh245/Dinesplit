package com.example.dinesplit.presentation.notification

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * UI model for notification items in the notification stream.
 * Provides a stable contract for the critical parts of the Notification stream.
 */
data class NotificationItemUi(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val trailingText: String,
    val isRead: Boolean = false
)

/**
 * Notification list state for Personal + Notification owner.
 * The following will connect this with ViewModel + repository.
 */
data class NotificationListState(
    val notifications: List<NotificationItemUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

