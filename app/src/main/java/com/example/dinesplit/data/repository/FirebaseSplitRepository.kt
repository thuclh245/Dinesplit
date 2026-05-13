package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillItem
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.SplitMethod
import com.example.dinesplit.domain.repository.SplitRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
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

    override fun getGroup(groupId: String): Flow<Group?> = callbackFlow {
        val registration = firestore.collection("groups")
            .document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toGroup())
            }

        awaitClose { registration.remove() }
    }

    override fun getBills(groupId: String): Flow<List<Bill>> = callbackFlow {
        val registration = firestore.collection("groups")
            .document(groupId)
            .collection("bills")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toBills().orEmpty())
            }

        awaitClose { registration.remove() }
    }

    override fun getBill(groupId: String, billId: String): Flow<Bill?> = callbackFlow {
        val registration = firestore.collection("groups")
            .document(groupId)
            .collection("bills")
            .document(billId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toBill())
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
            val groupRef = firestore.collection("groups").document(bill.groupId)
            val billsColl = groupRef.collection("bills")
            val docRef = billsColl.document(bill.id)
            docRef.set(bill.toMap(), SetOptions.merge()).awaitFirebase()
            groupRef.update(
                mapOf(
                    "totalExpense" to FieldValue.increment(bill.totalAmount),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).awaitFirebase()
        }
    }

    private fun QuerySnapshot.toGroups(): List<Group> {
        return documents.mapNotNull { doc -> doc.toGroup() }
    }

    private fun DocumentSnapshot.toGroup(): Group? {
        val name = getString("name") ?: return null
        return Group(
            id = getString("id") ?: id,
            name = name,
            imageUrl = getString("imageUrl"),
            memberCount = getLong("memberCount")?.toInt() ?: 0,
            totalExpense = getDouble("totalExpense") ?: 0.0,
            yourBalance = getDouble("yourBalance") ?: 0.0,
            createdAt = getLong("createdAt") ?: 0L
        )
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

    private fun QuerySnapshot.toBills(): List<Bill> {
        return documents.mapNotNull { doc -> doc.toBill() }
    }

    private fun DocumentSnapshot.toBill(): Bill? {
        val groupId = getString("groupId") ?: reference.parent.parent?.id ?: return null
        val name = getString("name") ?: return null
        val method = runCatching {
            SplitMethod.valueOf(getString("method") ?: SplitMethod.EQUAL.name)
        }.getOrDefault(SplitMethod.EQUAL)

        return Bill(
            id = getString("id") ?: id,
            groupId = groupId,
            name = name,
            totalAmount = getDouble("totalAmount") ?: 0.0,
            payerId = getString("payerId").orEmpty(),
            method = method,
            items = getBillItems(),
            shares = getShares(),
            date = getLong("date") ?: 0L
        )
    }

    private fun DocumentSnapshot.getBillItems(): List<BillItem> {
        @Suppress("UNCHECKED_CAST")
        val rawItems = get("items") as? List<Map<String, Any?>> ?: return emptyList()

        return rawItems.mapNotNull { item ->
            val name = item["name"] as? String ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val sharedBy = item["sharedByMemberIds"] as? List<String> ?: emptyList()

            BillItem(
                id = item["id"] as? String ?: "",
                name = name,
                price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                sharedByMemberIds = sharedBy
            )
        }
    }

    private fun DocumentSnapshot.getShares(): Map<String, Double> {
        @Suppress("UNCHECKED_CAST")
        val rawShares = get("shares") as? Map<String, Any?> ?: return emptyMap()

        return rawShares.mapValues { (_, amount) ->
            (amount as? Number)?.toDouble() ?: 0.0
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
