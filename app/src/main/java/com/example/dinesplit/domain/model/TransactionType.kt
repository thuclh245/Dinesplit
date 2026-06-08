package com.example.dinesplit.domain.model

/**
 * Phân loại của một giao dịch tài chính cá nhân.
 */
enum class TransactionType {
    /** Thu nhập (Incoming money). */
    INCOME,
    /** Chi tiêu (Outgoing money). */
    EXPENSE,
}
