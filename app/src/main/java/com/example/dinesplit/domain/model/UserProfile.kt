package com.example.dinesplit.domain.model

import java.util.Date

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val diningStyles: List<String> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val savedPostIds: List<String> = emptyList(),
    val fcmToken: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val isPublic: Boolean = true,
    val usernameLower: String = "",
    val followingIds: List<String> = emptyList(),
    val followerIds: List<String> = emptyList(),
) {
    fun isComplete(): Boolean {
        return displayName.isNotBlank() && username.isNotBlank()
    }
}
