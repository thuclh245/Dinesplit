package com.example.dinesplit.domain.repository

import android.net.Uri
import com.example.dinesplit.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(uid: String): UserProfile?

    suspend fun upsertProfile(profile: UserProfile): Result<Unit>

    suspend fun uploadAvatar(uid: String, avatarUri: Uri): Result<String>
}

