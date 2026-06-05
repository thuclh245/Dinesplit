package com.example.dinesplit.data.repository

import android.content.Context
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.core.firebase.FirestoreCollections
import com.example.dinesplit.domain.model.QrPayment
import com.example.dinesplit.domain.repository.QrPaymentRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.Date

class FirebaseQrPaymentRepository private constructor(
    private val firestore: FirebaseFirestore = FirebaseProviders.firestore
) : QrPaymentRepository {

    override suspend fun createQrPayment(payment: QrPayment): Result<Unit> = runCatching {
        firestore.collection(FirestoreCollections.QR_PAYMENTS)
            .document(payment.id)
            .set(payment.toFirestoreMap(), SetOptions.merge())
            .awaitFirebase()
    }

    override fun observeQrPayment(paymentId: String): Flow<QrPayment?> = callbackFlow {
        val registration = firestore.collection(FirestoreCollections.QR_PAYMENTS)
            .document(paymentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toQrPayment())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun updateQrPaymentStatus(
        paymentId: String,
        status: String,
        bankTransactionRef: String?
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to System.currentTimeMillis()
        )
        if (bankTransactionRef != null) {
            updates["bankTransactionRef"] = bankTransactionRef
        }
        if (status == "VERIFIED") {
            updates["verifiedAt"] = System.currentTimeMillis()
        }
        firestore.collection(FirestoreCollections.QR_PAYMENTS)
            .document(paymentId)
            .update(updates)
            .awaitFirebase()
    }

    private fun DocumentSnapshot.toQrPayment(): QrPayment? {
        if (!exists()) return null
        return QrPayment(
            id = id,
            groupId = getString("groupId").orEmpty(),
            billId = getString("billId"),
            payerUid = getString("payerUid").orEmpty(),
            receiverUid = getString("receiverUid").orEmpty(),
            amount = getDouble("amount") ?: 0.0,
            bankTransactionRef = getString("bankTransactionRef").orEmpty(),
            qrContent = getString("qrContent").orEmpty(),
            status = getString("status") ?: "PENDING",
            description = getString("description").orEmpty(),
            paymentGateway = getString("paymentGateway").orEmpty(),
            verifiedAt = getLongDateSafe("verifiedAt"),
            createdAt = getLongDateSafe("createdAt")
        )
    }

    private fun QrPayment.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "groupId" to groupId,
            "billId" to billId,
            "payerUid" to payerUid,
            "receiverUid" to receiverUid,
            "amount" to amount,
            "bankTransactionRef" to bankTransactionRef,
            "qrContent" to qrContent,
            "status" to status,
            "description" to description,
            "paymentGateway" to paymentGateway,
            "verifiedAt" to (verifiedAt?.time ?: 0L),
            "createdAt" to (createdAt?.time ?: System.currentTimeMillis())
        )
    }

    private fun DocumentSnapshot.getLongDateSafe(field: String): Date? {
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
        private var INSTANCE: FirebaseQrPaymentRepository? = null

        fun getInstance(context: Context): FirebaseQrPaymentRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseQrPaymentRepository().also { INSTANCE = it }
            }
        }
    }
}
