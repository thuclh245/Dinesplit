package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupListUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class GroupListViewModel(
    private val repository: SplitRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    init {
        observeGroups()
    }

    private fun observeGroups() {
        viewModelScope.launch {
            runCatching {
                repository.getGroups().collect { groups ->
                    _uiState.update {
                        it.copy(
                            groups = groups.sortedByDescending { group -> group.createdAt },
                            isLoading = false,
                            error = null,
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
}
