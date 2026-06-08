package com.example.dinesplit.domain.model

/**
 * Lớp dữ liệu biểu diễn nhắc nhở/cảnh báo chi tiêu và giới hạn ngân sách của người dùng.
 *
 * @property id ID duy nhất của nhắc nhở.
 * @property userId ID người dùng sở hữu nhắc nhở.
 * @property categoryId ID danh mục tài chính được đặt ngân sách (nếu là null, biểu thị giới hạn tổng thể của tất cả các danh mục).
 * @property categoryName Tên danh mục được đặt ngân sách (mặc định là "Overall" nếu categoryId là null).
 * @property budgetAmount Số tiền giới hạn ngân sách được thiết lập.
 * @property currentSpent Số tiền đã chi tiêu thực tế hiện tại thuộc danh mục này trong chu kỳ.
 * @property threshold Tỷ lệ chi tiêu chạm ngưỡng để phát ra cảnh báo (ví dụ: 0.8f tương ứng 80% ngân sách).
 * @property reminderType Tần suất hoặc loại nhắc nhở chi tiêu (Daily, Weekly, Monthly, Milestone).
 * @property isEnabled Trạng thái kích hoạt nhắc nhở (bật hoặc tắt).
 * @property lastAlertedAt Thời điểm gần nhất hệ thống phát ra cảnh báo cho nhắc nhở này, tính bằng mili-giây kể từ Epoch.
 * @property createdAt Thời điểm tạo nhắc nhở, tính bằng mili-giây kể từ Epoch.
 * @property updatedAt Thời điểm cập nhật nhắc nhở gần nhất, tính bằng mili-giây kể từ Epoch.
 */
data class SpendingReminder(
    val id: String,
    val userId: String,
    val categoryId: String? = null,
    val categoryName: String = "Overall",
    val budgetAmount: Double,
    val currentSpent: Double = 0.0,
    val threshold: Float = 0.8f,
    val reminderType: ReminderType = ReminderType.WEEKLY,
    val isEnabled: Boolean = true,
    val lastAlertedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Các loại tần suất hoặc cơ chế kích hoạt nhắc nhở chi tiêu ngân sách.
 */
enum class ReminderType {
    /** Kiểm tra và nhắc nhở chi tiêu hàng ngày. */
    DAILY,
    /** Kiểm tra và nhắc nhở chi tiêu hàng tuần. */
    WEEKLY,
    /** Kiểm tra và nhắc nhở chi tiêu hàng tháng. */
    MONTHLY,
    /** Nhắc nhở/cảnh báo khi đạt đến các mốc chi tiêu cụ thể (Ví dụ: đã tiêu 1, 2, 5 triệu đồng). */
    MILESTONE,
}
