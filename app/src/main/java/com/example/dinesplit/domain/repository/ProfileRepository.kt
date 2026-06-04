package com.example.dinesplit.domain.repository

import android.net.Uri
import com.example.dinesplit.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(uid: String): UserProfile?

    suspend fun searchProfiles(
        query: String,
        limit: Long = 20,
    ): Result<List<UserProfile>>

    suspend fun upsertProfile(profile: UserProfile): Result<Unit>

    suspend fun uploadAvatar(
        uid: String,
        avatarUri: Uri,
    ): Result<String>

    suspend fun getRecentSearches(uid: String): Result<List<String>>

    suspend fun saveRecentSearch(uid: String, query: String): Result<Unit>

    suspend fun clearRecentSearches(uid: String): Result<Unit>

    suspend fun followUser(currentUid: String, targetUid: String): Result<Unit>

    suspend fun unfollowUser(currentUid: String, targetUid: String): Result<Unit>

    suspend fun isFollowing(currentUid: String, targetUid: String): Result<Boolean>

    suspend fun getFollowers(uid: String): Result<List<UserProfile>>

    suspend fun getFollowing(uid: String): Result<List<UserProfile>>
}
