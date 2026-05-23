package com.example.dinesplit.domain.receipt

import com.example.dinesplit.domain.model.TransactionType

data class ReceiptCategoryOption(
    val id: String,
    val name: String,
    val type: TransactionType
)

data class ReceiptOcrResult(
    val rawText: String,
    val amount: Double?,
    val category: ReceiptCategoryOption?,
    val merchantName: String?
)
