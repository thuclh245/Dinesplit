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
import com.example.dinesplit.domain.usecase.SplitCalculationEngine
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
    val savedBill: Bill? = null,
    val error: String? = null,
    val isUsingFallbackMembers: Boolean = false
)

private val fallbackBillMembers = listOf(
    Member(id = "me", name = "Ban", initial = "B", isMe = true),
    Member(id = "minh", name = "Minh", initial = "M"),
    Member(id = "thanh_hang", name = "Thanh Hang", initial = "T")
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
            applyMembers(fallbackBillMembers, isFallback = true)
        }
        billItems.add(BillItem(name = "Mon 1", price = 0.0, sharedByMemberIds = emptyList()))
    }

    private fun loadGroupMembers() {
        viewModelScope.launch {
            repository.getGroupMembers(groupId).collect { members ->
                if (members.isEmpty()) {
                    applyMembers(fallbackBillMembers, isFallback = true)
                } else {
                    applyMembers(members, isFallback = false)
                }
            }
        }
    }

    private fun applyMembers(
        members: List<Member>,
        isFallback: Boolean = false
    ) {
        val selectedIds = members.map { it.id }.toSet()
        val payerId = members.firstOrNull { it.isMe }?.id ?: members.firstOrNull()?.id.orEmpty()
        _uiState.update {
            it.copy(
                members = members,
                selectedMemberIds = selectedIds,
                payerId = payerId,
                isUsingFallbackMembers = isFallback
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
        val normalizedAmount = newAmount.onlyDigits()
        _uiState.update { it.copy(totalAmountStr = normalizedAmount, error = null) }
    }

    fun onMethodSelect(method: SplitMethod) {
        _uiState.update { it.copy(selectedMethod = method, error = null) }
    }

    fun addItem() {
        billItems.add(BillItem(name = "Mon ${billItems.size + 1}", price = 0.0, sharedByMemberIds = emptyList()))
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
        customAmounts[memberId] = amount.onlyDigits()
    }

    fun saveBill() {
        viewModelScope.launch {
            saveBillBlocking()
        }
    }

    suspend fun saveBillBlocking(): Result<Unit> {
        val currentState = _uiState.value
        val billName = currentState.billName.trim().ifBlank { "Hoa don moi" }

        validateBillInput(currentState)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return Result.failure(IllegalArgumentException(error))
        }

        val totalAmount = calculateTotalAmount(currentState)
        val shares = calculateShares(totalAmount, currentState).getOrElse { throwable ->
            val message = throwable.message ?: "Khong the tinh tien chia"
            _uiState.update { it.copy(error = message) }
            return Result.failure(IllegalArgumentException(message))
        }
        val bill = Bill(
            groupId = groupId,
            name = billName,
            totalAmount = totalAmount,
            payerId = currentState.payerId,
            method = currentState.selectedMethod,
            items = if (currentState.selectedMethod == SplitMethod.ITEMIZED) billItems.toList() else emptyList(),
            shares = shares,
            paidMemberIds = listOf(currentState.payerId)
        )

        _uiState.update { it.copy(isLoading = true, error = null) }
        val result = repository.saveBill(bill)
        _uiState.update {
            if (result.isSuccess) {
                it.copy(isLoading = false, isSaved = true, savedBill = bill)
            } else {
                it.copy(isLoading = false, error = "Khong the luu hoa don")
            }
        }
        return result
    }

    private fun validateBillInput(state: CreateBillUiState): String? {
        val totalAmount = calculateTotalAmount(state)
        val selectedMembers = state.selectedMemberIds.toList()

        return when {
            groupId.isBlank() -> "Thieu nhom de luu hoa don"
            totalAmount <= 0.0 -> "Tong tien phai lon hon 0"
            state.selectedMemberIds.isEmpty() -> "Can chon it nhat mot nguoi tham gia"
            state.payerId.isBlank() -> "Can chon nguoi thanh toan"
            state.payerId !in state.members.map { it.id } -> "Nguoi thanh toan khong hop le"
            state.selectedMethod == SplitMethod.CUSTOM &&
                customAmounts.keys.any { it !in state.selectedMemberIds } -> "Custom amount chi ap dung cho nguoi duoc chon"
            state.selectedMethod == SplitMethod.CUSTOM &&
                selectedMembers.any { customAmounts[it].isNullOrBlank() } -> "Nhap so tien cho tat ca thanh vien duoc chon"
            state.selectedMethod == SplitMethod.CUSTOM &&
                !SplitCalculationEngine.moneyEquals(customAmountsForSelected(selectedMembers).values.sum(), totalAmount) ->
                "Tong tien tu nhap phai bang tong hoa don"
            state.selectedMethod == SplitMethod.ITEMIZED &&
                billItems.any { it.name.isBlank() || it.price <= 0.0 } -> "Moi mon can co ten va gia hop le"
            state.selectedMethod == SplitMethod.ITEMIZED &&
                billItems.any { it.sharedByMemberIds.isEmpty() } -> "Moi mon can chon nguoi chia"
            state.selectedMethod == SplitMethod.ITEMIZED &&
                billItems.any { item -> item.sharedByMemberIds.any { it !in state.selectedMemberIds } } ->
                "Nguoi chia mon phai nam trong danh sach tham gia"
            else -> null
        }
    }

    private fun calculateTotalAmount(state: CreateBillUiState): Double {
        return when (state.selectedMethod) {
            SplitMethod.ITEMIZED -> billItems.sumOf { it.price }
            SplitMethod.CUSTOM -> state.totalAmountStr.toDoubleOrNull() ?: 0.0
            SplitMethod.EQUAL -> state.totalAmountStr.toDoubleOrNull() ?: 0.0
        }
    }

    private fun calculateShares(
        totalAmount: Double,
        state: CreateBillUiState
    ): Result<Map<String, Double>> {
        val selectedMemberIds = state.selectedMemberIds.toList()
        return when (state.selectedMethod) {
            SplitMethod.EQUAL -> SplitCalculationEngine.calculateEqualShares(
                totalAmount = totalAmount,
                memberIds = selectedMemberIds
            )
            SplitMethod.CUSTOM -> SplitCalculationEngine.calculateCustomShares(
                totalAmount = totalAmount,
                memberIds = selectedMemberIds,
                customAmounts = customAmountsForSelected(selectedMemberIds)
            )
            SplitMethod.ITEMIZED -> SplitCalculationEngine.calculateItemizedShares(
                items = billItems.toList(),
                memberIds = selectedMemberIds
            )
        }
    }

    private fun customAmountsForSelected(memberIds: List<String>): Map<String, Double> {
        return memberIds.associateWith { memberId ->
            customAmounts[memberId]?.toDoubleOrNull() ?: 0.0
        }
    }

    private fun String.onlyDigits(): String {
        return filter { it.isDigit() }
    }
}
