package com.example.dinesplit.presentation.personal

import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.WalletType

/**
 * Lấy nhãn hiển thị tiếng Việt tương ứng cho loại giao dịch [TransactionType].
 *
 * @return Chuỗi mô tả tiếng Việt ("Thu nhập" hoặc "Chi tiêu").
 */
fun TransactionType.displayLabel(): String {
    return when (this) {
        TransactionType.INCOME -> "Thu nhập"
        TransactionType.EXPENSE -> "Chi tiêu"
    }
}

/**
 * Lấy nhãn hiển thị tiếng Việt tương ứng cho trạng thái an toàn chi tiêu [SafeToSpendStatus].
 *
 * @return Chuỗi mô tả tiếng Việt ("Khỏe mạnh", "Cần theo dõi" hoặc "Vượt ngưỡng").
 */
fun SafeToSpendStatus.displayLabel(): String {
    return when (this) {
        SafeToSpendStatus.HEALTHY -> "Khỏe mạnh"
        SafeToSpendStatus.WATCH -> "Cần theo dõi"
        SafeToSpendStatus.OVER -> "Vượt ngưỡng"
    }
}

/**
 * Lấy nhãn hiển thị tiếng Việt tương ứng cho loại nhắc nhở [ReminderType].
 *
 * @return Chuỗi mô tả tần suất nhắc nhở tiếng Việt.
 */
fun ReminderType.displayLabel(): String {
    return when (this) {
        ReminderType.DAILY -> "Hằng ngày"
        ReminderType.WEEKLY -> "Hằng tuần"
        ReminderType.MONTHLY -> "Hằng tháng"
        ReminderType.MILESTONE -> "Mốc chi tiêu"
    }
}

/**
 * Lấy nhãn hiển thị tiếng Việt tương ứng cho loại ví [WalletType].
 *
 * @return Chuỗi mô tả loại tài khoản ví tiếng Việt.
 */
fun WalletType.displayLabel(): String {
    return when (this) {
        WalletType.CASH -> "Tiền mặt"
        WalletType.BANK -> "Ngân hàng"
        WalletType.EWALLET -> "Ví điện tử"
        WalletType.CREDIT -> "Tín dụng"
    }
}

/**
 * Chuyển đổi loại giao dịch sang giá trị chuỗi dùng để cấu hình tham số định tuyến (navigation routing).
 *
 * @return Chuỗi route tương ứng ("income" hoặc "expense").
 */
fun TransactionType.toRouteValue(): String {
    return when (this) {
        TransactionType.INCOME -> "income"
        TransactionType.EXPENSE -> "expense"
    }
}

/**
 * Phân tích và tạo đối tượng [TransactionType] tương ứng từ giá trị chuỗi cấu hình route.
 *
 * @param value Chuỗi truyền vào từ route tham số.
 * @return Đối tượng [TransactionType] tương ứng hoặc null nếu không thể ánh xạ.
 */
fun transactionTypeFromRoute(value: String?): TransactionType? {
    if (value.isNullOrBlank()) return null
    return when (value.trim().lowercase()) {
        "income" -> TransactionType.INCOME
        "expense" -> TransactionType.EXPENSE
        else -> TransactionType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
