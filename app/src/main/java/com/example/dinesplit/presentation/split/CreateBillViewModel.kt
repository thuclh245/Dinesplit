package com.example.dinesplit.presentation.split

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.*
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CreateBillUiState(
    val billName: String = "",
    val totalAmountStr: String = "",
    val selectedMethod: SplitMethod = SplitMethod.EQUAL,
    val members: List<Member> = emptyList(),
    val selectedMemberIds: Set<String> = emptySet(),
    val payerId: String = "1", // Mặc định là Bạn
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class CreateBillViewModel(
    private val repository: SplitRepository,
    private val groupId: String,
    private val autoLoadMembers: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateBillUiState())
    val uiState: StateFlow<CreateBillUiState> = _uiState.asStateFlow()

    // Bill items and custom amounts are kept in observable state lists/maps for reactive UI
    val billItems = mutableStateListOf<BillItem>()
    val customAmounts = mutableStateMapOf<String, String>()

    init {
        if (autoLoadMembers) loadGroupMembers()
        // Khởi tạo món ăn đầu tiên
        billItems.add(BillItem(name = "Món 1", price = 0.0, sharedByMemberIds = emptyList()))
    }

    private fun loadGroupMembers() {
        viewModelScope.launch {
            repository.getGroupMembers(groupId).collect { members ->
                val ids = members.map { it.id }.toSet()
                _uiState.update { it.copy(members = members, selectedMemberIds = ids) }
            }
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

    // Test helper: set members directly
    fun setMembersForTest(members: List<Member>) {
        _uiState.update {
            it.copy(
                members = members,
                selectedMemberIds = members.map { member -> member.id }.toSet()
            )
        }
    }

    fun onBillNameChange(newName: String) {
        _uiState.update { it.copy(billName = newName) }
    }

    fun onTotalAmountChange(newAmount: String) {
        if (newAmount.all { it.isDigit() }) {
            _uiState.update { it.copy(totalAmountStr = newAmount) }
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

    // Synchronous suspendable save function for tests
    suspend fun saveBillBlocking(): Result<Unit> {
        val currentState = _uiState.value
        val totalAmount = if (currentState.selectedMethod == SplitMethod.ITEMIZED) {
            billItems.sumOf { it.price }
        } else {
            currentState.totalAmountStr.toDoubleOrNull() ?: 0.0
        }

        val shares = calculateShares(totalAmount, currentState.selectedMethod)

        val bill = Bill(
            groupId = groupId,
            name = currentState.billName,
            totalAmount = totalAmount,
            payerId = currentState.payerId,
            method = currentState.selectedMethod,
            items = if (currentState.selectedMethod == SplitMethod.ITEMIZED) billItems.toList() else emptyList(),
            shares = shares
        )

        _uiState.update { it.copy(isLoading = true) }
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
                        item.sharedByMemberIds.forEach { mid ->
                            shares[mid] = (shares[mid] ?: 0.0) + perPerson
                        }
                    }
                }
            }
        }
        return shares
    }
}
