package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ProfileRepository

class LocalProfileRepository private constructor(
    context: Context
) : ProfileRepository {

    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override suspend fun getProfile(uid: String): UserProfile? {
        val displayName = prefs.getString(key(uid, KEY_DISPLAY_NAME), null) ?: return null
        val username = prefs.getString(key(uid, KEY_USERNAME), null) ?: return null
        val email = prefs.getString(key(uid, KEY_EMAIL), null) ?: return null
        val avatarUrl = prefs.getString(key(uid, KEY_AVATAR_URL), "").orEmpty()
        val bio = prefs.getString(key(uid, KEY_BIO), "").orEmpty()
        val createdAt = prefs.getLong(key(uid, KEY_CREATED_AT), 0L)
        val updatedAt = prefs.getLong(key(uid, KEY_UPDATED_AT), createdAt)

        return UserProfile(
            uid = uid,
            displayName = displayName,
            username = username,
            email = email,
            avatarUrl = avatarUrl,
            bio = bio,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    override suspend fun upsertProfile(profile: UserProfile): Result<Unit> {
        prefs.edit()
            .putString(key(profile.uid, KEY_DISPLAY_NAME), profile.displayName)
            .putString(key(profile.uid, KEY_USERNAME), profile.username)
            .putString(key(profile.uid, KEY_EMAIL), profile.email)
            .putString(key(profile.uid, KEY_AVATAR_URL), profile.avatarUrl)
            .putString(key(profile.uid, KEY_BIO), profile.bio)
            .putLong(key(profile.uid, KEY_CREATED_AT), profile.createdAt)
            .putLong(key(profile.uid, KEY_UPDATED_AT), profile.updatedAt)
            .apply()
        return Result.success(Unit)
    }

    private fun key(uid: String, field: String): String = "profile_${uid}_$field"

    companion object {
        private const val PREF_NAME = "dinesplit_profile"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_BIO = "bio"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_UPDATED_AT = "updated_at"

        @Volatile
        private var INSTANCE: LocalProfileRepository? = null

        fun getInstance(context: Context): LocalProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalProfileRepository(context = context).also { INSTANCE = it }
            }
        }
    }
}

