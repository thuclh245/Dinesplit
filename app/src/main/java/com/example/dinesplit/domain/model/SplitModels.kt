package com.example.dinesplit.domain.model

import java.util.UUID

enum class SplitMethod {
    EQUAL, CUSTOM, ITEMIZED
}

data class Member(
    val id: String,
    val name: String,
    val initial: String,
    val isMe: Boolean = false
)

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val sharedByMemberIds: List<String>
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
    val date: Long = System.currentTimeMillis()
)