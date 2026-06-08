package com.example.dinesplit.domain.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Mô hình dữ liệu kích hoạt thông báo từ hoạt động mạng xã hội (Feed).
 *
 * @property postId ID bài viết liên quan.
 * @property postTitle Tiêu đề bài viết.
 * @property triggeredByUserId ID người dùng kích hoạt hành động.
 * @property triggeredByUserName Tên hiển thị của người kích hoạt.
 * @property triggerType Loại hành động kích hoạt [FeedTriggerType].
 */
data class FeedNotificationTrigger(
    val postId: String,
    val postTitle: String,
    val triggeredByUserId: String,
    val triggeredByUserName: String,
    val triggerType: FeedTriggerType,
)

/**
 * Enum định nghĩa các loại hành động kích hoạt thông báo từ hoạt động mạng xã hội.
 */
enum class FeedTriggerType {
    /** Người dùng đã thích bài viết. */
    POST_LIKED,
    /** Người dùng đã thêm bình luận. */
    COMMENT_ADDED,
    /** Người dùng đã thích bình luận. */
    COMMENT_LIKED,
    /** Người dùng đã chia sẻ bài viết. */
    POST_SHARED,
}

/**
 * Mô hình dữ liệu kích hoạt thông báo từ hoạt động chia tiền nhóm.
 *
 * @property billId ID hóa đơn liên quan.
 * @property groupId ID nhóm chia tiền.
 * @property billTitle Tiêu đề hóa đơn.
 * @property amount Số tiền liên quan.
 * @property triggeredByUserId ID người dùng kích hoạt.
 * @property triggeredByUserName Tên hiển thị của người kích hoạt.
 * @property triggerType Loại hành động kích hoạt [SplitTriggerType].
 */
data class SplitNotificationTrigger(
    val billId: String,
    val groupId: String,
    val billTitle: String,
    val amount: Double,
    val triggeredByUserId: String,
    val triggeredByUserName: String,
    val triggerType: SplitTriggerType,
)

/**
 * Enum định nghĩa các loại hành động kích hoạt thông báo từ chia tiền nhóm.
 */
enum class SplitTriggerType {
    /** Hóa đơn mới được tạo. */
    BILL_CREATED,
    /** Nhận được khoản thanh toán. */
    PAYMENT_RECEIVED,
    /** Khoản thanh toán đang chờ. */
    PAYMENT_PENDING,
    /** Hóa đơn đã được xác nhận. */
    BILL_CONFIRMED,
    /** Hóa đơn đã được tất toán hoàn toàn. */
    BILL_SETTLED,
    /** Bạn đang nợ tiền người khác. */
    YOU_OWE_MONEY,
    /** Ai đó đang nợ tiền bạn. */
    SOMEONE_OWES_YOU,
}

/**
 * Mô hình dữ liệu kích hoạt thông báo từ tài chính cá nhân (nhắc nhở chi tiêu, mục tiêu).
 *
 * @property relatedId ID đối tượng liên quan (danh mục, giao dịch, v.v.).
 * @property label Nhãn hiển thị mô tả nội dung kích hoạt.
 * @property amount Số tiền liên quan (nếu có).
 * @property categoryName Tên danh mục liên quan (nếu có).
 * @property triggerType Loại kích hoạt [PersonalTriggerType].
 */
data class PersonalNotificationTrigger(
    val relatedId: String? = null,
    val label: String,
    val amount: Double? = null,
    val categoryName: String? = null,
    val triggerType: PersonalTriggerType,
)

/**
 * Enum định nghĩa các loại kích hoạt thông báo tài chính cá nhân.
 */
enum class PersonalTriggerType {
    /** Nhắc nhở chi tiêu mới được tạo thành công. */
    REMINDER_CREATED,
    /** Chi tiêu đã chạm hoặc vượt ngưỡng cảnh báo. */
    REMINDER_THRESHOLD_REACHED,
}

/**
 * Mô hình dữ liệu kích hoạt cảnh báo ngân sách khi chi tiêu vượt ngưỡng.
 *
 * @property categoryId ID danh mục chi tiêu (null nếu là ngân sách tổng).
 * @property categoryName Tên danh mục.
 * @property currentSpent Số tiền đã chi tiêu hiện tại.
 * @property budgetLimit Giới hạn ngân sách đã thiết lập.
 * @property thresholdPercent Ngưỡng phần trăm cảnh báo (ví dụ: 0.8 cho 80%).
 */
data class PersonalReminderTrigger(
    val categoryId: String?,
    val categoryName: String,
    val currentSpent: Double,
    val budgetLimit: Double,
    val thresholdPercent: Float,
)

/**
 * Factory object tạo các đối tượng [Notification] từ các trigger kích hoạt khác nhau.
 * Đóng vai trò trung tâm xử lý việc ánh xạ dữ liệu nghiệp vụ thành thông báo hiển thị cho người dùng.
 */
