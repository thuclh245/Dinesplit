package com.example.dinesplit.domain.model

import java.util.Date

data class QrPayment(
    val id: String = "",
    val groupId: String = "",
    val billId: String? = null,
    val payerUid: String = "",
    val receiverUid: String = "",
    val amount: Double = 0.0,
    val bankTransactionRef: String = "",
    val qrContent: String = "",
    val status: String = "pending", // "pending" | "verified" | "failed"
    val verifiedAt: Date? = null,
    val createdAt: Date? = null,
)
