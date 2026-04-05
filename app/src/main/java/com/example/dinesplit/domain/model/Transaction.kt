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
    val category: String,
    val note: String?,
    val date: Long,
    val createdAt: Long
)

enum class TransactionType {
    INCOME,
    EXPENSE
}

