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
    fun fromFeedTrigger(trigger: FeedNotificationTrigger, recipientUserId: String): Notification {
        val (title, subtitle) = when (trigger.triggerType) {
            FeedTriggerType.POST_LIKED ->
                Pair("${trigger.triggeredByUserName} đã thích bài viết của bạn", trigger.postTitle)
            FeedTriggerType.COMMENT_ADDED ->
                Pair("${trigger.triggeredByUserName} đã bình luận", trigger.postTitle)
            FeedTriggerType.COMMENT_LIKED ->
                Pair("${trigger.triggeredByUserName} đã thích bình luận của bạn", trigger.postTitle)
            FeedTriggerType.POST_SHARED ->
                Pair("${trigger.triggeredByUserName} đã chia sẻ bài viết của bạn", trigger.postTitle)
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

    fun fromSplitTrigger(trigger: SplitNotificationTrigger, recipientUserId: String): Notification {
        val (title, notificationType, destination) = when (trigger.triggerType) {
            SplitTriggerType.BILL_CREATED ->
                Triple("${trigger.triggeredByUserName} đã tạo hóa đơn", NotificationType.BILL_CREATED, "SPLIT_DETAIL")
            SplitTriggerType.PAYMENT_RECEIVED ->
                Triple("${trigger.triggeredByUserName} đã thanh toán", NotificationType.PAYMENT_COMPLETED, "SPLIT_SETTLE")
            SplitTriggerType.PAYMENT_PENDING ->
                Triple("Sắp đến hạn thanh toán", NotificationType.PAYMENT_PENDING, "SPLIT_DETAIL")
            SplitTriggerType.BILL_CONFIRMED ->
                Triple("${trigger.triggeredByUserName} đã xác nhận hóa đơn", NotificationType.SPLIT_COMPLETED, "SPLIT_DETAIL")
            SplitTriggerType.BILL_SETTLED ->
                Triple("${trigger.triggeredByUserName} đã tất toán", NotificationType.SPLIT_COMPLETED, "SPLIT_SETTLE")
            SplitTriggerType.YOU_OWE_MONEY ->
                Triple("Bạn cần trả ${trigger.triggeredByUserName}", NotificationType.PAYMENT_PENDING, "SPLIT_DETAIL")
            SplitTriggerType.SOMEONE_OWES_YOU ->
                Triple("${trigger.triggeredByUserName} cần trả bạn", NotificationType.PAYMENT_PENDING, "SPLIT_SETTLE")
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
            senderId = trigger.triggeredByUserId,
            groupId = trigger.groupId,
        )
    }

    fun fromPersonalTrigger(
        trigger: PersonalNotificationTrigger,
        userId: String,
    ): Notification {
        val amountText = trigger.amount?.let { formatMoney(it) }
        val (title, subtitle, destination) = when (trigger.triggerType) {
            PersonalTriggerType.TRANSACTION_ADDED -> Triple(
                "Đã lưu giao dịch cá nhân",
                listOfNotNull(trigger.categoryName, amountText).joinToString(" - "),
                "TRANSACTION_DETAIL"
            )
            PersonalTriggerType.CATEGORY_CREATED -> Triple(
                "Danh mục đã sẵn sàng",
                "${trigger.label} hiện đã có trong Cá nhân",
                "CATEGORY_MANAGEMENT"
            )
            PersonalTriggerType.REMINDER_CREATED -> Triple(
                "Đã bật cảnh báo ngân sách",
                "${trigger.label} ở mức ${amountText ?: "ngân sách bạn chọn"}",
                "SPENDING_REMINDERS"
            )
            PersonalTriggerType.REMINDER_THRESHOLD_REACHED -> Triple(
                "Cảnh báo chi tiêu: ${trigger.categoryName ?: trigger.label}",
                amountText?.let { "Chi tiêu hiện tại là $it" } ?: trigger.label,
                "SPENDING_REMINDERS"
            )
            PersonalTriggerType.RECURRING_RULE_CREATED -> Triple(
                "Đã thêm khoản lặp lại",
                "${trigger.label}${amountText?.let { " - $it" }.orEmpty()}",
                "PERSONAL_PLANS"
            )
            PersonalTriggerType.GOAL_CREATED -> Triple(
                "Đã thêm mục tiêu",
                "${trigger.label}${amountText?.let { " - mục tiêu $it" }.orEmpty()}",
                "PERSONAL_PLANS"
            )
            PersonalTriggerType.WALLET_CREATED -> Triple(
                "Đã thêm ví",
                "${trigger.label}${amountText?.let { " - số dư $it" }.orEmpty()}",
                "PERSONAL_PLANS"
            )
            PersonalTriggerType.SAFE_TO_SPEND_CHANGED -> Triple(
                "Đã cập nhật mức an toàn chi tiêu",
                "${trigger.label}${amountText?.let { " - $it hôm nay" }.orEmpty()}",
                "PERSONAL"
            )
            PersonalTriggerType.PERSONAL_SCORE_CHANGED -> Triple(
                "Điểm cá nhân: ${trigger.band ?: "đã cập nhật"}",
                "Điểm hiện tại ${trigger.score ?: 0}. ${trigger.label}",
                "PERSONAL"
            )
            PersonalTriggerType.SPLIT_BRIDGED_TO_PERSONAL -> Triple(
                "Đã lưu chia tách vào Cá nhân",
                "${trigger.label}${amountText?.let { " - $it" }.orEmpty()}",
                "TRANSACTION_DETAIL"
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
            trigger = PersonalNotificationTrigger(
                relatedId = trigger.categoryId,
                label = "Bạn đã dùng $percentUsed% ngân sách ${formatMoney(trigger.budgetLimit)}",
                amount = trigger.currentSpent,
                categoryName = trigger.categoryName,
                triggerType = PersonalTriggerType.REMINDER_THRESHOLD_REACHED
            ),
            userId = userId
        )
    }

    fun transactionAdded(
        amount: Double,
        categoryName: String,
        type: TransactionType,
        userId: String,
        transactionId: String? = null,
    ): Notification {
        val typeLabel = if (type == TransactionType.EXPENSE) "chi tiêu" else "thu nhập"
        return fromPersonalTrigger(
            trigger = PersonalNotificationTrigger(
                relatedId = transactionId,
                label = "Đã thêm $typeLabel",
                amount = amount,
                categoryName = categoryName,
                triggerType = PersonalTriggerType.TRANSACTION_ADDED
            ),
            userId = userId
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
        val typeLabel = if (type == TransactionType.EXPENSE) "chi tiêu" else "thu nhập"
        return fromPersonalTrigger(
            trigger = PersonalNotificationTrigger(
                label = "Danh mục $typeLabel '$categoryName'",
                categoryName = categoryName,
                triggerType = PersonalTriggerType.CATEGORY_CREATED
            ),
            userId = userId
        )
    }

    private fun formatMoney(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(amount.toLong())} VND"
    }
}
