package com.example.dinesplit.domain.model

/**
 * Core notification model for the notification feed.
 * Can represent split payment updates, transaction alerts, and activity notifications.
 */
data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val subtitle: String,
    val type: NotificationType,
    val relatedId: String? = null,  // Can reference Split, Bill, Transaction, Post ID
    val isRead: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

enum class NotificationType {
    PAYMENT_COMPLETED,      // Split payment marked done
    PAYMENT_PENDING,        // Bill waiting for payment
    BILL_CREATED,           // New split bill created
    SPLIT_COMPLETED,        // All members confirmed split
    TRANSACTION_ALERT,      // Personal finance milestone
    ACTIVITY_UPDATE,        // Feed/social activity
    OTHER
}

