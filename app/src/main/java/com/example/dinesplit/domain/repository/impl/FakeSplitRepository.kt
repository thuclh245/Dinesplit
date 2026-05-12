package com.example.dinesplit.domain.repository.impl

import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSplitRepository : SplitRepository {
    private val _members = MutableStateFlow(
        listOf(
            Member(id = "1", name = "Bạn", initial = "B", isMe = true),
            Member(id = "2", name = "An", initial = "A"),
            Member(id = "3", name = "Bình", initial = "B")
        )
    )

    // last saved bill for assertions in tests
    var lastSavedBill: Bill? = null

    override fun getGroups(): Flow<List<Group>> = MutableStateFlow(emptyList())

    override suspend fun createGroup(group: Group) = Unit

    override suspend fun joinGroup(inviteCode: String) = Unit

    override fun getGroupMembers(groupId: String): Flow<List<Member>> = _members

    override suspend fun saveBill(bill: Bill): Result<Unit> {
        lastSavedBill = bill
        return Result.success(Unit)
    }
    
    fun getCurrentMembers(): List<Member> = _members.value
}
