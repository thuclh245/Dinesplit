package com.example.dinesplit.data.repository

import com.example.dinesplit.core.firebase.FirestoreCollections
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepository(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    override fun getNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        val subscription = firestore.collection(FirestoreCollections.NOTIFICATIONS)
            .whereEqualTo("recipientUid", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents?.mapNotNull { it.toObject(Notification::class.java)?.copy(id = it.id) } ?: emptyList()
                trySend(notifications)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun markAsRead(notificationId: String) {
        firestore.collection(FirestoreCollections.NOTIFICATIONS)
            .document(notificationId)
            .update("isRead", true)
            .await()
    }

    override suspend fun deleteNotification(notificationId: String) {
        firestore.collection(FirestoreCollections.NOTIFICATIONS)
            .document(notificationId)
            .delete()
            .await()
    }
}
