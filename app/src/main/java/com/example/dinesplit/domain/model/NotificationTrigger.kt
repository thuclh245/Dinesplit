package com.example.dinesplit.domain.model

import java.text.NumberFormat
import java.util.Locale

data class FeedNotificationTrigger(
    val postId: String,
    val postTitle: String,
    val triggeredByUserId: String,
    val triggeredByUserName: String,
    val triggerType: FeedTriggerType,
)

enum class FeedTriggerType {
    POST_LIKED,
    COMMENT_ADDED,
    COMMENT_LIKED,
    POST_SHARED,
}

data class SplitNotificationTrigger(
    val billId: String,
    val groupId: String,
    val billTitle: String,
    val amount: Double,
    val triggeredByUserId: String,
    val triggeredByUserName: String,
    val triggerType: SplitTriggerType,
)

enum class SplitTriggerType {
    BILL_CREATED,
    PAYMENT_RECEIVED,
    PAYMENT_PENDING,
    BILL_CONFIRMED,
    BILL_SETTLED,
    YOU_OWE_MONEY,
    SOMEONE_OWES_YOU,
}

data class PersonalNotificationTrigger(
    val relatedId: String? = null,
    val label: String,
    val amount: Double? = null,
    val categoryName: String? = null,
    val score: Int? = null,
    val band: String? = null,
    val triggerType: PersonalTriggerType,
)

enum class PersonalTriggerType {
    TRANSACTION_ADDED,
    CATEGORY_CREATED,
    REMINDER_CREATED,
    REMINDER_THRESHOLD_REACHED,
    RECURRING_RULE_CREATED,
    GOAL_CREATED,
    WALLET_CREATED,
    SAFE_TO_SPEND_CHANGED,
    PERSONAL_SCORE_CHANGED,
    SPLIT_BRIDGED_TO_PERSONAL,
}

data class PersonalReminderTrigger(
    val categoryId: String?,
    val categoryName: String,
    val currentSpent: Double,
    val budgetLimit: Double,
    val thresholdPercent: Float,
)

object NotificationFactory {
    fun fromFeedTrigger(
        trigger: FeedNotificationTrigger,
        recipientUserId: String,
    ): Notification {
        val (title, subtitle) =
            when (trigger.triggerType) {
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
            deepLinkTargetId = trigger.postId,
        )
    }

    fun fromSplitTrigger(
        trigger: SplitNotificationTrigger,
        recipientUserId: String,
    ): Notification {
        val (title, notificationType, destination) =
            when (trigger.triggerType) {
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
            deepLinkTargetId = trigger.billId,
        )
    }

