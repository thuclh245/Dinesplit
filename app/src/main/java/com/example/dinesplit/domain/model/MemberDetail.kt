package com.example.dinesplit.domain.model

import java.util.Date

data class MemberDetail(
    val uid: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val role: String = "member", // "admin" | "member"
    val joinedAt: Date? = null,
    val totalOwed: Double = 0.0,
    val totalPaid: Double = 0.0,
)
