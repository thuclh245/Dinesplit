package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface SplitRepository {
    suspend fun saveBill(bill: Bill): Result<Unit>
    fun getBills(groupId: String): Flow<List<Bill>>
    fun getGroupMembers(groupId: String): Flow<List<Member>>
}
