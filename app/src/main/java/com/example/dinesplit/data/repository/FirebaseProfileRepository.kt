package com.example.dinesplit.data.repository

import android.content.Context
import android.net.Uri
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
    private val storage = FirebaseProviders.storage

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

            firestore.runTransaction { transaction ->
                val profileRef = profileDocument(profile.uid)
                val claimRef = usernameClaimDocument(normalizedUsername)
                val currentUsernameLower = transaction.get(profileRef).profileUsernameLower()

                ensureClaimAvailable(transaction, claimRef, profile.uid)
                releaseOldUsernameClaimIfChanged(transaction, profile.uid, currentUsernameLower, normalizedUsername)
                upsertUsernameClaim(transaction, claimRef, profile, normalizedUsername)
                upsertProfileDocument(transaction, profileRef, profile, normalizedUsername)
            }.awaitFirebase()
        }
    }

    override suspend fun uploadAvatar(uid: String, avatarUri: Uri): Result<String> {
        return runCatching {
            val avatarReference = avatarDocument(uid)
            avatarReference.putFile(avatarUri).awaitFirebase()
            avatarReference.downloadUrl.awaitFirebase().toString()
        }
    }

    private fun profileDocument(uid: String) = firestore.collection(COLLECTION_USERS).document(uid)

    private fun usernameClaimDocument(usernameLower: String) =
        firestore.collection(COLLECTION_USERNAME_CLAIMS).document(usernameLower)

    private fun avatarDocument(uid: String) =
        storage.reference.child("avatars/$uid/profile_avatar.jpg")

    private fun ensureClaimAvailable(
        transaction: com.google.firebase.firestore.Transaction,
        claimRef: com.google.firebase.firestore.DocumentReference,
        currentUid: String
    ) {
        val claimSnapshot = transaction.get(claimRef)
        if (claimSnapshot.exists() && claimSnapshot.getString(FIELD_UID) != currentUid) {
            throw UsernameAlreadyExistsException()
        }
    }

    private fun releaseOldUsernameClaimIfChanged(
        transaction: com.google.firebase.firestore.Transaction,
        currentUid: String,
        currentUsernameLower: String?,
        normalizedUsername: String
    ) {
        if (currentUsernameLower.isNullOrBlank() || currentUsernameLower == normalizedUsername) return

        val oldClaimRef = usernameClaimDocument(currentUsernameLower)
        val oldClaimSnapshot = transaction.get(oldClaimRef)
        if (oldClaimSnapshot.exists() && oldClaimSnapshot.getString(FIELD_UID) == currentUid) {
            transaction.delete(oldClaimRef)
        }
    }

    private fun upsertUsernameClaim(
        transaction: com.google.firebase.firestore.Transaction,
        claimRef: com.google.firebase.firestore.DocumentReference,
        profile: UserProfile,
        normalizedUsername: String
    ) {
        transaction.set(
            claimRef,
            profile.toUsernameClaimMap(normalizedUsername),
            SetOptions.merge()
        )
    }

    private fun upsertProfileDocument(
        transaction: com.google.firebase.firestore.Transaction,
        profileRef: com.google.firebase.firestore.DocumentReference,
        profile: UserProfile,
        normalizedUsername: String
    ) {
        transaction.set(
            profileRef,
            profile.toFirestoreMap(normalizedUsername),
            SetOptions.merge()
        )
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

    private fun UserProfile.toUsernameClaimMap(normalizedUsername: String): Map<String, Any> {
        val now = System.currentTimeMillis()
        return mapOf(
            FIELD_UID to uid,
            FIELD_USERNAME to displayNameUsernameSafe(username),
            FIELD_USERNAME_LOWER to normalizedUsername,
            FIELD_CLAIMED_AT to now,
            FIELD_UPDATED_AT to now
        )
    }

    private fun displayNameUsernameSafe(value: String): String {
        return value.trim()
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

    private fun DocumentSnapshot.profileUsernameLower(): String? {
        return getString(FIELD_USERNAME_LOWER)?.takeIf { it.isNotBlank() }
            ?: getString(FIELD_USERNAME)?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
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
        private const val COLLECTION_USERNAME_CLAIMS = "username_claims"
        private const val FIELD_UID = "uid"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_USERNAME = "username"
        private const val FIELD_USERNAME_LOWER = "usernameLower"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_AVATAR_URL = "avatarUrl"
        private const val FIELD_BIO = "bio"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_CLAIMED_AT = "claimedAt"

        @Volatile
        private var INSTANCE: FirebaseProfileRepository? = null

        fun getInstance(context: Context): FirebaseProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseProfileRepository(context = context).also { INSTANCE = it }
            }
        }
    }
}


