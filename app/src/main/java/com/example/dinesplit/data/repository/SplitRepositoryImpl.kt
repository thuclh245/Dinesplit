package com.example.dinesplit.data.repository

import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class SplitRepositoryImpl : SplitRepository {
    // Tạm thời dùng In-memory để demo Persistence trong phiên làm việc
    // Sau này sẽ thay thế bằng Room DAO hoặc Firebase Firestore
    private val _bills = MutableStateFlow<List<Bill>>(emptyList())
    private val _members = MutableStateFlow<List<Member>>(
        listOf(
            Member("1", "Bạn", "B", true),
            Member("2", "Minh", "M"),
            Member("3", "Sarah Chen", "S")
        )
    )

    override suspend fun saveBill(bill: Bill): Result<Unit> {
        return try {
            _bills.value = _bills.value + bill
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getBills(groupId: String): Flow<List<Bill>> {
        return _bills.asStateFlow().map { list -> list.filter { it.groupId == groupId } }
    }

    override fun getGroupMembers(groupId: String): Flow<List<Member>> {
        // Tạm thời trả về danh sách mock cho mọi group
        return _members.asStateFlow()
    }
}
