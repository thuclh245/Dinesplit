package com.example.dinesplit.domain.model

/**
 * Core transaction model for personal finance flow.
 * date and createdAt use epoch milliseconds.
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

enum class TransactionSource {
    MANUAL,
    SPLIT,
    RECURRING,
    RECEIPT,
}
