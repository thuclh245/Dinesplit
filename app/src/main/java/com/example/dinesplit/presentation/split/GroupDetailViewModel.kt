package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    val amount: Double
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
    val error: String? = null
)

class GroupDetailViewModel(
    private val repository: SplitRepository,
    private val groupId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        observeGroupDetail()
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
                    val settlements = calculateSettlements(memberBalances)
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
        if (firestoreMembers.isNotEmpty()) return firestoreMembers

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
                isMe = id == "me"
            )
        }
    }

    private fun calculateMemberBalances(
        bills: List<Bill>,
        members: List<Member>
    ): List<GroupMemberBalance> {
        val balances = members.associate { it.id to 0.0 }.toMutableMap()

        bills.forEach { bill ->
            bill.shares.forEach { (memberId, amount) ->
                val isPayer = memberId == bill.payerId
                val isPaid = bill.paidMemberIds.contains(memberId)
                if (!isPayer && !isPaid && amount > 0.0) {
                    balances[memberId] = (balances[memberId] ?: 0.0) - amount
                    balances[bill.payerId] = (balances[bill.payerId] ?: 0.0) + amount
                }
            }
        }

        return balances.map { (memberId, balance) ->
            val member = members.firstOrNull { it.id == memberId }
            val name = member?.name ?: fallbackMemberName(memberId)
            GroupMemberBalance(
                memberId = memberId,
                name = name,
                initial = member?.initial ?: name.firstOrNull()?.uppercase().orEmpty(),
                balance = if (abs(balance) < 0.5) 0.0 else balance,
                isMe = member?.isMe ?: (memberId == "me")
            )
        }.sortedWith(
            compareByDescending<GroupMemberBalance> { it.isMe }
                .thenBy { it.balance >= 0.0 }
                .thenBy { it.name }
        )
    }

    private fun calculateSettlements(
        balances: List<GroupMemberBalance>
    ): List<SettlementSuggestion> {
        val debtors = balances
            .filter { it.balance < -0.5 }
            .map { it to abs(it.balance) }
            .toMutableList()
        val creditors = balances
            .filter { it.balance > 0.5 }
            .map { it to it.balance }
            .toMutableList()
        val suggestions = mutableListOf<SettlementSuggestion>()

        var debtorIndex = 0
        var creditorIndex = 0
        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val (debtor, debtAmount) = debtors[debtorIndex]
            val (creditor, creditAmount) = creditors[creditorIndex]
            val amount = minOf(debtAmount, creditAmount)

            if (amount > 0.5) {
                suggestions += SettlementSuggestion(
                    fromMemberId = debtor.memberId,
                    fromName = debtor.name,
                    toMemberId = creditor.memberId,
                    toName = creditor.name,
                    amount = amount
                )
            }

            debtors[debtorIndex] = debtor to (debtAmount - amount)
            creditors[creditorIndex] = creditor to (creditAmount - amount)

            if (debtors[debtorIndex].second <= 0.5) debtorIndex++
            if (creditors[creditorIndex].second <= 0.5) creditorIndex++
        }

        return suggestions
    }

    private fun fallbackMemberName(memberId: String): String {
        return when (memberId) {
            "me" -> "Bạn"
            "minh" -> "Minh"
            "thanh_hang" -> "Thanh Hằng"
            "tuan_anh" -> "Tuấn Anh"
            else -> memberId
        }
    }
}
