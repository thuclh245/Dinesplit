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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.Normalizer
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseSplitRepository(
    private val firestore: FirebaseFirestore = FirebaseProviders.firestore,
) : SplitRepository {
    override fun getGroups(): Flow<List<Group>> =
        callbackFlow {
            val currentUserId = FirebaseProviders.auth.currentUser?.uid
            if (currentUserId.isNullOrBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val registration =
                firestore.collection("groups")
                    .whereArrayContains("memberIds", currentUserId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.w("FirebaseSplitRepo", "getGroups: ${error.message}")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.toVisibleGroups().orEmpty())
                    }

            awaitClose { registration.remove() }
        }

    override fun getGroup(groupId: String): Flow<Group?> =
        callbackFlow {
            val registration =
                firestore.collection("groups")
                    .document(groupId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.w("FirebaseSplitRepo", "getGroup($groupId): ${error.message}")
                            trySend(null)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.toGroup()?.takeUnless { group -> group.isLegacyDemoSplitGroup() })
                    }

            awaitClose { registration.remove() }
        }

    override fun getBills(groupId: String): Flow<List<Bill>> =
        callbackFlow {
            val registration =
                firestore.collection("groups")
                    .document(groupId)
                    .collection("bills")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.w("FirebaseSplitRepo", "getBills($groupId): ${error.message}")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.toBills().orEmpty())
                    }

            awaitClose { registration.remove() }
        }

    override fun getBill(
        groupId: String,
        billId: String,
    ): Flow<Bill?> =
        callbackFlow {
            val registration =
                firestore.collection("groups")
                    .document(groupId)
                    .collection("bills")
                    .document(billId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.w("FirebaseSplitRepo", "getBill($groupId/$billId): ${error.message}")
                            trySend(null)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.toBill()?.takeUnless { bill -> bill.isLegacyDemoSplitBill() })
                    }

            awaitClose { registration.remove() }
        }

    override suspend fun createGroup(
        group: Group,
        members: List<Member>,
    ) {
        val groupRef = firestore.collection("groups").document(group.id)
        val cleanMembers = members
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
        val batch = firestore.batch()

        batch.set(groupRef, group.toMap(memberIds = cleanMembers.map { it.id }))
        cleanMembers.forEach { member ->
            batch.set(groupRef.collection("members").document(member.id), member.toMap())
        }

        batch.commit().awaitFirebase()
    }

    override suspend fun deleteGroup(
        groupId: String,
        userId: String,
    ): Result<Unit> {
        return runCatching {
            require(userId.isNotBlank()) { "Bạn cần đăng nhập để xóa nhóm" }

            val groupRef = firestore.collection("groups").document(groupId)
            val groupSnapshot = groupRef.get().awaitFirebase()
            val ownerId =
                groupSnapshot.getString("ownerId")
                    ?: groupSnapshot.getStringListField("memberIds").firstOrNull()
            require(ownerId == userId) {
                "Chỉ chủ nhóm mới có quyền xóa nhóm"
            }

            val bills = groupRef.collection("bills").get().awaitFirebase()
            val members = groupRef.collection("members").get().awaitFirebase()
            val batch = firestore.batch()

            bills.documents.forEach { document ->
                batch.delete(document.reference)
            }
            members.documents.forEach { document ->
                batch.delete(document.reference)
            }
            batch.delete(groupRef)
            batch.commit().awaitFirebase()
        }
    }

    override suspend fun leaveGroup(
        groupId: String,
        userId: String,
    ): Result<Unit> {
        return runCatching {
            require(userId.isNotBlank()) { "Bạn cần đăng nhập để rời nhóm" }

            val groupRef = firestore.collection("groups").document(groupId)
            val memberRef = groupRef.collection("members").document(userId)
            val groupSnapshot = groupRef.get().awaitFirebase()
            val currentMemberIds = groupSnapshot.getStringListField("memberIds")
            val remainingMemberIds = currentMemberIds.filter { it != userId }
            val ownerId = groupSnapshot.getString("ownerId")
            val groupUpdates =
                mutableMapOf<String, Any>(
                    "memberIds" to FieldValue.arrayRemove(userId),
                    "leftMemberIds" to FieldValue.arrayUnion(userId),
                    "memberCount" to FieldValue.increment(-1),
                    "updatedAt" to System.currentTimeMillis(),
                )

            if (ownerId == userId) {
                groupUpdates["ownerId"] = remainingMemberIds.firstOrNull().orEmpty()
            }

            val batch = firestore.batch()

            batch.delete(memberRef)
            batch.update(groupRef, groupUpdates)
            batch.commit().awaitFirebase()
        }
    }

    override suspend fun joinGroup(inviteCode: String) {
        // Joining by invite code will be wired once the invite collection is finalized.
    }

    override fun getGroupMembers(groupId: String): Flow<List<Member>> =
        callbackFlow {
            val groupRef = firestore.collection("groups").document(groupId)
            val membersColl = groupRef.collection("members")
            var latestGroupSnapshot: DocumentSnapshot? = null
            var latestMembersSnapshot: QuerySnapshot? = null

            fun emitMembers() {
                val membersSnapshot = latestMembersSnapshot ?: return
                launch {
                    trySend(membersSnapshot.toMembersWithGroupFallback(latestGroupSnapshot))
                }
            }

            val groupRegistration: ListenerRegistration =
                groupRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.w("FirebaseSplitRepo", "getGroupMembers/group($groupId): ${error.message}")
                        return@addSnapshotListener
                    }
                    latestGroupSnapshot = snapshot
                    emitMembers()
                }

            val membersRegistration: ListenerRegistration =
                membersColl.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.w("FirebaseSplitRepo", "getGroupMembers/members($groupId): ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        latestMembersSnapshot = snapshot
                        emitMembers()
                    }
                }

            awaitClose {
                groupRegistration.remove()
                membersRegistration.remove()
            }
        }

    override suspend fun saveBill(bill: Bill): Result<Unit> {
        return runCatching {
            val currentUserId = FirebaseProviders.auth.currentUser?.uid.orEmpty()
            require(currentUserId.isNotBlank()) { "Bạn cần đăng nhập để lưu hóa đơn" }
            require(bill.createdBy.isNotBlank()) { "Thiếu người tạo hóa đơn" }

            val groupRef = firestore.collection("groups").document(bill.groupId)
            val billsColl = groupRef.collection("bills")
            val docRef = billsColl.document(bill.id)

            val groupSnapshot = groupRef.get().awaitFirebase()
            val currentGroupTotal = groupSnapshot.getDouble("totalExpense") ?: 0.0
            val existingBillSnapshot = docRef.get().awaitFirebase()
            val previousAmount =
                if (existingBillSnapshot.exists()) {
                    val existingCreator = existingBillSnapshot.getString("createdBy").orEmpty()
                    require(existingCreator == currentUserId) {
                        "Chỉ người tạo hóa đơn mới có quyền sửa hóa đơn"
                    }
                    existingBillSnapshot.getDouble("totalAmount") ?: 0.0
                } else {
                    require(bill.createdBy == currentUserId) {
                        "Chỉ người đang đăng nhập mới có thể tạo hóa đơn"
                    }
                    0.0
                }
            val updatedGroupTotal = (currentGroupTotal + bill.totalAmount - previousAmount).coerceAtLeast(0.0)
            val now = System.currentTimeMillis()
            val billToSave = bill.copy(updatedAt = now)
            val batch = firestore.batch()

            batch.set(docRef, billToSave.toMap(), SetOptions.merge())
            batch.update(
                groupRef,
                mapOf(
                    "totalExpense" to updatedGroupTotal,
                    "updatedAt" to now,
                ),
            )
            batch.commit().awaitFirebase()
        }
    }

    override suspend fun deleteBill(
        groupId: String,
        billId: String,
        userId: String,
    ): Result<Unit> {
        return runCatching {
            require(userId.isNotBlank()) { "Bạn cần đăng nhập để xóa hóa đơn" }

            val groupRef = firestore.collection("groups").document(groupId)
            val billRef = groupRef.collection("bills").document(billId)
            val billSnapshot = billRef.get().awaitFirebase()
            if (!billSnapshot.exists()) return@runCatching

            val creatorId = billSnapshot.getString("createdBy").orEmpty()
            require(creatorId == userId) {
                "Chỉ người tạo hóa đơn mới có quyền xóa hóa đơn"
            }

            val groupSnapshot = groupRef.get().awaitFirebase()
            val currentGroupTotal = groupSnapshot.getDouble("totalExpense") ?: 0.0
            val billTotal = billSnapshot.getDouble("totalAmount") ?: 0.0
            val now = System.currentTimeMillis()
            val batch = firestore.batch()

            batch.delete(billRef)
            batch.update(
                groupRef,
                mapOf(
                    "totalExpense" to (currentGroupTotal - billTotal).coerceAtLeast(0.0),
                    "updatedAt" to now,
                ),
            )
            batch.commit().awaitFirebase()
        }
    }

    override suspend fun markBillMemberPaid(
        groupId: String,
        billId: String,
        memberId: String,
    ): Result<Unit> {
        return runCatching {
            firestore.collection("groups")
                .document(groupId)
                .collection("bills")
                .document(billId)
                .update(
                    mapOf(
                        "paidMemberIds" to FieldValue.arrayUnion(memberId),
                        "updatedAt" to System.currentTimeMillis(),
                    ),
                )
                .awaitFirebase()
        }
    }

    private fun QuerySnapshot.toVisibleGroups(): List<Group> {
        val currentUserId = FirebaseProviders.auth.currentUser?.uid
        return documents
            .filter { document -> document.isVisibleTo(currentUserId) }
            .mapNotNull { doc -> doc.toGroup() }
            .filterNot { group -> group.isLegacyDemoSplitGroup() }
    }

    private fun DocumentSnapshot.isVisibleTo(currentUserId: String?): Boolean {
        if (currentUserId.isNullOrBlank()) return true

        val memberIds = getStringListField("memberIds")
        val leftMemberIds = getStringListField("leftMemberIds")
        val hasMembershipList = contains("memberIds")

        return currentUserId !in leftMemberIds &&
            (!hasMembershipList || currentUserId in memberIds)
    }

    private fun DocumentSnapshot.getLongDateSafe(field: String): Long? {
        return try {
            getTimestamp(field)?.toDate()?.time
        } catch (e: Exception) {
            try {
                getLong(field)
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun DocumentSnapshot.toGroup(): Group? {
        val name = getString("name") ?: return null
        val createdAtVal = getLongDateSafe("createdAt") ?: 0L
        return Group(
            id = getString("id") ?: id,
            name = name,
            imageUrl = getString("imageUrl"),
            memberCount = getLong("memberCount")?.toInt() ?: 0,
            totalExpense = getDouble("totalExpense") ?: 0.0,
            yourBalance = getDouble("yourBalance") ?: 0.0,
            createdAt = createdAtVal,
            ownerId = getString("ownerId") ?: getStringListField("memberIds").firstOrNull(),
            memberIds = getStringListField("memberIds"),
            leftMemberIds = getStringListField("leftMemberIds"),
            updatedAt = getLongDateSafe("updatedAt") ?: createdAtVal,
        )
    }

    private suspend fun QuerySnapshot.toMembersWithGroupFallback(groupSnapshot: DocumentSnapshot?): List<Member> {
        val currentUserId = FirebaseProviders.auth.currentUser?.uid
        val members = toMembers()
        val knownMemberIds = members.mapTo(mutableSetOf()) { it.id }
        val missingGroupMemberIds =
            groupSnapshot
                ?.getStringListField("memberIds")
                .orEmpty()
                .filter { memberId -> memberId.isNotBlank() && memberId !in knownMemberIds }

        val fallbackMembers =
            missingGroupMemberIds.map { memberId ->
                firestore.collection("users")
                    .document(memberId)
                    .get()
                    .awaitFirebase()
                    .toMemberFromUserProfile(memberId, currentUserId)
                    ?: fallbackMember(memberId, currentUserId)
            }

        return (members + fallbackMembers)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<Member> { it.id == currentUserId }.thenBy { it.name })
    }

    private fun QuerySnapshot.toMembers(): List<Member> {
        val currentUserId = FirebaseProviders.auth.currentUser?.uid
        val rawMembers = documents.mapNotNull { doc ->
            val id = doc.getString("id") ?: doc.id
            val name = doc.getString("name") ?: return@mapNotNull null
            val initial = doc.getString("initial") ?: name.firstOrNull()?.toString().orEmpty()
            val avatarUrl = doc.getString("avatarUrl").orEmpty()
            val isMe = doc.getBoolean("isMe") ?: false
            Member(id = id, name = name, initial = initial, avatarUrl = avatarUrl, isMe = isMe)
        }

        return rawMembers
            .map { member ->
                if (currentUserId.isNullOrBlank()) {
                    member
                } else {
                    member.copy(isMe = member.id == currentUserId)
                }
            }
            .distinctBy { it.id }
    }

    private fun DocumentSnapshot.toMemberFromUserProfile(
        memberId: String,
        currentUserId: String?,
    ): Member? {
        if (!exists()) return null

        val name =
            getString("displayName")?.takeIf { it.isNotBlank() }
                ?: getString("username")?.takeIf { it.isNotBlank() }
                ?: getString("email")?.takeIf { it.isNotBlank() }
                ?: return null

        return Member(
            id = getString("uid")?.takeIf { it.isNotBlank() } ?: memberId,
            name = name,
            initial = name.firstOrNull()?.uppercase().orEmpty(),
            avatarUrl = getString("avatarUrl").orEmpty(),
            isMe = memberId == currentUserId,
        )
    }

    private fun fallbackMember(
        memberId: String,
        currentUserId: String?,
    ): Member {
        val name = if (memberId == currentUserId) "Bạn" else memberId
        return Member(
            id = memberId,
            name = name,
            initial = name.firstOrNull()?.uppercase().orEmpty(),
            isMe = memberId == currentUserId,
        )
    }

    private fun QuerySnapshot.toBills(): List<Bill> {
        return documents
            .mapNotNull { doc -> doc.toBill() }
            .filterNot { bill -> bill.isLegacyDemoSplitBill() }
    }

    private fun Group.isLegacyDemoSplitGroup(): Boolean {
        val normalizedId = id.toDemoKey()
        val normalizedName = name.toDemoKey()

        return LEGACY_DEMO_ID_MARKERS.any { marker -> normalizedId.contains(marker) } ||
            LEGACY_DEMO_GROUP_NAME_MARKERS.any { marker -> normalizedName.contains(marker) } ||
            memberIds.any { memberId -> memberId in LEGACY_DEMO_MEMBER_IDS }
    }

    private fun Bill.isLegacyDemoSplitBill(): Boolean {
        val normalizedId = id.toDemoKey()
        val normalizedGroupId = groupId.toDemoKey()
        val normalizedName = name.toDemoKey()

        return LEGACY_DEMO_ID_MARKERS.any { marker ->
            normalizedId.contains(marker) || normalizedGroupId.contains(marker)
        } ||
            LEGACY_DEMO_BILL_NAME_MARKERS.any { marker -> normalizedName.contains(marker) } ||
            payerId in LEGACY_DEMO_MEMBER_IDS ||
            createdBy in LEGACY_DEMO_MEMBER_IDS ||
            shares.keys.any { memberId -> memberId in LEGACY_DEMO_MEMBER_IDS }
    }

    private fun String.toDemoKey(): String {
        val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(decomposed, "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    private fun DocumentSnapshot.toBill(): Bill? {
        val groupId = getString("groupId") ?: reference.parent.parent?.id ?: return null
        val name = getString("name") ?: return null
        val method =
            runCatching {
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
            paidMemberIds = getPaidMemberIds(),
            createdBy = getString("createdBy").orEmpty(),
            paymentQrBankCode = getString("paymentQrBankCode").orEmpty(),
            paymentQrAccountNumber = getString("paymentQrAccountNumber").orEmpty(),
            paymentQrAccountName = getString("paymentQrAccountName").orEmpty(),
            date = getLongDateSafe("date") ?: 0L,
            updatedAt = getLongDateSafe("updatedAt") ?: getLongDateSafe("date") ?: 0L,
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
                sharedByMemberIds = sharedBy,
                quantity = ((item["quantity"] as? Number)?.toInt() ?: 1).coerceAtLeast(1),
                unitPrice =
                    (item["unitPrice"] as? Number)?.toDouble()
                        ?: (((item["price"] as? Number)?.toDouble() ?: 0.0) /
                            ((item["quantity"] as? Number)?.toInt() ?: 1).coerceAtLeast(1)),
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

    private fun DocumentSnapshot.getPaidMemberIds(): List<String> {
        val paidMemberIds =
            (get("paidMemberIds") as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()

        return paidMemberIds.ifEmpty { listOfNotNull(getString("payerId")) }
    }

    private fun DocumentSnapshot.getStringListField(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.filterIsInstance<String>()
            .orEmpty()
    }

    private fun Group.toMap(memberIds: List<String>): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "imageUrl" to imageUrl,
            "memberCount" to memberIds.size,
            "memberIds" to memberIds,
            "leftMemberIds" to leftMemberIds,
            "ownerId" to ownerId,
            "totalExpense" to totalExpense,
            "yourBalance" to yourBalance,
            "createdAt" to createdAt,
            "updatedAt" to System.currentTimeMillis(),
        )
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
            "paidMemberIds" to paidMemberIds,
            "createdBy" to createdBy,
            "paymentQrBankCode" to paymentQrBankCode,
            "paymentQrAccountNumber" to paymentQrAccountNumber,
            "paymentQrAccountName" to paymentQrAccountName,
            "date" to date,
            "updatedAt" to updatedAt,
        )
    }

    private fun BillItem.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "price" to price,
            "quantity" to quantity,
            "unitPrice" to unitPrice,
            "sharedByMemberIds" to sharedByMemberIds,
        )
    }

    private fun Member.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "initial" to initial,
            "avatarUrl" to avatarUrl,
            "isMe" to isMe
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
        private val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        private val LEGACY_DEMO_ID_MARKERS = listOf("demo", "mock", "seed")
        private val LEGACY_DEMO_MEMBER_IDS =
            setOf("chef_hoang_uid", "foodie_lan_uid", "cafe_huy_uid")
        private val LEGACY_DEMO_GROUP_NAME_MARKERS =
            listOf(
                "hoi an trua dong nghiep",
                "hoi ca phe cuoi tuan",
                "team an nhau sai gon",
            )
        private val LEGACY_DEMO_BILL_NAME_MARKERS =
            listOf(
                "hoa don am thuc",
                "bua bun bo o xuan",
                "tiec nuong bbq cuoi tuan",
            )

        @Volatile
        private var INSTANCE: FirebaseSplitRepository? = null

        fun getInstance(
            @Suppress("UNUSED_PARAMETER") context: Context,
        ): FirebaseSplitRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseSplitRepository().also { INSTANCE = it }
            }
        }
    }
}
