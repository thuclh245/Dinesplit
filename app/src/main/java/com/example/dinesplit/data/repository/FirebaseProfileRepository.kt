package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ProfileRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseProfileRepository private constructor(
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
            val normalizedUsername = normalizeUsername(profile.username)
            ensureUsernameAvailable(profile.uid, normalizedUsername)

            firestore
                .collection(COLLECTION_USERS)
                .document(profile.uid)
                .set(
                    profile.toFirestoreMap(normalizedUsername),
                    SetOptions.merge()
                )
                .awaitFirebase()
            return@runCatching
        }
    }

    private fun UserProfile.toFirestoreMap(normalizedUsername: String): Map<String, Any> {
        return mapOf(
            FIELD_UID to uid,
            FIELD_DISPLAY_NAME to displayName,
            FIELD_USERNAME to displayNameUsernameSafe(username),
            FIELD_USERNAME_LOWER to normalizedUsername,
            FIELD_EMAIL to email,
            FIELD_AVATAR_URL to avatarUrl,
            FIELD_BIO to bio,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to updatedAt
        )
    }

    private fun displayNameUsernameSafe(value: String): String {
        return value.trim()
    }

    private suspend fun ensureUsernameAvailable(currentUid: String, normalizedUsername: String) {
        val query = firestore
            .collection(COLLECTION_USERS)
            .whereEqualTo(FIELD_USERNAME_LOWER, normalizedUsername)
            .get()
            .awaitFirebase()

        val conflictExists = query.documents.any { it.id != currentUid }
        if (conflictExists) {
            throw UsernameAlreadyExistsException()
        }
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

    private fun normalizeUsername(value: String): String {
        return value.trim().lowercase()
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
        private const val FIELD_USERNAME_LOWER = "usernameLower"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_AVATAR_URL = "avatarUrl"
        private const val FIELD_BIO = "bio"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"

        @Volatile
        private var INSTANCE: FirebaseProfileRepository? = null

        fun getInstance(context: Context): FirebaseProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseProfileRepository(context = context).also { INSTANCE = it }
            }
        }
    }
}


