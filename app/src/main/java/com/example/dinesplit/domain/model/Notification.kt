package com.example.dinesplit.domain.model

import java.util.Date

data class Notification(
    val id: String = "",
    val recipientUid: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val data: Map<String, Any?> = emptyMap(),
    val isRead: Boolean = false,
    val createdAt: Date? = null
)
