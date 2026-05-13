package com.example.dinesplit.domain.model

import java.util.Date

data class Bill(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val totalAmount: Double = 0.0,
    val currency: String = "VND",
    val paidBy: String = "",
    val splitMethod: String = "equal", // "equal" | "custom" | "by_item"
    val date: Date? = null,
    val note: String = "",
    val receiptUrl: String = "",
    val createdBy: String = "",
    val isSettled: Boolean = false,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
