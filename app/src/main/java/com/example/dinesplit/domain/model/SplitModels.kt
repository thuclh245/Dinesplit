package com.example.dinesplit.domain.model

import java.util.UUID

enum class SplitMethod {
    EQUAL,
    CUSTOM,
    ITEMIZED,
}

enum class PaymentStatus {
    PAYER,
    PAID,
    UNPAID,
}

enum class BillStatus {
    OPEN,
    SETTLED,
}

data class Member(
    val id: String,
    val name: String,
    val initial: String,
    val avatarUrl: String = "",
    val isMe: Boolean = false
)

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val sharedByMemberIds: List<String>,
)

data class Bill(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val name: String,
    val totalAmount: Double,
    val payerId: String,
    val method: SplitMethod,
    val items: List<BillItem> = emptyList(),
    val shares: Map<String, Double> = emptyMap(), // MemberId -> Amount
    val paidMemberIds: List<String> = emptyList(),
    val date: Long = System.currentTimeMillis(),
) {
    fun paymentStatusFor(memberId: String): PaymentStatus {
        return when {
            memberId == payerId -> PaymentStatus.PAYER
            memberId in paidMemberIds -> PaymentStatus.PAID
            else -> PaymentStatus.UNPAID
        }
    }

    val status: BillStatus
        get() {
            val debtors = shares.keys.filter { it != payerId }
            return if (debtors.all { it in paidMemberIds }) BillStatus.SETTLED else BillStatus.OPEN
        }
}

data class Settlement(
    val fromMemberId: String,
    val toMemberId: String,
    val amount: Double,
)
