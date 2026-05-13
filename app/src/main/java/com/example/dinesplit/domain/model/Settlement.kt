package com.example.dinesplit.domain.model

import java.util.Date

data class Settlement(
    val id: String = "",
    val groupId: String = "",
    val billId: String? = null,
    val fromUid: String = "",
    val toUid: String = "",
    val amount: Double = 0.0,
    val method: String = "manual", // "manual" | "qr_transfer"
    val qrTransactionRef: String = "",
    val note: String = "",
    val status: String = "pending", // "pending" | "confirmed" | "rejected"
    val confirmedAt: Date? = null,
    val createdAt: Date? = null
)
