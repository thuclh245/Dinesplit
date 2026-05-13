package com.example.dinesplit.domain.repository

import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface SplitRepository {
    fun getGroups(): Flow<List<Group>>
    fun getGroup(groupId: String): Flow<Group?>
    fun getBills(groupId: String): Flow<List<Bill>>
    fun getBill(groupId: String, billId: String): Flow<Bill?>
    suspend fun createGroup(group: Group)
    suspend fun joinGroup(inviteCode: String)
    fun getGroupMembers(groupId: String): Flow<List<Member>>
    suspend fun saveBill(bill: Bill): Result<Unit>
}
