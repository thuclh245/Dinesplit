package com.example.dinesplit.presentation.split

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillItem
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.SplitMethod
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateBillUiState(
    val billName: String = "",
    val totalAmountStr: String = "",
    val selectedMethod: SplitMethod = SplitMethod.EQUAL,
    val members: List<Member> = emptyList(),
    val selectedMemberIds: Set<String> = emptySet(),
    val payerId: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

private val fallbackBillMembers = listOf(
    Member(id = "me", name = "Bạn", initial = "B", isMe = true),
    Member(id = "minh", name = "Minh", initial = "M"),
    Member(id = "thanh_hang", name = "Thanh Hằng", initial = "T")
)

class CreateBillViewModel(
    private val repository: SplitRepository,
    private val groupId: String,
    private val autoLoadMembers: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateBillUiState())
    val uiState: StateFlow<CreateBillUiState> = _uiState.asStateFlow()

    val billItems = mutableStateListOf<BillItem>()
    val customAmounts = mutableStateMapOf<String, String>()

    init {
        if (autoLoadMembers) {
            loadGroupMembers()
        } else {
            applyMembers(fallbackBillMembers)
        }
        billItems.add(BillItem(name = "Món 1", price = 0.0, sharedByMemberIds = emptyList()))
    }

    private fun loadGroupMembers() {
        viewModelScope.launch {
            repository.getGroupMembers(groupId).collect { members ->
                applyMembers(members.ifEmpty { fallbackBillMembers })
            }
        }
    }

    private fun applyMembers(members: List<Member>) {
        val selectedIds = members.map { it.id }.toSet()
        val payerId = members.firstOrNull { it.isMe }?.id ?: members.firstOrNull()?.id.orEmpty()
        _uiState.update {
            it.copy(
                members = members,
                selectedMemberIds = selectedIds,
                payerId = payerId
            )
        }
    }

    fun setPayer(memberId: String) {
        _uiState.update { it.copy(payerId = memberId) }
    }

    fun toggleMemberSelection(memberId: String) {
        _uiState.update { state ->
            val current = state.selectedMemberIds.toMutableSet()
            if (current.contains(memberId)) current.remove(memberId) else current.add(memberId)
            state.copy(selectedMemberIds = current)
        }
    }

    fun setMembersForTest(members: List<Member>) {
        applyMembers(members)
    }

    fun onBillNameChange(newName: String) {
        _uiState.update { it.copy(billName = newName, error = null) }
    }

    fun onTotalAmountChange(newAmount: String) {
        if (newAmount.all { it.isDigit() }) {
            _uiState.update { it.copy(totalAmountStr = newAmount, error = null) }
        }
    }

    fun onMethodSelect(method: SplitMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    fun addItem() {
        billItems.add(BillItem(name = "Món ${billItems.size + 1}", price = 0.0, sharedByMemberIds = emptyList()))
    }

    fun removeItem(item: BillItem) {
        if (billItems.size > 1) {
            billItems.remove(item)
        }
    }

    fun updateItem(updatedItem: BillItem) {
        val index = billItems.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            billItems[index] = updatedItem
        }
    }

    fun onCustomAmountChange(memberId: String, amount: String) {
        if (amount.all { it.isDigit() }) {
            customAmounts[memberId] = amount
        }
    }

    fun saveBill() {
        viewModelScope.launch {
            saveBillBlocking()
        }
    }

    suspend fun saveBillBlocking(): Result<Unit> {
        val currentState = _uiState.value
        val billName = currentState.billName.trim().ifBlank { "Hóa đơn mới" }

        validateBillInput(currentState, billName)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return Result.failure(IllegalArgumentException(error))
        }

        val totalAmount = calculateTotalAmount(currentState)

        val shares = calculateShares(totalAmount, currentState.selectedMethod)
        val bill = Bill(
            groupId = groupId,
            name = billName,
            totalAmount = totalAmount,
            payerId = currentState.payerId,
            method = currentState.selectedMethod,
            items = if (currentState.selectedMethod == SplitMethod.ITEMIZED) billItems.toList() else emptyList(),
            shares = shares
        )

        _uiState.update { it.copy(isLoading = true, error = null) }
        val result = repository.saveBill(bill)
        _uiState.update {
            if (result.isSuccess) {
                it.copy(isLoading = false, isSaved = true)
            } else {
                it.copy(isLoading = false, error = "Không thể lưu hóa đơn")
            }
        }
        return result
    }

    private fun validateBillInput(state: CreateBillUiState, billName: String): String? {
        val totalAmount = calculateTotalAmount(state)

        return when {
            groupId.isBlank() -> "Thiếu nhóm để lưu hóa đơn"
            totalAmount <= 0.0 -> "Tổng tiền phải lớn hơn 0"
            state.selectedMemberIds.isEmpty() -> "Cần chọn ít nhất một người tham gia"
            state.payerId.isBlank() -> "Cần chọn người thanh toán"
            else -> null
        }
    }

    private fun calculateTotalAmount(state: CreateBillUiState): Double {
        return when (state.selectedMethod) {
            SplitMethod.ITEMIZED -> billItems.sumOf { it.price }
            SplitMethod.CUSTOM -> {
                state.totalAmountStr.toDoubleOrNull()
                    ?: customAmounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
            }
            SplitMethod.EQUAL -> state.totalAmountStr.toDoubleOrNull() ?: 0.0
        }
    }

    private fun calculateShares(totalAmount: Double, method: SplitMethod): Map<String, Double> {
        val shares = mutableMapOf<String, Double>()
        val members = _uiState.value.members

        when (method) {
            SplitMethod.EQUAL -> {
                val selected = _uiState.value.selectedMemberIds
                if (selected.isNotEmpty()) {
                    val share = totalAmount / selected.size
                    selected.forEach { id -> shares[id] = share }
                }
            }
            SplitMethod.CUSTOM -> {
                _uiState.value.selectedMemberIds.forEach { id ->
                    shares[id] = customAmounts[id]?.toDoubleOrNull() ?: 0.0
                }
            }
            SplitMethod.ITEMIZED -> {
                members.forEach { shares[it.id] = 0.0 }
                billItems.forEach { item ->
                    if (item.sharedByMemberIds.isNotEmpty()) {
                        val perPerson = item.price / item.sharedByMemberIds.size
                        item.sharedByMemberIds.forEach { memberId ->
                            shares[memberId] = (shares[memberId] ?: 0.0) + perPerson
                        }
                    }
                }
            }
        }
        return shares
    }
}
