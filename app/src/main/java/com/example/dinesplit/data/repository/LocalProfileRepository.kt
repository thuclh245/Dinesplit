package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ProfileRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocalProfileRepository private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context
) : ProfileRepository {

    private val firestore = FirebaseProviders.firestore

    override suspend fun getProfile(uid: String): UserProfile? {
        val snapshot = firestore
            .collection(COLLECTION_USERS)
            .document(uid)
            .get()
            .awaitFirebase()

        return snapshot.toUserProfile(uid)
    }

    override suspend fun upsertProfile(profile: UserProfile): Result<Unit> {
        return runCatching {
            firestore
                .collection(COLLECTION_USERS)
                .document(profile.uid)
                .set(profile.toFirestoreMap(), SetOptions.merge())
                .awaitFirebase()
        }.map { Unit }
    }

    private fun UserProfile.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            FIELD_UID to uid,
            FIELD_DISPLAY_NAME to displayName,
            FIELD_USERNAME to username,
            FIELD_EMAIL to email,
            FIELD_AVATAR_URL to avatarUrl,
            FIELD_BIO to bio,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to updatedAt
        )
    }

    private fun DocumentSnapshot.toUserProfile(uid: String): UserProfile? {
        if (!exists()) return null

        val displayName = getString(FIELD_DISPLAY_NAME) ?: return null
        val username = getString(FIELD_USERNAME) ?: return null
        val email = getString(FIELD_EMAIL) ?: return null
        val avatarUrl = getString(FIELD_AVATAR_URL).orEmpty()
        val bio = getString(FIELD_BIO).orEmpty()
        val createdAt = getLong(FIELD_CREATED_AT) ?: 0L
        val updatedAt = getLong(FIELD_UPDATED_AT) ?: createdAt

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

    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase task failed")
                    )
                }
            }
        }
    }

    companion object {
        private const val COLLECTION_USERS = "user_profiles"
        private const val FIELD_UID = "uid"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_USERNAME = "username"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_AVATAR_URL = "avatarUrl"
        private const val FIELD_BIO = "bio"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"

        @Volatile
        private var INSTANCE: LocalProfileRepository? = null

        fun getInstance(context: Context): LocalProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalProfileRepository(context = context).also { INSTANCE = it }
            }
        }
    }
}

