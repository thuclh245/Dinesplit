package com.example.dinesplit.domain.model

import java.util.Date

data class BillSplit(
    val uid: String = "",
    val displayName: String = "",
    val amount: Double = 0.0,
    val isPaid: Boolean = false,
    val paidAt: Date? = null,
    val paidVia: String? = null // "manual" | "qr_transfer"
)
