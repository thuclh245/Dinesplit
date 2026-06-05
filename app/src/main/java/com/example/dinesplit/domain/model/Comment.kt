package com.example.dinesplit.domain.model

import java.util.Date

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val content: String = "",
    val createdAt: Date? = null,
)
