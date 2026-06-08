package com.example.dinesplit.domain.model

/**
 * Lớp dữ liệu biểu diễn một giao dịch tài chính cá nhân trong hệ thống DineSplit.
 *
 * @property id ID duy nhất của giao dịch.
 * @property userId ID người dùng sở hữu giao dịch này.
 * @property amount Số tiền giao dịch.
 * @property type Loại giao dịch (INCOME - Thu nhập hoặc EXPENSE - Chi tiêu).
 * @property categoryId ID danh mục tài chính liên kết (ví dụ: "c_food").
 * @property category Tên hiển thị của danh mục (ví dụ: "Dining Out").
 * @property note Ghi chú hoặc mô tả ngắn về giao dịch.
 * @property date Thời điểm giao dịch phát sinh, tính bằng mili-giây kể từ Epoch.
 * @property createdAt Thời điểm giao dịch được tạo trên hệ thống, tính bằng mili-giây kể từ Epoch.
 * @property source Nguồn gốc tạo giao dịch (ví dụ: thủ công, từ split bill, hoặc định kỳ).
 * @property sourceGroupId ID của nhóm split bill liên kết (nếu có).
 * @property sourceBillId ID của hóa đơn split bill liên kết (nếu có).
 * @property recurringRuleId ID của quy tắc lặp lại tự động liên kết (nếu có).
 * @property receiptImageUrl URL hình ảnh hóa đơn đi kèm (tải lên từ camera hoặc bộ nhớ).
 * @property walletId ID của ví/tài khoản tài chính thực hiện giao dịch (nếu có).
 */
data class Transaction(
    val id: String,
    val userId: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val category: String,
    val note: String?,
    val date: Long,
    val createdAt: Long,
    val source: TransactionSource = TransactionSource.MANUAL,
    val sourceGroupId: String? = null,
    val sourceBillId: String? = null,
    val recurringRuleId: String? = null,
    val receiptImageUrl: String? = null,
    val walletId: String? = null,
)

/**
 * Danh sách các nguồn gốc có thể tạo ra một giao dịch tài chính trong hệ thống DineSplit.
 */
enum class TransactionSource {
    /** Nhập thủ công bằng tay từ giao diện thêm giao dịch. */
    MANUAL,
    /** Tự động sinh ra từ mô-đun chia tiền hóa đơn nhóm (Split Bill). */
    SPLIT,
    /** Tự động sinh ra định kỳ dựa trên cấu hình quy tắc (Recurring Rule). */
    RECURRING,
    /** Sinh ra thông qua quét ảnh hóa đơn/OCR (Receipt). */
    RECEIPT,
}
