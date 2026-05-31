package com.example.dinesplit.domain.model

import java.util.Date

data class Post(
    val id: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val caption: String = "",
    val imageUrls: List<String> = emptyList(),
    val location: String? = null,
    val linkedGroupId: String? = null,
    val linkedBillId: String? = null,
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val visibility: String = "public", // "public" | "followers_only"
    val tags: List<String> = emptyList(),
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