object NotificationFactory {
    /**
     * Tạo thông báo từ kích hoạt hoạt động mạng xã hội (like, bình luận, chia sẻ bài viết).
     *
     * @param trigger Dữ liệu kích hoạt [FeedNotificationTrigger].
     * @param recipientUserId ID người nhận thông báo.
     * @return Đối tượng [Notification] với deep link đến chi tiết hoạt động.
     */
    fun fromFeedTrigger(
        trigger: FeedNotificationTrigger,
        recipientUserId: String,
    ): Notification {
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
            deepLinkDestination = NotificationDestination.ACTIVITY_DETAIL.name,
            deepLinkTargetId = trigger.postId,
        )
    }

    /**
     * Tạo thông báo từ kích hoạt hoạt động chia tiền nhóm (tạo hóa đơn, thanh toán, tất toán).
     *
     * @param trigger Dữ liệu kích hoạt [SplitNotificationTrigger].
     * @param recipientUserId ID người nhận thông báo.
     * @return Đối tượng [Notification] với deep link đến chi tiết chia tiền hoặc màn hình tất toán.
     */
    fun fromSplitTrigger(
        trigger: SplitNotificationTrigger,
        recipientUserId: String,
    ): Notification {
        val (title, notificationType, destination) = when (trigger.triggerType) {
            SplitTriggerType.BILL_CREATED ->
                Triple(
                    "${trigger.triggeredByUserName} đã tạo hóa đơn",
                    NotificationType.BILL_CREATED,
                    NotificationDestination.SPLIT_DETAIL,
                )
            SplitTriggerType.PAYMENT_RECEIVED ->
                Triple(
                    "${trigger.triggeredByUserName} đã thanh toán",
                    NotificationType.PAYMENT_COMPLETED,
                    NotificationDestination.SPLIT_SETTLE,
                )
            SplitTriggerType.PAYMENT_PENDING ->
                Triple(
                    "Sắp đến hạn thanh toán",
                    NotificationType.PAYMENT_PENDING,
                    NotificationDestination.SPLIT_DETAIL,
                )
            SplitTriggerType.BILL_CONFIRMED ->
                Triple(
                    "${trigger.triggeredByUserName} đã xác nhận hóa đơn",
                    NotificationType.SPLIT_COMPLETED,
                    NotificationDestination.SPLIT_DETAIL,
                )
            SplitTriggerType.BILL_SETTLED ->
                Triple(
                    "${trigger.triggeredByUserName} đã tất toán",
                    NotificationType.SPLIT_COMPLETED,
                    NotificationDestination.SPLIT_SETTLE,
                )
            SplitTriggerType.YOU_OWE_MONEY ->
                Triple(
                    "Bạn cần trả ${trigger.triggeredByUserName}",
                    NotificationType.PAYMENT_PENDING,
                    NotificationDestination.SPLIT_DETAIL,
                )
            SplitTriggerType.SOMEONE_OWES_YOU ->
                Triple(
                    "${trigger.triggeredByUserName} cần trả bạn",
                    NotificationType.PAYMENT_PENDING,
                    NotificationDestination.SPLIT_SETTLE,
                )
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
            deepLinkDestination = destination.name,
            deepLinkTargetId = trigger.billId,
            senderId = trigger.triggeredByUserId,
            groupId = trigger.groupId,
        )
    }

    /**
     * Tạo thông báo từ kích hoạt tài chính cá nhân (tạo nhắc nhở, cảnh báo ngưỡng chi tiêu).
     *
     * @param trigger Dữ liệu kích hoạt [PersonalNotificationTrigger].
     * @param userId ID người dùng nhận thông báo.
     * @return Đối tượng [Notification] với deep link đến màn hình nhắc nhở chi tiêu.
     */
    fun fromPersonalTrigger(
        trigger: PersonalNotificationTrigger,
        userId: String,
    ): Notification {
        val amountText = trigger.amount?.let { formatMoney(it) }
        val (title, subtitle, destination) = when (trigger.triggerType) {
            PersonalTriggerType.REMINDER_CREATED ->
                Triple(
                    "Đã bật cảnh báo ngân sách",
                    "${trigger.label} ở mức ${amountText ?: "ngân sách bạn chọn"}",
                    NotificationDestination.SPENDING_REMINDERS,
                )
            PersonalTriggerType.REMINDER_THRESHOLD_REACHED ->
                Triple(
                    "Cảnh báo chi tiêu: ${trigger.categoryName ?: trigger.label}",
                    amountText?.let { "Chi tiêu hiện tại là $it" } ?: trigger.label,
                    NotificationDestination.SPENDING_REMINDERS,
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
            deepLinkDestination = destination.name,
            deepLinkTargetId = trigger.relatedId,
        )
    }

    /**
     * Tạo thông báo cảnh báo khi chi tiêu đã vượt ngưỡng ngân sách nhắc nhở.
     * Nội bộ chuyển đổi [PersonalReminderTrigger] thành [PersonalNotificationTrigger] và gọi [fromPersonalTrigger].
     *
     * @param trigger Dữ liệu kích hoạt ngưỡng [PersonalReminderTrigger].
     * @param userId ID người dùng nhận thông báo.
     * @return Đối tượng [Notification] cảnh báo vượt ngưỡng chi tiêu.
     */
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
                triggerType = PersonalTriggerType.REMINDER_THRESHOLD_REACHED,
            ),
            userId = userId,
        )
    }

    /**
     * Tạo thông báo hệ thống khi người dùng thiết lập nhắc nhở chi tiêu mới thành công.
     *
     * @param categoryName Tên danh mục nhắc nhở.
     * @param budgetAmount Hạn mức ngân sách đã thiết lập.
     * @param userId ID người dùng.
     * @return Đối tượng [Notification] xác nhận tạo nhắc nhở thành công.
     */
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

    /**
     * Định dạng số tiền thành chuỗi tiền tệ Việt Nam kèm hậu tố "VND".
     *
     * @param amount Số tiền cần định dạng.
     * @return Chuỗi số tiền đã định dạng. Ví dụ: "500.000 VND".
     */
    private fun formatMoney(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(amount.toLong())} VND"
    }
}
