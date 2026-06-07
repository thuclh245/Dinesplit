
package com.example.dinesplit.data.repository

import android.content.Context
import android.net.Uri
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.core.firebase.FirestoreCollections
import com.example.dinesplit.domain.exception.UsernameAlreadyExistsException
import com.example.dinesplit.domain.model.NotificationDestination
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ProfileRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseProfileRepository private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
) : ProfileRepository {
    private val firestore = FirebaseProviders.firestore
    private val storage = FirebaseProviders.storage

    override suspend fun getProfile(uid: String): UserProfile? {
        return try {
            val snapshot =
                firestore
                    .collection(COLLECTION_USERS)
                    .document(uid)
                    .get()
                    .awaitFirebase()

            if (!snapshot.exists()) {
                return null
            }
            snapshot.toUserProfile(uid)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseProfileRepo", "Error fetching profile", e)
            throw e
        }
    }

    override suspend fun getProfileByUsername(username: String): UserProfile? {
        return try {
            val normalizedUsername = normalizeUsername(username)
            val claimSnapshot = firestore.collection(COLLECTION_USERNAME_CLAIMS)
                .document(normalizedUsername)
                .get()
                .awaitFirebase()

            if (!claimSnapshot.exists()) {
                return null
            }
            val uid = claimSnapshot.getString(FIELD_UID) ?: return null
            getProfile(uid)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseProfileRepo", "Error fetching profile by username", e)
            throw e
        }
    }

    override suspend fun searchProfiles(
        query: String,
        limit: Long,
    ): Result<List<UserProfile>> {
        return runCatching {
            val normalizedQuery = normalizeUsername(query)
            val usersRef = firestore.collection(COLLECTION_USERS)

            val snapshot =
                if (normalizedQuery.isBlank()) {
                    usersRef
                        .orderBy(FIELD_UPDATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                        .awaitFirebase()
                } else {
                    usersRef
                        .orderBy(FIELD_USERNAME_LOWER)
                        .startAt(normalizedQuery)
                        .endAt(normalizedQuery + "\uf8ff")
                        .limit(limit)
                        .get()
                        .awaitFirebase()
                }

            snapshot.documents.mapNotNull { document ->
                document.toUserProfile(document.id)
            }
        }
    }

    override suspend fun upsertProfile(profile: UserProfile): Result<Unit> {
        return runCatching {
            val normalizedUsername = normalizeUsername(profile.username)
            val profileRef = profileDocument(profile.uid)
            val claimRef = usernameClaimDocument(normalizedUsername)

            // 1. Check if username is already taken by someone else
            val claimSnapshot = claimRef.get().awaitFirebase()
            if (claimSnapshot.exists() && claimSnapshot.getString(FIELD_UID) != profile.uid) {
                throw UsernameAlreadyExistsException()
            }

            // 2. Get current profile to check if we need to release an old username
            val currentProfileSnapshot = profileRef.get().awaitFirebase()
            val oldUsernameLower = currentProfileSnapshot.profileUsernameLower()

            // Profile and username claim must become visible together for security rules.
            val batch = firestore.batch()
            batch.set(
                profileRef,
                profile.toFirestoreMap(normalizedUsername),
                SetOptions.merge(),
            )
            batch.set(
                claimRef,
                profile.toUsernameClaimMap(normalizedUsername),
                SetOptions.merge(),
            )

            // Release the previous username in the same atomic write.
            if (oldUsernameLower != null && oldUsernameLower != normalizedUsername) {
                batch.delete(usernameClaimDocument(oldUsernameLower))
            }

            batch.commit().awaitFirebase()
        }
    }

    override suspend fun uploadAvatar(
        uid: String,
        avatarUri: Uri,
    ): Result<String> {
        return runCatching {
            val avatarReference = avatarDocument(uid)
            avatarReference.putFile(avatarUri).awaitFirebase()
            avatarReference.downloadUrl.awaitFirebase().toString()
        }
    }

    override suspend fun getRecentSearches(uid: String): Result<List<String>> {
        return runCatching {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("recentSearches")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .awaitFirebase()

            snapshot.documents.mapNotNull { it.getString("query") }
        }
    }

    override suspend fun saveRecentSearch(uid: String, query: String): Result<Unit> {
        return runCatching {
            val searchId = query.trim().lowercase()
            val data = mapOf(
                "query" to query.trim(),
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("recentSearches")
                .document(searchId)
                .set(data)
                .awaitFirebase()
            Unit
        }
    }

    override suspend fun clearRecentSearches(uid: String): Result<Unit> {
        return runCatching {
            val ref = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .collection("recentSearches")

            val snapshot = ref.get().awaitFirebase()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().awaitFirebase()
            Unit
        }
    }

    override suspend fun getFollowers(uid: String): Result<List<UserProfile>> {
        return runCatching {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .whereArrayContains("followingIds", uid)
                .get()
                .awaitFirebase()
            snapshot.documents.mapNotNull { doc ->
                doc.toUserProfile(doc.id)
            }
        }
    }

    override suspend fun getFollowing(uid: String): Result<List<UserProfile>> {
        return runCatching {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .whereArrayContains("followerIds", uid)
                .get()
                .awaitFirebase()
            snapshot.documents.mapNotNull { doc ->
                doc.toUserProfile(doc.id)
            }
        }
    }

    override suspend fun isFollowing(currentUid: String, targetUid: String): Result<Boolean> {
        return runCatching {
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(currentUid)
                .get()
                .awaitFirebase()
            val followingList = (userDoc.get("followingIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            
            if (followingList.contains(targetUid)) {
                true
            } else {
                val subDoc = firestore.collection(COLLECTION_USERS)
                    .document(currentUid)
                    .collection("following")
                    .document(targetUid)
                    .get()
                    .awaitFirebase()
                subDoc.exists()
            }
        }
    }

    override suspend fun followUser(currentUid: String, targetUid: String): Result<Unit> {
        return runCatching {
            val userRef = firestore.collection(COLLECTION_USERS).document(currentUid)
            val targetRef = firestore.collection(COLLECTION_USERS).document(targetUid)
            val followingSubRef = userRef.collection("following").document(targetUid)
            val followersSubRef = targetRef.collection("followers").document(currentUid)

            firestore.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val targetSnap = transaction.get(targetRef)

                val followingIds = (userSnap.get("followingIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val followerIds = (targetSnap.get("followerIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                if (!followingIds.contains(targetUid)) {
                    val newFollowingIds = followingIds + targetUid
                    val currentFollowingCount = userSnap.getLong("followingCount") ?: 0L
                    transaction.update(userRef, "followingIds", newFollowingIds, "followingCount", currentFollowingCount + 1)
                }

                if (!followerIds.contains(currentUid)) {
                    val newFollowerIds = followerIds + currentUid
                    val currentFollowersCount = targetSnap.getLong("followersCount") ?: 0L
                    transaction.update(targetRef, "followerIds", newFollowerIds, "followersCount", currentFollowersCount + 1)
                }

                transaction.set(followingSubRef, mapOf("followedAt" to System.currentTimeMillis()))
                transaction.set(followersSubRef, mapOf("followedAt" to System.currentTimeMillis()))

                val displayNameA = userSnap.getString("displayName") ?: "Ai đó"
                val usernameA = userSnap.getString("username") ?: ""

                if (currentUid != targetUid) {
                    val notificationId = "${System.currentTimeMillis()}_follow_${currentUid}"
                    val notificationRef = firestore.collection("user_notifications")
                        .document(targetUid)
                        .collection("notifications")
                        .document(notificationId)

                    val notificationMap = mapOf(
                        "id" to notificationId,
                        "userId" to targetUid,
                        "title" to "$displayNameA đã bắt đầu theo dõi bạn",
                        "subtitle" to if (usernameA.isNotEmpty()) "@$usernameA" else "",
                        "type" to "ACTIVITY_UPDATE",
                        "relatedId" to currentUid,
                        "isRead" to false,
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis(),
                        "deepLinkDestination" to NotificationDestination.PROFILE.name,
                        "deepLinkTargetId" to currentUid,
                        "senderId" to currentUid,
                    )
                    transaction.set(notificationRef, notificationMap)
                }
            }.awaitFirebase()
            Unit
        }
    }

    override suspend fun unfollowUser(currentUid: String, targetUid: String): Result<Unit> {
        return runCatching {
            val userRef = firestore.collection(COLLECTION_USERS).document(currentUid)
            val targetRef = firestore.collection(COLLECTION_USERS).document(targetUid)
            val followingSubRef = userRef.collection("following").document(targetUid)
            val followersSubRef = targetRef.collection("followers").document(currentUid)

            firestore.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val targetSnap = transaction.get(targetRef)

                val followingIds = (userSnap.get("followingIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val followerIds = (targetSnap.get("followerIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                if (followingIds.contains(targetUid)) {
                    val newFollowingIds = followingIds - targetUid
                    val currentFollowingCount = userSnap.getLong("followingCount") ?: 0L
                    val newCount = maxOf(0L, currentFollowingCount - 1)
                    transaction.update(userRef, "followingIds", newFollowingIds, "followingCount", newCount)
                }

                if (followerIds.contains(currentUid)) {
                    val newFollowerIds = followerIds - currentUid
                    val currentFollowersCount = targetSnap.getLong("followersCount") ?: 0L
                    val newCount = maxOf(0L, currentFollowersCount - 1)
                    transaction.update(targetRef, "followerIds", newFollowerIds, "followersCount", newCount)
                }

                transaction.delete(followingSubRef)
                transaction.delete(followersSubRef)
            }.awaitFirebase()
            Unit
        }
    }

    private fun profileDocument(uid: String) = firestore.collection(COLLECTION_USERS).document(uid)

    private fun usernameClaimDocument(usernameLower: String) = firestore.collection(COLLECTION_USERNAME_CLAIMS).document(usernameLower)

    private fun avatarDocument(uid: String) = storage.reference.child("avatars/$uid/profile_avatar.jpg")

    private fun UserProfile.toFirestoreMap(normalizedUsername: String): Map<String, Any> {
        return mapOf(
            FIELD_UID to uid,
            FIELD_DISPLAY_NAME to displayName,
            FIELD_USERNAME to displayNameUsernameSafe(username),
            FIELD_USERNAME_LOWER to normalizedUsername,
            FIELD_EMAIL to email,
            FIELD_AVATAR_URL to avatarUrl,
            FIELD_BIO to bio,
            FIELD_DINING_STYLES to diningStyles,
            FIELD_FOLLOWERS_COUNT to followersCount,
            FIELD_FOLLOWING_COUNT to followingCount,
            FIELD_POSTS_COUNT to postsCount,
            FIELD_SAVED_POST_IDS to savedPostIds,
            FIELD_FCM_TOKEN to fcmToken,
            FIELD_CREATED_AT to (createdAt?.time ?: 0L),
            FIELD_UPDATED_AT to (updatedAt?.time ?: System.currentTimeMillis()),
            FIELD_IS_PUBLIC to isPublic,
            "followingIds" to followingIds,
            "followerIds" to followerIds,
        )
    }

    private fun UserProfile.toUsernameClaimMap(normalizedUsername: String): Map<String, Any> {
        val now = System.currentTimeMillis()
        return mapOf(
            FIELD_UID to uid,
            FIELD_USERNAME to displayNameUsernameSafe(username),
            FIELD_USERNAME_LOWER to normalizedUsername,
            FIELD_CLAIMED_AT to now,
            FIELD_UPDATED_AT to now,
        )
    }

    private fun displayNameUsernameSafe(value: String): String {
        return value.trim()
    }

    private fun DocumentSnapshot.getDateSafe(field: String): Date? {
        return try {
            getTimestamp(field)?.toDate()
        } catch (e: Exception) {
            try {
                getLong(field)?.let { Date(it) }
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun DocumentSnapshot.toUserProfile(uid: String): UserProfile? {
        if (!exists()) return null

        val displayName = getString(FIELD_DISPLAY_NAME) ?: return null
        val username = getString(FIELD_USERNAME) ?: return null
        val email = getString(FIELD_EMAIL) ?: return null
        val avatarUrl = getString(FIELD_AVATAR_URL).orEmpty()
        val bio = getString(FIELD_BIO).orEmpty()
        val diningStyles = get(FIELD_DINING_STYLES) as? List<*> ?: emptyList<Any>()
        val followersCount = getLong(FIELD_FOLLOWERS_COUNT)?.toInt() ?: 0
        val followingCount = getLong(FIELD_FOLLOWING_COUNT)?.toInt() ?: 0
        val postsCount = getLong(FIELD_POSTS_COUNT)?.toInt() ?: 0
        val savedPostIds = get(FIELD_SAVED_POST_IDS) as? List<*> ?: emptyList<Any>()
        val fcmToken = getString(FIELD_FCM_TOKEN).orEmpty()
        val createdAt = getDateSafe(FIELD_CREATED_AT)
        val updatedAt = getDateSafe(FIELD_UPDATED_AT) ?: createdAt
        val isPublic = getBoolean(FIELD_IS_PUBLIC) ?: true
        val usernameLower = getString(FIELD_USERNAME_LOWER).orEmpty()
        @Suppress("UNCHECKED_CAST")
        val followingIds = get("followingIds") as? List<String> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val followerIds = get("followerIds") as? List<String> ?: emptyList()

        return UserProfile(
            uid = uid,
            displayName = displayName,
            username = username,
            email = email,
            avatarUrl = avatarUrl,
            bio = bio,
            diningStyles = diningStyles.mapNotNull { it?.toString() },
            followersCount = followersCount,
            followingCount = followingCount,
            postsCount = postsCount,
            savedPostIds = savedPostIds.mapNotNull { it?.toString() },
            fcmToken = fcmToken,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isPublic = isPublic,
            usernameLower = usernameLower,
            followingIds = followingIds,
            followerIds = followerIds,
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
                        task.exception ?: IllegalStateException("Firebase task failed"),
                    )
                }
            }
        }
    }

    companion object {
        private const val COLLECTION_USERS = FirestoreCollections.USERS
        private const val COLLECTION_USERNAME_CLAIMS = FirestoreCollections.USERNAMES
        private const val FIELD_UID = "uid"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_USERNAME = "username"
        private const val FIELD_USERNAME_LOWER = "usernameLower"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_AVATAR_URL = "avatarUrl"
        private const val FIELD_BIO = "bio"
        private const val FIELD_DINING_STYLES = "diningStyles"
        private const val FIELD_FOLLOWERS_COUNT = "followersCount"
        private const val FIELD_FOLLOWING_COUNT = "followingCount"
        private const val FIELD_POSTS_COUNT = "postsCount"
        private const val FIELD_SAVED_POST_IDS = "savedPostIds"
        private const val FIELD_FCM_TOKEN = "fcmToken"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_CLAIMED_AT = "claimedAt"
        private const val FIELD_IS_PUBLIC = "isPublic"

        @Volatile
        private var INSTANCE: FirebaseProfileRepository? = null

        fun getInstance(context: Context): FirebaseProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseProfileRepository(context = context).also { INSTANCE = it }
            }
        }
    }
}
