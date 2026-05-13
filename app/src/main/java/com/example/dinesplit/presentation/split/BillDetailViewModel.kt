package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillDetailUiState(
    val bill: Bill? = null,
    val members: List<Member> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class BillDetailViewModel(
    private val repository: SplitRepository,
    private val groupId: String,
    private val billId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillDetailUiState())
    val uiState: StateFlow<BillDetailUiState> = _uiState.asStateFlow()

    init {
        observeBillDetail()
    }

    private fun observeBillDetail() {
        viewModelScope.launch {
            runCatching {
                combine(
                    repository.getBill(groupId, billId),
                    repository.getGroupMembers(groupId)
                ) { bill, members -> bill to members }
                    .collect { (bill, members) ->
                        _uiState.update {
                            it.copy(
                                bill = bill,
                                members = members,
                                isLoading = false,
                                error = if (bill == null) "Không tìm thấy hóa đơn" else null
                            )
                        }
                    }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Không thể tải chi tiết hóa đơn"
                    )
                }
            }
        }
    }
}
