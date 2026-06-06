package com.example.dinesplit.presentation.split

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillItem
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.NotificationFactory
import com.example.dinesplit.domain.model.SplitMethod
import com.example.dinesplit.domain.model.SplitNotificationTrigger
import com.example.dinesplit.domain.model.SplitTriggerType
import com.example.dinesplit.domain.repository.NotificationRepository
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
    val isEditMode: Boolean = false,
    val originalBill: Bill? = null,
    val paymentQrBankCode: String = "",
    val paymentQrAccountNumber: String = "",
    val paymentQrAccountName: String = "",
    val error: String? = null,
    val isUsingFallbackMembers: Boolean = false,
)

private val fallbackBillMembers =
    listOf(
        Member(id = "me", name = "Ban", initial = "B", isMe = true),
        Member(id = "minh", name = "Minh", initial = "M"),
        Member(id = "thanh_hang", name = "Thanh Hang", initial = "T"),
    )

class CreateBillViewModel(
    private val repository: SplitRepository,
    private val groupId: String,
    private val notificationRepository: NotificationRepository? = null,
    private val autoLoadMembers: Boolean = true,
    private val currentUserId: String? = null,
    private val editBillId: String? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateBillUiState())
    val uiState: StateFlow<CreateBillUiState> = _uiState.asStateFlow()

    val billItems = mutableStateListOf<BillItem>()
    val customAmounts = mutableStateMapOf<String, String>()
    private var hasAppliedEditBill = false

    init {
        if (autoLoadMembers) {
            loadGroupMembers()
        } else {
            applyMembers(fallbackBillMembers, isFallback = true)
        }
        billItems.add(BillItem(name = "Món 1", price = 0.0, sharedByMemberIds = emptyList()))
        if (!editBillId.isNullOrBlank()) {
            _uiState.update { it.copy(isEditMode = true, isLoading = true) }
            loadBillForEdit(editBillId)
        }
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

    private fun loadBillForEdit(billId: String) {
        viewModelScope.launch {
            repository.getBill(groupId, billId).collect { bill ->
                when {
                    bill == null -> {
                        _uiState.update {
                            it.copy(
                                isEditMode = true,
                                isLoading = false,
                                error = "Không tìm thấy hóa đơn",
                            )
                        }
                    }

                    bill.createdBy != currentUserId -> {
                        _uiState.update {
                            it.copy(
                                isEditMode = true,
                                isLoading = false,
                                originalBill = bill,
                                error = "Chỉ người tạo hóa đơn mới có quyền sửa hóa đơn",
                            )
                        }
                    }

                    !hasAppliedEditBill -> applyBillForEdit(bill)

                    else -> {
                        _uiState.update {
                            it.copy(
                                isEditMode = true,
                                isLoading = false,
                                originalBill = bill,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applyBillForEdit(bill: Bill) {
        hasAppliedEditBill = true
        billItems.clear()
        if (bill.method == SplitMethod.ITEMIZED && bill.items.isNotEmpty()) {
            billItems.addAll(bill.items)
        } else {
            billItems.add(BillItem(name = "Món 1", price = 0.0, sharedByMemberIds = emptyList()))
        }

        customAmounts.clear()
        bill.shares.forEach { (memberId, amount) ->
            customAmounts[memberId] = amountToInputString(amount)
        }

        _uiState.update {
            it.copy(
                billName = bill.name,
                totalAmountStr = amountToInputString(bill.totalAmount),
                selectedMethod = bill.method,
                selectedMemberIds = bill.shares.keys,
                payerId = bill.payerId,
                paymentQrBankCode = bill.paymentQrBankCode,
                paymentQrAccountNumber = bill.paymentQrAccountNumber,
                paymentQrAccountName = bill.paymentQrAccountName,
                isEditMode = true,
                isLoading = false,
                originalBill = bill,
                error = null,
            )
        }
    }

    private fun applyMembers(
        members: List<Member>,
        isFallback: Boolean = false,
    ) {
        val state = _uiState.value
        val selectedIds =
            if (state.isEditMode || state.originalBill != null) {
                state.selectedMemberIds.ifEmpty { state.originalBill?.shares?.keys ?: emptySet() }
            } else {
                members.map { it.id }.toSet()
            }
        val payerId =
            if (state.isEditMode || state.originalBill != null) {
                state.payerId.ifBlank { state.originalBill?.payerId.orEmpty() }
            } else {
                members.firstOrNull { it.isMe }?.id ?: members.firstOrNull()?.id.orEmpty()
            }
        _uiState.update {
            it.copy(
                members = members,
                selectedMemberIds = selectedIds,
                payerId = payerId,
                isUsingFallbackMembers = isFallback,
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
        if (method == SplitMethod.ITEMIZED) {
            seedFirstItemFromEnteredTotal()
        }
        _uiState.update { it.copy(selectedMethod = method, error = null) }
    }

    fun onPaymentQrBankCodeChange(value: String) {
        _uiState.update {
            it.copy(
                paymentQrBankCode = value.filter { char -> char.isLetterOrDigit() }.uppercase(),
                error = null,
            )
        }
    }

    fun onPaymentQrAccountNumberChange(value: String) {
        _uiState.update {
            it.copy(
                paymentQrAccountNumber = value.filter { char -> char.isLetterOrDigit() },
                error = null,
            )
        }
    }

    fun onPaymentQrAccountNameChange(value: String) {
        _uiState.update {
            it.copy(
                paymentQrAccountName = value.uppercase(),
                error = null,
            )
        }
    }

    fun applyReceiptOcr(
        amount: Double?,
        merchantName: String?,
    ) {
        val cleanMerchantName = merchantName?.trim().orEmpty()
        _uiState.update { state ->
            val detectedAmount = amount?.takeIf { value -> value > 0.0 }
            val updatedTotal = detectedAmount?.let(::amountToInputString)
            if (detectedAmount != null && state.selectedMethod == SplitMethod.ITEMIZED) {
                applyReceiptAmountToFirstItem(detectedAmount)
            }

            state.copy(
                billName =
                    if (state.billName.isBlank() && cleanMerchantName.isNotBlank()) {
                        cleanMerchantName
                    } else {
                        state.billName
                    },
                totalAmountStr = updatedTotal ?: state.totalAmountStr,
                error = null,
            )
        }
    }

    private fun applyReceiptAmountToFirstItem(amount: Double) {
        if (billItems.isEmpty()) {
            billItems.add(BillItem(name = "Món 1", price = amount, sharedByMemberIds = emptyList()))
            return
        }

        billItems[0] = billItems[0].copy(price = amount)
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

    fun onCustomAmountChange(
        memberId: String,
        amount: String,
    ) {
        customAmounts[memberId] = amount.onlyDigits()
    }

    fun saveBill() {
        if (_uiState.value.isLoading || _uiState.value.isSaved) return
        viewModelScope.launch {
            saveBillBlocking()
        }
    }

    suspend fun saveBillBlocking(): Result<Unit> {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isSaved) {
            return Result.failure(IllegalStateException("Đang thực hiện tác vụ"))
        }
        val billName = currentState.billName.trim().ifBlank { "Hóa đơn mới" }
        val originalBill = currentState.originalBill
        val normalizedCurrentUserId = currentUserId.orEmpty()

        if (currentState.isEditMode) {
            val creatorId = originalBill?.createdBy.orEmpty()
            if (creatorId.isBlank() || creatorId != normalizedCurrentUserId) {
                val message = "Chỉ người tạo hóa đơn mới có quyền sửa hóa đơn"
                _uiState.update { it.copy(error = message) }
                return Result.failure(IllegalStateException(message))
            }
        } else if (normalizedCurrentUserId.isBlank()) {
            val message = "Bạn cần đăng nhập để tạo hóa đơn"
            _uiState.update { it.copy(error = message) }
            return Result.failure(IllegalStateException(message))
        }

        validateBillInput(currentState)?.let { error ->
            _uiState.update { it.copy(error = error) }
            return Result.failure(IllegalArgumentException(error))
        }

        val totalAmount = calculateTotalAmount(currentState)
        val shares =
            calculateShares(totalAmount, currentState).getOrElse { throwable ->
                val message = throwable.message ?: "Không thể tính tiền chia"
                _uiState.update { it.copy(error = message) }
                return Result.failure(IllegalArgumentException(message))
            }
        val paidMemberIds =
            if (currentState.isEditMode) {
                (originalBill?.paidMemberIds.orEmpty() + currentState.payerId)
                    .filter { memberId -> memberId == currentState.payerId || memberId in shares.keys }
                    .distinct()
            } else {
                listOf(currentState.payerId)
            }
        val bill =
            Bill(
                id = originalBill?.id ?: java.util.UUID.randomUUID().toString(),
                groupId = groupId,
                name = billName,
                totalAmount = totalAmount,
                payerId = currentState.payerId,
                method = currentState.selectedMethod,
                items = if (currentState.selectedMethod == SplitMethod.ITEMIZED) billItems.toList() else emptyList(),
                shares = shares,
                paidMemberIds = paidMemberIds,
                createdBy = originalBill?.createdBy?.ifBlank { normalizedCurrentUserId } ?: normalizedCurrentUserId,
                paymentQrBankCode = currentState.paymentQrBankCode.trim().uppercase(),
                paymentQrAccountNumber = currentState.paymentQrAccountNumber.trim(),
                paymentQrAccountName = currentState.paymentQrAccountName.trim(),
                date = originalBill?.date ?: System.currentTimeMillis(),
            )

        _uiState.update { it.copy(isLoading = true, error = null) }
        val result = repository.saveBill(bill)
        if (result.isSuccess && !currentState.isEditMode) {
            notifyBillCreated(
                bill = bill,
                members = currentState.members,
                senderId = normalizedCurrentUserId,
            )
        }
        _uiState.update {
            if (result.isSuccess) {
                it.copy(isLoading = false, isSaved = true, savedBill = bill)
            } else {
                it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Không thể lưu hóa đơn")
            }
        }
        return result
    }

    private suspend fun notifyBillCreated(
        bill: Bill,
        members: List<Member>,
        senderId: String,
    ) {
        val notifications = notificationRepository ?: return
        if (senderId.isBlank()) return

        val senderName =
            members.firstOrNull { member -> member.id == senderId }
                ?.name
                ?.takeIf { name -> name.isNotBlank() }
                ?: "Thanh vien"
        val recipientIds =
            (bill.shares.keys + bill.payerId)
                .filter { memberId -> memberId.isNotBlank() && memberId != senderId }
                .distinct()

        recipientIds.forEach { recipientId ->
            val amount = bill.shares[recipientId]?.takeIf { value -> value > 0.0 } ?: bill.totalAmount
            val notification =
                NotificationFactory.fromSplitTrigger(
                    trigger =
                        SplitNotificationTrigger(
                            billId = bill.id,
                            groupId = bill.groupId,
                            billTitle = bill.name,
                            amount = amount,
                            triggeredByUserId = senderId,
                            triggeredByUserName = senderName,
                            triggerType = SplitTriggerType.BILL_CREATED,
                        ),
                    recipientUserId = recipientId,
                ).copy(
                    id = billCreatedNotificationId(bill.id, recipientId),
                    senderId = senderId,
                )

            runCatching {
                notifications.insertNotification(notification)
            }
        }
    }

    private fun billCreatedNotificationId(
        billId: String,
        recipientId: String,
    ): String {
        val rawId = "bill_created_${billId}_$recipientId"
        return rawId
            .replace("/", "_")
            .replace("\\", "_")
    }

    private fun validateBillInput(state: CreateBillUiState): String? {
        val totalAmount = calculateTotalAmount(state)
        val selectedMembers = state.selectedMemberIds.toList()
        val hasAnyQrInput =
            state.paymentQrBankCode.isNotBlank() ||
                state.paymentQrAccountNumber.isNotBlank() ||
                state.paymentQrAccountName.isNotBlank()

        return when {
            groupId.isBlank() -> "Thiếu nhóm để lưu hóa đơn"
            totalAmount <= 0.0 -> "Tổng tiền phải lớn hơn 0"
            state.selectedMemberIds.isEmpty() -> "Cần chọn ít nhất một người tham gia"
            state.payerId.isBlank() -> "Cần chọn người thanh toán"
            state.payerId !in state.members.map { it.id } -> "Người thanh toán không hợp lệ"
            hasAnyQrInput && state.paymentQrBankCode.isBlank() -> "Nhập mã ngân hàng để tạo QR nhận tiền"
            hasAnyQrInput && state.paymentQrAccountNumber.isBlank() -> "Nhập số tài khoản để tạo QR nhận tiền"
            hasAnyQrInput && state.paymentQrAccountName.isBlank() -> "Nhập tên tài khoản để tạo QR nhận tiền"
            state.selectedMethod == SplitMethod.CUSTOM &&
                customAmounts.keys.any { it !in state.selectedMemberIds } -> "Số tiền tự nhập chỉ áp dụng cho người được chọn"
            state.selectedMethod == SplitMethod.CUSTOM &&
                selectedMembers.any { customAmounts[it].isNullOrBlank() } -> "Nhập số tiền cho tất cả thành viên được chọn"
            state.selectedMethod == SplitMethod.CUSTOM &&
                !SplitCalculationEngine.moneyEquals(customAmountsForSelected(selectedMembers).values.sum(), totalAmount) ->
                "Tổng tiền tự nhập phải bằng tổng hóa đơn"
            state.selectedMethod == SplitMethod.ITEMIZED &&
                billItems.any { it.name.isBlank() || it.price <= 0.0 } -> "Mỗi món cần có tên và giá hợp lệ"
            state.selectedMethod == SplitMethod.ITEMIZED &&
                billItems.any { it.sharedByMemberIds.isEmpty() } -> "Mỗi món cần chọn người chia"
            state.selectedMethod == SplitMethod.ITEMIZED &&
                billItems.any { item -> item.sharedByMemberIds.any { it !in state.selectedMemberIds } } ->
                "Người chia món phải nằm trong danh sách tham gia"
            else -> null
        }
    }

    private fun seedFirstItemFromEnteredTotal() {
        val enteredTotal = _uiState.value.totalAmountStr.toDoubleOrNull() ?: 0.0
        if (enteredTotal <= 0.0 || billItems.isEmpty() || billItems.any { it.price > 0.0 }) return

        billItems[0] = billItems[0].copy(price = enteredTotal)
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
        state: CreateBillUiState,
    ): Result<Map<String, Double>> {
        val selectedMemberIds = state.selectedMemberIds.toList()
        return when (state.selectedMethod) {
            SplitMethod.EQUAL ->
                SplitCalculationEngine.calculateEqualShares(
                    totalAmount = totalAmount,
                    memberIds = selectedMemberIds,
                )
            SplitMethod.CUSTOM ->
                SplitCalculationEngine.calculateCustomShares(
                    totalAmount = totalAmount,
                    memberIds = selectedMemberIds,
                    customAmounts = customAmountsForSelected(selectedMemberIds),
                )
            SplitMethod.ITEMIZED ->
                SplitCalculationEngine.calculateItemizedShares(
                    items = billItems.toList(),
                    memberIds = selectedMemberIds,
                )
        }
    }

    private fun customAmountsForSelected(memberIds: List<String>): Map<String, Double> {
        return memberIds.associateWith { memberId ->
            customAmounts[memberId]?.toDoubleOrNull() ?: 0.0
        }
    }

    private fun amountToInputString(amount: Double): String {
        return amount.toLong().toString()
    }

    private fun String.onlyDigits(): String {
        return filter { it.isDigit() }
    }
}
