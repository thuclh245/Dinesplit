package com.example.dinesplit.domain.model

/**
 * Các chu kỳ lặp lại được hỗ trợ cho giao dịch định kỳ.
 */
enum class RecurringCadence {
    /** Lặp lại hàng tuần. */
    WEEKLY,
    /** Lặp lại hàng tháng. */
    MONTHLY,
}

/**
 * Lớp dữ liệu biểu diễn quy tắc giao dịch tự động lặp lại (ví dụ: đăng ký gói Netflix, đóng tiền điện nước định kỳ).
 *
 * @property id ID duy nhất của quy tắc.
 * @property userId ID người dùng sở hữu quy tắc này.
 * @property name Tên quy tắc lặp lại (ví dụ: "Netflix subscription").
 * @property amount Số tiền giao dịch lặp lại.
 * @property type Loại giao dịch (INCOME hoặc EXPENSE).
 * @property categoryId ID danh mục tài chính liên kết.
 * @property categoryName Tên danh mục tài chính liên kết.
 * @property cadence Chu kỳ lặp lại (hàng tuần hoặc hàng tháng).
 * @property dayOfMonth Ngày chạy giao dịch trong tháng (từ 1 đến 31).
 * @property nextRunAt Thời điểm dự kiến chạy tiếp theo, tính bằng mili-giây kể từ Epoch.
 * @property isEnabled Trạng thái kích hoạt (bật/tắt tự động tạo giao dịch).
 * @property createdAt Thời điểm tạo quy tắc, tính bằng mili-giây kể từ Epoch.
 * @property updatedAt Thời điểm cập nhật quy tắc gần nhất, tính bằng mili-giây kể từ Epoch.
 */
data class RecurringRule(
    val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val categoryName: String,
    val cadence: RecurringCadence,
    val dayOfMonth: Int,
    val nextRunAt: Long,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Trạng thái của một mục tiêu tiết kiệm cá nhân.
 */
enum class GoalStatus {
    /** Đang hoạt động, tích lũy tiền tiết kiệm. */
    ACTIVE,
    /** Đã hoàn thành (đạt số tiền mục tiêu). */
    COMPLETED,
    /** Đang tạm dừng tích lũy. */
    PAUSED,
}

/**
 * Lớp dữ liệu biểu diễn một mục tiêu tiết kiệm tài chính cá nhân (ví dụ: mua laptop, quỹ du lịch).
 *
 * @property id ID duy nhất của mục tiêu.
 * @property userId ID người dùng sở hữu mục tiêu.
 * @property title Tiêu đề mục tiêu (ví dụ: "Mua iPhone 17").
 * @property targetAmount Số tiền mục tiêu cần tiết kiệm.
 * @property currentAmount Số tiền hiện tại đã tiết kiệm được.
 * @property categoryId ID danh mục tài chính liên kết (có thể null nếu là mục tiêu chung).
 * @property deadlineAt Hạn chót đạt được mục tiêu, tính bằng mili-giây kể từ Epoch.
 * @property status Trạng thái hiện tại của mục tiêu tiết kiệm.
 * @property createdAt Thời điểm tạo mục tiêu, tính bằng mili-giây kể từ Epoch.
 * @property updatedAt Thời điểm cập nhật mục tiêu gần nhất, tính bằng mili-giây kể từ Epoch.
 */
data class PersonalGoal(
    val id: String,
    val userId: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val categoryId: String?,
    val deadlineAt: Long,
    val status: GoalStatus,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Các loại ví hoặc tài khoản tài chính cá nhân được hỗ trợ.
 */
enum class WalletType {
    /** Tiền mặt cầm tay. */
    CASH,
    /** Thẻ hoặc tài khoản ngân hàng. */
    BANK,
    /** Ví điện tử (Ví dụ: Momo, ZaloPay). */
    EWALLET,
    /** Thẻ tín dụng/ghi nợ. */
    CREDIT,
}

/**
 * Lớp dữ liệu biểu diễn một ví tiền hoặc tài khoản tài chính cá nhân.
 *
 * @property id ID duy nhất của ví.
 * @property userId ID người dùng sở hữu ví.
 * @property name Tên ví (ví dụ: "Techcombank", "Ví Momo").
 * @property type Phân loại ví tiền.
 * @property balance Số dư hiện tại của ví.
 * @property color Mã màu dạng Hex để hiển thị ví trên giao diện UI (ví dụ: "#FF0000").
 * @property isArchived Trạng thái lưu trữ (đã ngừng hoạt động/ẩn đi).
 * @property createdAt Thời điểm tạo ví, tính bằng mili-giây kể từ Epoch.
 * @property updatedAt Thời điểm cập nhật ví gần nhất, tính bằng mili-giây kể từ Epoch.
 */
data class PersonalWallet(
    val id: String,
    val userId: String,
    val name: String,
    val type: WalletType,
    val balance: Double,
    val color: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