    fun fromPersonalTrigger(
        trigger: PersonalNotificationTrigger,
        userId: String,
    ): Notification {
        val amountText = trigger.amount?.let { formatMoney(it) }
        val (title, subtitle, destination) =
            when (trigger.triggerType) {
                PersonalTriggerType.TRANSACTION_ADDED ->
                    Triple(
                        "Personal transaction saved",
                        listOfNotNull(trigger.categoryName, amountText).joinToString(" - "),
                        "TRANSACTION_DETAIL",
                    )
                PersonalTriggerType.CATEGORY_CREATED ->
                    Triple(
                        "Category ready",
                        "${trigger.label} is now available in Personal",
                        "CATEGORY_MANAGEMENT",
                    )
                PersonalTriggerType.REMINDER_CREATED ->
                    Triple(
                        "Budget guard enabled",
                        "${trigger.label} at ${amountText ?: "your selected budget"}",
                        "SPENDING_REMINDERS",
                    )
                PersonalTriggerType.REMINDER_THRESHOLD_REACHED ->
                    Triple(
                        "Spending alert: ${trigger.categoryName ?: trigger.label}",
                        amountText?.let { "Current spending is $it" } ?: trigger.label,
                        "SPENDING_REMINDERS",
                    )
                PersonalTriggerType.RECURRING_RULE_CREATED ->
                    Triple(
                        "Recurring radar added",
                        "${trigger.label}${amountText?.let { " - $it" }.orEmpty()}",
                        "PERSONAL_PLANS",
                    )
                PersonalTriggerType.GOAL_CREATED ->
                    Triple(
                        "Goal added",
                        "${trigger.label}${amountText?.let { " - target $it" }.orEmpty()}",
                        "PERSONAL_PLANS",
                    )
                PersonalTriggerType.WALLET_CREATED ->
                    Triple(
                        "Wallet added",
                        "${trigger.label}${amountText?.let { " - balance $it" }.orEmpty()}",
                        "PERSONAL_PLANS",
                    )
                PersonalTriggerType.SAFE_TO_SPEND_CHANGED ->
                    Triple(
                        "Safe-to-spend updated",
                        "${trigger.label}${amountText?.let { " - $it today" }.orEmpty()}",
                        "PERSONAL",
                    )
                PersonalTriggerType.PERSONAL_SCORE_CHANGED ->
                    Triple(
                        "Personal score: ${trigger.band ?: "updated"}",
                        "Current score ${trigger.score ?: 0}. ${trigger.label}",
                        "PERSONAL",
                    )
                PersonalTriggerType.SPLIT_BRIDGED_TO_PERSONAL ->
                    Triple(
                        "Split saved to Personal",
                        "${trigger.label}${amountText?.let { " - $it" }.orEmpty()}",
                        "TRANSACTION_DETAIL",
                    )
            }

        val now = System.currentTimeMillis()
        return Notification(
            id = "${now}_personal_${trigger.triggerType.name.lowercase()}_${trigger.relatedId.orEmpty()}",
            userId = userId,
            title = title,
            subtitle = subtitle.ifBlank { trigger.label },
            type = NotificationType.TRANSACTION_ALERT,
            relatedId = trigger.relatedId,
            isRead = false,
            createdAt = now,
            updatedAt = now,
            deepLinkDestination = destination,
            deepLinkTargetId = trigger.relatedId,
        )
    }

    fun fromReminderTrigger(
        trigger: PersonalReminderTrigger,
        userId: String,
    ): Notification {
        val percentUsed = (trigger.currentSpent / trigger.budgetLimit * 100).toInt()

        return fromPersonalTrigger(
            trigger =
                PersonalNotificationTrigger(
                    relatedId = trigger.categoryId,
                    label = "You have spent $percentUsed% of your ${formatMoney(trigger.budgetLimit)} budget",
                    amount = trigger.currentSpent,
                    categoryName = trigger.categoryName,
                    triggerType = PersonalTriggerType.REMINDER_THRESHOLD_REACHED,
                ),
            userId = userId,
        )
    }

    fun transactionAdded(
        amount: Double,
        categoryName: String,
        type: TransactionType,
        userId: String,
        transactionId: String? = null,
    ): Notification {
        val typeLabel = if (type == TransactionType.EXPENSE) "Expense" else "Income"
        return fromPersonalTrigger(
            trigger =
                PersonalNotificationTrigger(
                    relatedId = transactionId,
                    label = "$typeLabel added",
                    amount = amount,
                    categoryName = categoryName,
                    triggerType = PersonalTriggerType.TRANSACTION_ADDED,
                ),
            userId = userId,
        )
    }

    fun reminderCreated(
        categoryName: String,
        budgetAmount: Double,
        userId: String,
    ): Notification {
        return fromPersonalTrigger(
            trigger =
                PersonalNotificationTrigger(
                    label = categoryName,
                    amount = budgetAmount,
                    categoryName = categoryName,
                    triggerType = PersonalTriggerType.REMINDER_CREATED,
                ),
            userId = userId,
        )
    }

    fun categoryCreated(
        categoryName: String,
        type: TransactionType,
        userId: String,
    ): Notification {
        val typeLabel = if (type == TransactionType.EXPENSE) "Expense" else "Income"
        return fromPersonalTrigger(
            trigger =
                PersonalNotificationTrigger(
                    label = "$typeLabel category '$categoryName'",
                    categoryName = categoryName,
                    triggerType = PersonalTriggerType.CATEGORY_CREATED,
                ),
            userId = userId,
        )
    }

    private fun formatMoney(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(amount.toLong())} VND"
    }
}
