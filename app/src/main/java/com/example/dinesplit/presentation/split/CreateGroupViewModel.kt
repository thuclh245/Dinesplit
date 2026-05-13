package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.repository.SplitRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val currentUserMember = Member(
    id = "me",
    name = "Bạn",
    initial = "B",
    isMe = true
)

private val createGroupMemberDirectory = listOf(
    currentUserMember,
    Member(id = "minh", name = "Minh", initial = "M"),
    Member(id = "thanh_hang", name = "Thanh Hằng", initial = "T"),
    Member(id = "tuan_anh", name = "Tuấn Anh", initial = "A")
)

data class CreateGroupUiState(
    val groupName: String = "",
    val selectedCategory: String = "Du lịch",
    val selectedMemberIds: Set<String> = setOf("minh", "thanh_hang"),
    val isLoading: Boolean = false,
    val isCreated: Boolean = false,
    val error: String? = null
)

class CreateGroupViewModel(
    private val repository: SplitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    fun onGroupNameChange(value: String) {
        _uiState.update { it.copy(groupName = value, error = null) }
    }

    fun onCategorySelected(value: String) {
        _uiState.update { it.copy(selectedCategory = value) }
    }

    fun onMemberToggled(memberId: String) {
        _uiState.update { state ->
            val selectedIds = state.selectedMemberIds.toMutableSet()
            if (selectedIds.contains(memberId)) {
                selectedIds.remove(memberId)
            } else {
                selectedIds.add(memberId)
            }
            state.copy(selectedMemberIds = selectedIds)
        }
    }

    fun createGroup() {
        val currentState = _uiState.value
        val trimmedName = currentState.groupName.trim()

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(error = "Tên nhóm không được để trống") }
            return
        }

        if (currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                val now = System.currentTimeMillis()
                val members = buildSelectedMembers(currentState.selectedMemberIds)
                val group = Group(
                    id = UUID.randomUUID().toString(),
                    name = trimmedName,
                    imageUrl = null,
                    memberCount = members.size,
                    totalExpense = 0.0,
                    yourBalance = 0.0,
                    createdAt = now
                )

                repository.createGroup(group, members)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, isCreated = true) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Không thể tạo nhóm"
                    )
                }
            }
        }
    }

    private fun buildSelectedMembers(selectedMemberIds: Set<String>): List<Member> {
        val selectedMembers = createGroupMemberDirectory
            .filter { it.id in selectedMemberIds }

        return (listOf(currentUserMember) + selectedMembers)
            .distinctBy { it.id }
    }
}
