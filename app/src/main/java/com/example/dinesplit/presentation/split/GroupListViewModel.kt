package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ProfileRepository
import com.example.dinesplit.domain.repository.SplitRepository
import com.example.dinesplit.domain.usecase.SplitCalculationEngine
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

data class GroupListUiState(
    val groups: List<Group> = emptyList(),
    val membersByGroup: Map<String, List<Member>> = emptyMap(),
    val amountYouOwe: Double = 0.0,
    val amountYouAreOwed: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

private data class GroupListGroupData(
    val bills: List<Bill>,
    val members: List<Member>
)

class GroupListViewModel(
    private val repository: SplitRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val currentUserId: String?
        get() = FirebaseProviders.auth.currentUser?.uid

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    private val profileCache = mutableMapOf<String, UserProfile?>()

    init {
        observeGroups()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeGroups() {
        viewModelScope.launch {
            runCatching {
                repository.getGroups()
                    .flatMapLatest { groups ->
                        if (groups.isEmpty()) {
                            flowOf(groups to emptyMap<String, GroupListGroupData>())
                        } else {
                            combineGroupFlows(groups)
                        }
                    }
                    .collect { (groups, dataByGroup) ->
                        val userId = currentUserId
                        val billsByGroup = dataByGroup.mapValues { (_, data) -> data.bills }
                        val membersByGroup = enrichMembersByGroup(
                            dataByGroup.mapValues { (_, data) -> data.members }
                        )
                        val groupsWithBalances = groups
                            .map { group ->
                                val bills = billsByGroup[group.id].orEmpty()
                                val balanceSummary = SplitCalculationEngine.calculateUserBalance(
                                    bills = bills,
                                    userId = userId
                                )

                                group.copy(
                                    totalExpense = if (bills.isEmpty()) {
                                        group.totalExpense
                                    } else {
                                        bills.sumOf { bill -> bill.totalAmount }
                                    },
                                    yourBalance = balanceSummary.netBalance
                                )
                            }
                            .sortedByDescending { group -> group.createdAt }

                        val totalBalanceSummary = SplitCalculationEngine.calculateUserBalance(
                            bills = billsByGroup.values.flatten(),
                            userId = userId
                        )

                        _uiState.update {
                            it.copy(
                                groups = groupsWithBalances,
                                membersByGroup = membersByGroup,
                                amountYouOwe = totalBalanceSummary.amountYouOwe,
                                amountYouAreOwed = totalBalanceSummary.amountYouAreOwed,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Không thể tải danh sách nhóm",
                    )
                }
            }
        }
    }

    private fun combineGroupFlows(groups: List<Group>): Flow<Pair<List<Group>, Map<String, GroupListGroupData>>> {
        val groupFlows = groups.map { group ->
            combine(
                repository.getBills(group.id),
                repository.getGroupMembers(group.id)
            ) { bills, members ->
                group.id to GroupListGroupData(
                    bills = bills,
                    members = members
                )
            }
        }

        return combine(groupFlows) { groupPairs ->
            groups to groupPairs.toMap()
        }
    }

    private suspend fun enrichMembersByGroup(
        membersByGroup: Map<String, List<Member>>
    ): Map<String, List<Member>> {
        return membersByGroup.mapValues { (_, members) ->
            members.map { member -> enrichMember(member) }
        }
    }

    private suspend fun enrichMember(member: Member): Member {
        val profile = profileFor(member.id)
        val profileName = profile?.displayName?.takeIf { it.isNotBlank() }
        val displayName = profileName ?: member.name
        val initial = displayName.firstOrNull()?.uppercase().orEmpty()
            .ifBlank { member.initial }

        return member.copy(
            name = displayName,
            initial = initial,
            avatarUrl = member.avatarUrl.ifBlank { profile?.avatarUrl.orEmpty() }
        )
    }

    private suspend fun profileFor(userId: String): UserProfile? {
        if (userId.isBlank()) return null
        if (!profileCache.containsKey(userId)) {
            profileCache[userId] = profileRepository.getProfile(userId)
        }
        return profileCache[userId]
    }
}
