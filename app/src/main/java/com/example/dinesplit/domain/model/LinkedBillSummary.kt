package com.example.dinesplit.domain.model

data class LinkedBillSummary(
    val billId: String,
    val groupId: String,
    val billName: String,
    val totalAmount: Double,
    val isSettled: Boolean,
    val myShare: Double,
    val isMyPaid: Boolean,
    val isIPayer: Boolean,
)
