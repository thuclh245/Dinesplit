package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.domain.repository.NotificationRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseNotificationRepository private constructor(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val firestore: FirebaseFirestore = FirebaseProviders.firestore
) : NotificationRepository {

    override suspend fun getNotifications(): List<Notification> {
        val uid = requireCurrentUserId()
        val snapshot = firestore
            .collection(COLLECTION_USER_NOTIFICATIONS)
            .document(uid)
            .collection(COLLECTION_NOTIFICATIONS)
            .orderBy(FIELD_CREATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .awaitFirebase()

        return snapshot.documents.mapNotNull { document ->
            document.toNotification(uid)
        }
    }

    override suspend fun insertNotification(notification: Notification) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_NOTIFICATIONS)
            .document(uid)
            .collection(COLLECTION_NOTIFICATIONS)
            .document(notification.id)
            .set(notification.toFirestoreMap())
            .awaitFirebase()
    }

    override suspend fun markAsRead(notificationId: String) {
        updateReadState(notificationId = notificationId, isRead = true)
    }

    override suspend fun markAsUnread(notificationId: String) {
        updateReadState(notificationId = notificationId, isRead = false)
    }

    private suspend fun updateReadState(notificationId: String, isRead: Boolean) {
        val uid = requireCurrentUserId()
        firestore
            .collection(COLLECTION_USER_NOTIFICATIONS)
            .document(uid)
            .collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update(
                mapOf(
                    FIELD_IS_READ to isRead,
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                )
            )
            .awaitFirebase()
    }

    private fun requireCurrentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in to use notifications")
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toNotification(uid: String): Notification? {
        val type = getString(FIELD_TYPE)?.let { value ->
            NotificationType.entries.firstOrNull { it.name == value }
        } ?: return null

        return Notification(
            id = getString(FIELD_ID) ?: id,
            userId = getString(FIELD_USER_ID) ?: uid,
            title = getString(FIELD_TITLE) ?: return null,
            subtitle = getString(FIELD_SUBTITLE) ?: return null,
            type = type,
            relatedId = getString(FIELD_RELATED_ID)?.takeIf { it.isNotBlank() },
            isRead = getBoolean(FIELD_IS_READ) ?: false,
            createdAt = getLong(FIELD_CREATED_AT) ?: return null,
            updatedAt = getLong(FIELD_UPDATED_AT) ?: System.currentTimeMillis(),
            deepLinkDestination = getString(FIELD_DEEP_LINK_DESTINATION)?.takeIf { it.isNotBlank() },
            deepLinkTargetId = getString(FIELD_DEEP_LINK_TARGET_ID)?.takeIf { it.isNotBlank() }
        )
    }

    private fun Notification.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            FIELD_ID to id,
            FIELD_USER_ID to userId,
            FIELD_TITLE to title,
            FIELD_SUBTITLE to subtitle,
            FIELD_TYPE to type.name,
            FIELD_RELATED_ID to relatedId.orEmpty(),
            FIELD_IS_READ to isRead,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to updatedAt,
            FIELD_DEEP_LINK_DESTINATION to deepLinkDestination.orEmpty(),
            FIELD_DEEP_LINK_TARGET_ID to deepLinkTargetId.orEmpty()
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
        private const val COLLECTION_USER_NOTIFICATIONS = "user_notifications"
        private const val COLLECTION_NOTIFICATIONS = "notifications"

        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TITLE = "title"
        private const val FIELD_SUBTITLE = "subtitle"
        private const val FIELD_TYPE = "type"
        private const val FIELD_RELATED_ID = "relatedId"
        private const val FIELD_IS_READ = "isRead"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_DEEP_LINK_DESTINATION = "deepLinkDestination"
        private const val FIELD_DEEP_LINK_TARGET_ID = "deepLinkTargetId"

        @Volatile
        private var INSTANCE: FirebaseNotificationRepository? = null

        fun getInstance(context: Context): FirebaseNotificationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseNotificationRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
