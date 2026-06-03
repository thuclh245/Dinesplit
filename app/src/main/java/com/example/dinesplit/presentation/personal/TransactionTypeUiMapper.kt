package com.example.dinesplit.presentation.personal

import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.WalletType

fun TransactionType.displayLabel(): String {
    return when (this) {
        TransactionType.INCOME -> "Thu nhập"
        TransactionType.EXPENSE -> "Chi tiêu"
    }
}

fun SafeToSpendStatus.displayLabel(): String {
    return when (this) {
        SafeToSpendStatus.HEALTHY -> "Khỏe mạnh"
        SafeToSpendStatus.WATCH -> "Cần theo dõi"
        SafeToSpendStatus.OVER -> "Vượt ngưỡng"
    }
}

fun ReminderType.displayLabel(): String {
    return when (this) {
        ReminderType.DAILY -> "Hằng ngày"
        ReminderType.WEEKLY -> "Hằng tuần"
        ReminderType.MONTHLY -> "Hằng tháng"
        ReminderType.MILESTONE -> "Mốc chi tiêu"
    }
}

fun WalletType.displayLabel(): String {
    return when (this) {
        WalletType.CASH -> "Tiền mặt"
        WalletType.BANK -> "Ngân hàng"
        WalletType.EWALLET -> "Ví điện tử"
        WalletType.CREDIT -> "Tín dụng"
    }
}

fun TransactionType.toRouteValue(): String {
    return when (this) {
        TransactionType.INCOME -> "income"
        TransactionType.EXPENSE -> "expense"
    }
}

fun transactionTypeFromRoute(value: String?): TransactionType? {
    if (value.isNullOrBlank()) return null
    return when (value.trim().lowercase()) {
        "income" -> TransactionType.INCOME
        "expense" -> TransactionType.EXPENSE
        else -> TransactionType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
