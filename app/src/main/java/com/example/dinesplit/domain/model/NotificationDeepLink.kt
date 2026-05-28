package com.example.dinesplit.domain.model

/**
 * Deep link data for notification routing.
 * Allows notifications to navigate to specific screens within the app.
 */
data class NotificationDeepLink(
    val notificationId: String,
    val destination: NotificationDestination,
    val targetId: String? = null
)

enum class NotificationDestination {
    SPLIT_DETAIL,           // Navigate to a specific split/bill
    TRANSACTION_DETAIL,     // Navigate to a transaction
    SPLIT_SETTLE,           // Navigate to settle screen
    ACTIVITY_DETAIL,        // Navigate to post/activity
    PROFILE,                // Navigate to user profile
    NONE
}

