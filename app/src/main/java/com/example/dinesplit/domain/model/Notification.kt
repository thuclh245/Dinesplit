package com.example.dinesplit.domain.model

/**
 * Mô hình thông báo cốt lõi cho nguồn cấp thông báo của ứng dụng.
 * Đại diện cho các loại thông báo: cập nhật thanh toán chia tiền, cảnh báo chi tiêu ngân sách,
 * hoạt động mạng xã hội, và các thông báo hệ thống khác.
 *
 * @property id ID duy nhất của thông báo (thường là timestamp + ID đối tượng liên quan).
 * @property userId ID người dùng nhận thông báo.
 * @property title Tiêu đề chính của thông báo.
 * @property subtitle Mô tả phụ / nội dung bổ sung.
 * @property type Loại thông báo [NotificationType].
 * @property relatedId ID đối tượng liên quan (có thể là ID hóa đơn, giao dịch, bài viết, v.v.).
 * @property isRead Trạng thái đã đọc hay chưa.
 * @property createdAt Thời điểm tạo thông báo tính bằng mili-giây.
 * @property updatedAt Thời điểm cập nhật cuối cùng tính bằng mili-giây.
 * @property deepLinkDestination Tên đích đến deep link [NotificationDestination] dùng để điều hướng khi bấm vào thông báo.
 * @property deepLinkTargetId ID đích đến deep link (ví dụ: ID bài viết, ID hóa đơn).
 * @property senderId ID người gửi/người kích hoạt thông báo (nếu có).
 * @property groupId ID nhóm chia tiền liên quan (nếu có).
 */
data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val subtitle: String,
    val type: NotificationType,
    val relatedId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deepLinkDestination: String? = null,
    val deepLinkTargetId: String? = null,
    val senderId: String? = null,
    val groupId: String? = null,
)

/**
 * Enum định nghĩa các loại thông báo trong ứng dụng.
 */
enum class NotificationType {
    /** Thanh toán chia tiền đã hoàn tất. */
    PAYMENT_COMPLETED,
    /** Hóa đơn đang chờ thanh toán. */
    PAYMENT_PENDING,
    /** Hóa đơn chia tiền mới được tạo. */
    BILL_CREATED,
    /** Tất cả thành viên đã xác nhận chia tiền. */
    SPLIT_COMPLETED,
    /** Cảnh báo chi tiêu / ngân sách. */
    TRANSACTION_ALERT,
    /** Hoạt động mạng xã hội (like, bình luận, chia sẻ). */
    ACTIVITY_UPDATE,
    /** Loại thông báo khác. */
    OTHER,
}
