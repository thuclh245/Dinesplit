package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillItem
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.repository.SplitRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseSplitRepository(
    private val firestore: FirebaseFirestore = FirebaseProviders.firestore
) : SplitRepository {

    override fun getGroups(): Flow<List<Group>> = callbackFlow {
        val registration = firestore.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toGroups().orEmpty())
            }

        awaitClose { registration.remove() }
    }

    override suspend fun createGroup(group: Group) {
        firestore.collection("groups").document(group.id).set(group).await()
    }

    override suspend fun joinGroup(inviteCode: String) {
        // Joining by invite code will be wired once the invite collection is finalized.
    }

    override fun getGroupMembers(groupId: String): Flow<List<Member>> = callbackFlow {
        val membersColl = firestore.collection("groups").document(groupId).collection("members")
        val registration: ListenerRegistration = membersColl.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toMembers())
            }
        }

        awaitClose { registration.remove() }
    }

    override suspend fun saveBill(bill: Bill): Result<Unit> {
        return runCatching {
            val billsColl = firestore.collection("groups").document(bill.groupId).collection("bills")
            val docRef = billsColl.document(bill.id)
            docRef.set(bill.toMap(), SetOptions.merge()).awaitFirebase()
        }
    }

    private fun QuerySnapshot.toGroups(): List<Group> {
        return documents.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            Group(
                id = doc.getString("id") ?: doc.id,
                name = name,
                imageUrl = doc.getString("imageUrl"),
                memberCount = doc.getLong("memberCount")?.toInt() ?: 0,
                totalExpense = doc.getDouble("totalExpense") ?: 0.0,
                yourBalance = doc.getDouble("yourBalance") ?: 0.0,
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        }
    }

    private fun QuerySnapshot.toMembers(): List<Member> {
        return documents.mapNotNull { doc ->
            val id = doc.getString("id") ?: doc.id
            val name = doc.getString("name") ?: return@mapNotNull null
            val initial = doc.getString("initial") ?: name.firstOrNull()?.toString().orEmpty()
            val isMe = doc.getBoolean("isMe") ?: false
            Member(id = id, name = name, initial = initial, isMe = isMe)
        }
    }

    private fun Bill.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "groupId" to groupId,
            "name" to name,
            "totalAmount" to totalAmount,
            "payerId" to payerId,
            "method" to method.name,
            "items" to items.map { it.toMap() },
            "shares" to shares,
            "date" to date
        )
    }

    private fun BillItem.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "price" to price,
            "sharedByMemberIds" to sharedByMemberIds
        )
    }

    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(task.exception ?: IllegalStateException("Firebase task failed"))
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseSplitRepository? = null

        fun getInstance(@Suppress("UNUSED_PARAMETER") context: Context): FirebaseSplitRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseSplitRepository().also { INSTANCE = it }
            }
        }
    }
}
