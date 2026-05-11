package com.example.dinesplit.data.repository

import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.repository.SplitRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseSplitRepository(
    private val firestore: FirebaseFirestore
) : SplitRepository {

    override fun getGroups(): Flow<List<Group>> = callbackFlow {
        val subscription = firestore.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.mapNotNull { doc ->
                    // Map Firestore document to Group model
                    null // Placeholder
                } ?: emptyList()
                trySend(groups)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createGroup(group: Group) {
        firestore.collection("groups").document(group.id).set(group).await()
    }

    override suspend fun joinGroup(inviteCode: String) {
        // Implementation for joining group via invite code
    }
}
