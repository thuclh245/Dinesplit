package com.example.dinesplit.presentation.personal

import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.transactionTypeFromStringOrNull

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
    return transactionTypeFromStringOrNull(value)
}

