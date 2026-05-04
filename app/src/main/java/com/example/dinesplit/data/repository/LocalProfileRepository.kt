package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.data.local.PersonalDatabaseHelper
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ProfileRepository

class LocalProfileRepository private constructor(
    context: Context
) : ProfileRepository {

    private val dbHelper = PersonalDatabaseHelper.getInstance(context)

    override suspend fun getProfile(uid: String): UserProfile? {
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            "users",
            arrayOf("uid", "email", "display_name", "username", "avatar_url", "bio", "created_at", "updated_at"),
            "uid = ?",
            arrayOf(uid),
            null,
            null,
            null
        )

        val profile = if (cursor.moveToFirst()) {
            UserProfile(
                uid = cursor.getString(0),
                email = cursor.getString(1),
                displayName = cursor.getString(2),
                username = cursor.getString(3),
                avatarUrl = cursor.getString(4),
                bio = cursor.getString(5),
                createdAt = cursor.getLong(6),
                updatedAt = cursor.getLong(7)
            )
        } else {
            null
        }
        cursor.close()

        return profile
    }

    override suspend fun upsertProfile(profile: UserProfile): Result<Unit> {
        val db = dbHelper.writableDatabase

        return try {
            val values = android.content.ContentValues().apply {
                put("uid", profile.uid)
                put("email", profile.email)
                put("display_name", profile.displayName)
                put("username", profile.username)
                put("avatar_url", profile.avatarUrl)
                put("bio", profile.bio)
                put("created_at", profile.createdAt)
                put("updated_at", profile.updatedAt)
            }

            // Try update first
            val rowsUpdated = db.update(
                "users",
                values,
                "uid = ?",
                arrayOf(profile.uid)
            )

            // If no rows updated, insert
            if (rowsUpdated == 0) {
                db.insert("users", null, values)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: LocalProfileRepository? = null

        fun getInstance(context: Context): LocalProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalProfileRepository(context = context).also { INSTANCE = it }
            }
        }
    }
}

