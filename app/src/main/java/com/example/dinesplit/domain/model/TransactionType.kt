package com.example.dinesplit.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE
}

fun transactionTypeFromStringOrNull(value: String?): TransactionType? {
    if (value.isNullOrBlank()) return null
    return when (value.trim().lowercase()) {
        "income" -> TransactionType.INCOME
        "expense" -> TransactionType.EXPENSE
        else -> TransactionType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

fun transactionTypeFromString(value: String?): TransactionType {
    return transactionTypeFromStringOrNull(value) ?: TransactionType.EXPENSE
}

