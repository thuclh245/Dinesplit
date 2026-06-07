package com.example.dinesplit.domain.model

import java.util.Date

data class Story(
    val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val caption: String = "",
    val imageUrl: String = "",
    val location: String? = null,
    val visibility: String = "public",
    val createdAt: Date? = null,
    val expiresAt: Date? = null,
)
