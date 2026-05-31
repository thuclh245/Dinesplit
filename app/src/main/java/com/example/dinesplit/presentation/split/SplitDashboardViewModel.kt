package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplitDashboardRecentBill(
    val bill: Bill,
    val groupName: String
)

data class SplitDashboardUiState(
    val groups: List<Group> = emptyList(),
    val billsByGroup: Map<String, List<Bill>> = emptyMap(),
    val recentBills: List<SplitDashboardRecentBill> = emptyList(),
    val amountYouOwe: Double = 0.0,
    val amountYouAreOwed: Double = 0.0,
    val currentUserId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class SplitDashboardViewModel(
    private val repository: SplitRepository
) : ViewModel() {

    private val currentUserId: String?
        get() = FirebaseProviders.auth.currentUser?.uid

    private val _uiState = MutableStateFlow(SplitDashboardUiState(currentUserId = currentUserId))
    val uiState: StateFlow<SplitDashboardUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeDashboard() {
        viewModelScope.launch {
            runCatching {
                repository.getGroups()
                    .flatMapLatest { groups ->
                        if (groups.isEmpty()) {
                            flowOf(groups to emptyMap())
                        } else {
                            combineBillFlows(groups)
                        }
                    }
                    .collect { (groups, billsByGroup) ->
                        val sortedGroups = groups.sortedByDescending { it.createdAt }
                        val recentBills = sortedGroups
                            .flatMap { group ->
                                billsByGroup[group.id].orEmpty().map { bill ->
                                    SplitDashboardRecentBill(bill = bill, groupName = group.name)
                                }
                            }
                            .sortedByDescending { it.bill.date }
                            .take(5)

                        _uiState.update {
                            it.copy(
                                groups = sortedGroups,
                                billsByGroup = billsByGroup,
                                recentBills = recentBills,
                                amountYouOwe = calculateAmountYouOwe(billsByGroup),
                                amountYouAreOwed = calculateAmountYouAreOwed(billsByGroup),
                                currentUserId = currentUserId,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Không thể tải dữ liệu chia tiền"
                    )
                }
            }
        }
    }

    private fun combineBillFlows(groups: List<Group>): Flow<Pair<List<Group>, Map<String, List<Bill>>>> {
        val billFlows = groups.map { group ->
            repository.getBills(group.id).map { bills -> group.id to bills }
        }

        return combine(billFlows) { billPairs ->
            groups to billPairs.toMap()
        }
    }

    private fun calculateAmountYouOwe(billsByGroup: Map<String, List<Bill>>): Double {
        val userId = currentUserId ?: return 0.0
        return billsByGroup.values.flatten().sumOf { bill ->
            if (bill.payerId == userId || bill.paidMemberIds.contains(userId)) {
                0.0
            } else {
                bill.shares[userId] ?: 0.0
            }
        }
    }

    private fun calculateAmountYouAreOwed(billsByGroup: Map<String, List<Bill>>): Double {
        val userId = currentUserId ?: return 0.0
        return billsByGroup.values.flatten().sumOf { bill ->
            if (bill.payerId != userId) {
                0.0
            } else {
                bill.shares
                    .filterKeys { memberId -> memberId != userId && !bill.paidMemberIds.contains(memberId) }
                    .values
                    .sum()
            }
        }
    }
}
