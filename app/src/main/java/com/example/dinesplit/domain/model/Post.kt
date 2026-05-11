package com.example.dinesplit.domain.model

data class Post(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val location: String?,
    val mainImageUrl: String,
    val dinersCount: Int,
    val likesCount: Int,
    val commentsCount: Int,
    val caption: String,
    val shareAmount: Double,
    val createdAt: Long
)
