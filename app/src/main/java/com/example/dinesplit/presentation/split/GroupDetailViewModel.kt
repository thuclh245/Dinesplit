package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.repository.SplitRepository
import com.example.dinesplit.domain.usecase.SplitCalculationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupMemberBalance(
    val memberId: String,
    val name: String,
    val initial: String,
    val balance: Double,
    val isMe: Boolean
)

data class SettlementSuggestion(
    val fromMemberId: String,
    val fromName: String,
    val toMemberId: String,
    val toName: String,
    val amount: Double,
    val relatedBillId: String? = null
)

data class GroupDetailUiState(
    val group: Group? = null,
    val bills: List<Bill> = emptyList(),
    val members: List<Member> = emptyList(),
    val memberBalances: List<GroupMemberBalance> = emptyList(),
    val settlements: List<SettlementSuggestion> = emptyList(),
    val totalExpense: Double = 0.0,
    val yourBalance: Double = 0.0,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val isLeaving: Boolean = false,
    val isLeft: Boolean = false,
    val currentUserId: String? = null,
    val error: String? = null
) {
    val isCurrentUserOwner: Boolean
        get() = !group?.ownerId.isNullOrBlank() && group?.ownerId == currentUserId
}

class GroupDetailViewModel(
    private val repository: SplitRepository,
    private val groupId: String,
    private val currentUserId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState(currentUserId = currentUserId))
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        observeGroupDetail()
    }

    fun deleteGroup() {
        if (_uiState.value.isDeleting) return

        val userId = currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Bạn cần đăng nhập để xóa nhóm") }
            return
        }

        if (!_uiState.value.isCurrentUserOwner) {
            _uiState.update { it.copy(error = "Chỉ chủ nhóm mới có quyền xóa nhóm") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            val result = repository.deleteGroup(groupId = groupId, userId = userId)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isDeleting = false, isDeleted = true)
                } else {
                    it.copy(
                        isDeleting = false,
                        error = result.exceptionOrNull()?.message ?: "Không thể xóa nhóm"
                    )
                }
            }
        }
    }

    fun leaveGroup() {
        if (_uiState.value.isLeaving) return

        val userId = currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Bạn cần đăng nhập để rời nhóm") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLeaving = true, error = null) }
            val result = repository.leaveGroup(groupId = groupId, userId = userId)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLeaving = false, isLeft = true)
                } else {
                    it.copy(
                        isLeaving = false,
                        error = result.exceptionOrNull()?.message ?: "Không thể rời nhóm"
                    )
                }
            }
        }
    }

    private fun observeGroupDetail() {
        viewModelScope.launch {
            runCatching {
                combine(
                    repository.getGroup(groupId),
                    repository.getBills(groupId),
                    repository.getGroupMembers(groupId)
                ) { group, bills, members ->
                    Triple(group, bills, members)
                }.collect { (group, bills, members) ->
                    val effectiveMembers = buildEffectiveMembers(members, bills)
                    val memberBalances = calculateMemberBalances(bills, effectiveMembers)
                    val settlements = calculateSettlements(memberBalances, bills)
                    val totalExpense = bills.sumOf { it.totalAmount }.takeIf { it > 0.0 }
                        ?: (group?.totalExpense ?: 0.0)
                    val yourBalance = memberBalances.firstOrNull { it.isMe }?.balance ?: 0.0

                    _uiState.update {
                        it.copy(
                            group = group,
                            bills = bills,
                            members = effectiveMembers,
                            memberBalances = memberBalances,
                            settlements = settlements,
                            totalExpense = totalExpense,
                            yourBalance = yourBalance,
                            isLoading = false,
                            error = if (group == null) "Không tìm thấy nhóm" else null
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Không thể tải chi tiết nhóm"
                    )
                }
            }
        }
    }

    private fun buildEffectiveMembers(
        firestoreMembers: List<Member>,
        bills: List<Bill>
    ): List<Member> {
        if (firestoreMembers.isNotEmpty()) {
            return firestoreMembers.map { member ->
                member.copy(isMe = member.id == currentUserId)
            }
        }

        val ids = bills
            .flatMap { bill -> bill.shares.keys + bill.payerId }
            .filter { it.isNotBlank() }
            .distinct()

        return ids.map { id ->
            val name = fallbackMemberName(id)
            Member(
                id = id,
                name = name,
                initial = name.firstOrNull()?.uppercase().orEmpty(),
                isMe = id == currentUserId
            )
        }
    }

    private fun calculateMemberBalances(
        bills: List<Bill>,
        members: List<Member>
    ): List<GroupMemberBalance> {
        val balances = SplitCalculationEngine.calculateBalances(
            bills = bills,
            memberIds = members.map { it.id }
        )

        return balances.map { (memberId, balance) ->
            val member = members.firstOrNull { it.id == memberId }
            val name = member?.name ?: fallbackMemberName(memberId)
            GroupMemberBalance(
                memberId = memberId,
                name = name,
                initial = member?.initial ?: name.firstOrNull()?.uppercase().orEmpty(),
                balance = balance,
                isMe = member?.isMe ?: (memberId == currentUserId)
            )
        }.sortedWith(
            compareByDescending<GroupMemberBalance> { it.isMe }
                .thenBy { it.balance >= 0.0 }
                .thenBy { it.name }
        )
    }

    private fun calculateSettlements(
        balances: List<GroupMemberBalance>,
        bills: List<Bill>
    ): List<SettlementSuggestion> {
        val balanceById = balances.associate { it.memberId to it.balance }
        val memberById = balances.associateBy { it.memberId }

        return SplitCalculationEngine.calculateSettlements(balanceById).map { settlement ->
            val debtor = memberById[settlement.fromMemberId]
            val creditor = memberById[settlement.toMemberId]
            SettlementSuggestion(
                fromMemberId = settlement.fromMemberId,
                fromName = debtor?.name ?: fallbackMemberName(settlement.fromMemberId),
                toMemberId = settlement.toMemberId,
                toName = creditor?.name ?: fallbackMemberName(settlement.toMemberId),
                amount = settlement.amount,
                relatedBillId = findRelatedUnpaidBill(
                    bills = bills,
                    debtorId = settlement.fromMemberId,
                    creditorId = settlement.toMemberId
                )?.id
            )
        }
    }

    private fun findRelatedUnpaidBill(
        bills: List<Bill>,
        debtorId: String,
        creditorId: String
    ): Bill? {
        val unpaidBillsForDebtor = bills
            .filter { bill ->
                debtorId != bill.payerId &&
                    (bill.shares[debtorId] ?: 0.0) > 0.0 &&
                    debtorId !in bill.paidMemberIds
            }
            .sortedByDescending { it.date }

        return unpaidBillsForDebtor.firstOrNull { bill ->
            bill.payerId == creditorId
        } ?: unpaidBillsForDebtor.firstOrNull()
    }

    private fun fallbackMemberName(memberId: String): String {
        if (memberId == currentUserId) return "Bạn"

        return when (memberId) {
            "me" -> "Bạn"
            "minh" -> "Minh"
            "thanh_hang" -> "Thanh Hằng"
            "tuan_anh" -> "Tuấn Anh"
            else -> memberId
        }
    }
}
