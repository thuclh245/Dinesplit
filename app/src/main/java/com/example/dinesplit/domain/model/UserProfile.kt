package com.example.dinesplit.domain.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val username: String,
    val email: String,
    val avatarUrl: String = "",
    val bio: String = "",
    val createdAt: Long,
    val updatedAt: Long
) {
    fun isComplete(): Boolean {
        return displayName.isNotBlank() && username.isNotBlank()
    }
}

