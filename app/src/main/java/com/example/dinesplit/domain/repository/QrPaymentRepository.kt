package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.QrPayment
import kotlinx.coroutines.flow.Flow

interface QrPaymentRepository {
    suspend fun createQrPayment(payment: QrPayment): Result<Unit>
    fun observeQrPayment(paymentId: String): Flow<QrPayment?>
    fun observeBillPayments(groupId: String, billId: String): Flow<List<QrPayment>>
    suspend fun updateQrPaymentStatus(paymentId: String, status: String, bankTransactionRef: String?): Result<Unit>
}
