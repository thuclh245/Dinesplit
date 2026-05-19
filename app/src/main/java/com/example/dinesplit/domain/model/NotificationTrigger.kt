package com.example.dinesplit.domain.model

import java.text.NumberFormat
import java.util.Locale

data class FeedNotificationTrigger(
    val postId: String,
    val postTitle: String,
    val triggeredByUserId: String,
    val triggeredByUserName: String,
    val triggerType: FeedTriggerType
)

enum class FeedTriggerType {
    POST_LIKED,
    COMMENT_ADDED,
    COMMENT_LIKED,
    POST_SHARED
}

data class SplitNotificationTrigger(
    val billId: String,
    val groupId: String,
    val billTitle: String,
    val amount: Double,
    val triggeredByUserId: String,
    val triggeredByUserName: String,
    val triggerType: SplitTriggerType
)

enum class SplitTriggerType {
    BILL_CREATED,
    PAYMENT_RECEIVED,
    PAYMENT_PENDING,
    BILL_CONFIRMED,
    BILL_SETTLED,
    YOU_OWE_MONEY,
    SOMEONE_OWES_YOU
}

data class PersonalReminderTrigger(
    val categoryId: String?,
    val categoryName: String,
    val currentSpent: Double,
    val budgetLimit: Double,
    val thresholdPercent: Float
)

object NotificationFactory {
    fun fromFeedTrigger(trigger: FeedNotificationTrigger, recipientUserId: String): Notification {
        val (title, subtitle) = when (trigger.triggerType) {
            FeedTriggerType.POST_LIKED ->
                Pair("${trigger.triggeredByUserName} liked your post", trigger.postTitle)
            FeedTriggerType.COMMENT_ADDED ->
                Pair("${trigger.triggeredByUserName} commented", trigger.postTitle)
            FeedTriggerType.COMMENT_LIKED ->
                Pair("${trigger.triggeredByUserName} liked your comment", trigger.postTitle)
            FeedTriggerType.POST_SHARED ->
                Pair("${trigger.triggeredByUserName} shared your post", trigger.postTitle)
        }

        return Notification(
            id = "${System.currentTimeMillis()}_${trigger.postId}",
            userId = recipientUserId,
            title = title,
            subtitle = subtitle,
            type = NotificationType.ACTIVITY_UPDATE,
            relatedId = trigger.postId,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            deepLinkDestination = "ACTIVITY_DETAIL",
            deepLinkTargetId = trigger.postId
        )
    }

    fun fromSplitTrigger(trigger: SplitNotificationTrigger, recipientUserId: String): Notification {
        val (title, notificationType, destination) = when (trigger.triggerType) {
            SplitTriggerType.BILL_CREATED ->
                Triple("${trigger.triggeredByUserName} created a bill", NotificationType.BILL_CREATED, "SPLIT_DETAIL")
            SplitTriggerType.PAYMENT_RECEIVED ->
                Triple("${trigger.triggeredByUserName} paid", NotificationType.PAYMENT_COMPLETED, "SPLIT_SETTLE")
            SplitTriggerType.PAYMENT_PENDING ->
                Triple("Payment due soon", NotificationType.PAYMENT_PENDING, "SPLIT_DETAIL")
            SplitTriggerType.BILL_CONFIRMED ->
                Triple("${trigger.triggeredByUserName} confirmed the bill", NotificationType.SPLIT_COMPLETED, "SPLIT_DETAIL")
            SplitTriggerType.BILL_SETTLED ->
                Triple("${trigger.triggeredByUserName} settled up", NotificationType.SPLIT_COMPLETED, "SPLIT_SETTLE")
            SplitTriggerType.YOU_OWE_MONEY ->
                Triple("You owe ${trigger.triggeredByUserName}", NotificationType.PAYMENT_PENDING, "SPLIT_DETAIL")
            SplitTriggerType.SOMEONE_OWES_YOU ->
                Triple("${trigger.triggeredByUserName} owes you", NotificationType.PAYMENT_PENDING, "SPLIT_SETTLE")
        }

        return Notification(
            id = "${System.currentTimeMillis()}_${trigger.billId}",
            userId = recipientUserId,
            title = title,
            subtitle = "${trigger.billTitle} - ${formatMoney(trigger.amount)}",
            type = notificationType,
            relatedId = trigger.billId,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            deepLinkDestination = destination,
            deepLinkTargetId = trigger.billId
        )
    }

    fun fromReminderTrigger(trigger: PersonalReminderTrigger, userId: String): Notification {
        val percentUsed = (trigger.currentSpent / trigger.budgetLimit * 100).toInt()

        return Notification(
            id = "${System.currentTimeMillis()}_reminder_${trigger.categoryId ?: "overall"}",
            userId = userId,
            title = "Spending alert: ${trigger.categoryName}",
            subtitle = "You have spent $percentUsed% of your ${formatMoney(trigger.budgetLimit)} budget",
            type = NotificationType.TRANSACTION_ALERT,
            relatedId = trigger.categoryId,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            deepLinkDestination = "SPENDING_REMINDERS",
            deepLinkTargetId = trigger.categoryId
        )
    }

    private fun formatMoney(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(amount.toLong())} VND"
    }
}
