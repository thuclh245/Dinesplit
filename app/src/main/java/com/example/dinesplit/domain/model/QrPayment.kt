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
    val status: String = "PENDING", // PENDING | MARKED_PAID | CONFIRMED | REJECTED
    val description: String = "",
    val paymentGateway: String = "",
    val verifiedAt: Date? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
)

object QrPaymentStatus {
    const val PENDING = "PENDING"
    const val MARKED_PAID = "MARKED_PAID"
    const val CONFIRMED = "CONFIRMED"
    const val REJECTED = "REJECTED"
}
