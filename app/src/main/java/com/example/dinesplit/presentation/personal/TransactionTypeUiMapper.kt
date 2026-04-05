package com.example.dinesplit.presentation.personal

import com.example.dinesplit.domain.model.TransactionType

fun TransactionType.displayLabel(): String {
    return when (this) {
        TransactionType.INCOME -> "Income"
        TransactionType.EXPENSE -> "Expense"
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

