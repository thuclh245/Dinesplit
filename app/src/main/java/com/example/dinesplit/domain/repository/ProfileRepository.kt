package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(uid: String): UserProfile?

    suspend fun upsertProfile(profile: UserProfile): Result<Unit>
}

