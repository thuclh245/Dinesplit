package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val group: Group? = null,
    val bills: List<Bill> = emptyList(),
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
                    repository.getBills(groupId)
                ) { group, bills -> group to bills }
                    .collect { (group, bills) ->
                        _uiState.update {
                            it.copy(
                                group = group,
                                bills = bills,
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
}
