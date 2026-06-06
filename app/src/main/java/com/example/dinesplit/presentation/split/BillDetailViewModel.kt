package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationDestination
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.domain.model.QrPayment
import com.example.dinesplit.domain.repository.NotificationRepository
import com.example.dinesplit.domain.repository.SplitRepository
import com.example.dinesplit.domain.repository.QrPaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import java.util.Date

data class BillDetailUiState(
    val bill: Bill? = null,
    val members: List<Member> = emptyList(),
    val currentMemberId: String = "",
    val isLoading: Boolean = true,
    val isUpdatingPayment: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val canManageBill: Boolean = false,
    val paymentMessage: String? = null,
    val error: String? = null,
    val activeQrPayment: QrPayment? = null,
)

class BillDetailViewModel(
    private val repository: SplitRepository,
    private val notificationRepository: NotificationRepository,
    private val qrPaymentRepository: QrPaymentRepository,
    private val groupId: String,
    private val billId: String,
    private val currentUserId: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BillDetailUiState())
    val uiState: StateFlow<BillDetailUiState> = _uiState.asStateFlow()

    init {
        observeBillDetail()
    }

    fun markCurrentMemberPaid() {
        markMemberPaid(_uiState.value.currentMemberId)
    }

    fun markMemberPaid(memberId: String) {
        val state = _uiState.value
        val bill = state.bill ?: return
        if (memberId.isBlank() || memberId == bill.payerId || memberId in bill.paidMemberIds) return

        viewModelScope.launch {
            val optimisticBill =
                bill.copy(
                    paidMemberIds = (bill.paidMemberIds + memberId).distinct(),
                )
            _uiState.update {
                it.copy(
                    bill = optimisticBill,
                    isUpdatingPayment = true,
                    paymentMessage = null,
                    error = null,
                )
            }

            val result = repository.markBillMemberPaid(groupId, billId, memberId)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isUpdatingPayment = false,
                        paymentMessage = "Đã đánh dấu đã trả",
                    )
                } else {
                    it.copy(
                        bill = bill,
                        isUpdatingPayment = false,
                        paymentMessage =
                            result.exceptionOrNull()?.message
                                ?: "Không thể cập nhật trạng thái thanh toán",
                    )
                }
            }
        }
    }

    fun sendPaymentReminder(memberId: String) {
        val state = _uiState.value
        val bill = state.bill ?: return
        if (memberId.isBlank() || memberId == currentUserId || memberId == bill.payerId || memberId in bill.paidMemberIds) {
            return
        }

        val memberName = state.members.firstOrNull { it.id == memberId }?.name
            ?: fallbackMemberName(memberId)
        val senderName = state.members.firstOrNull { it.id == currentUserId }?.name
            ?: fallbackMemberName(currentUserId.orEmpty())
                .ifBlank { "Một thành viên" }
        val amount = bill.shares[memberId] ?: 0.0

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val notification = Notification(
                id = "${now}_${bill.id}_${memberId}_payment_reminder",
                userId = memberId,
                title = "$senderName nhắc bạn thanh toán",
                subtitle = "${bill.name} - ${formatReminderAmount(amount)} đ",
                type = NotificationType.PAYMENT_PENDING,
                relatedId = bill.id,
                isRead = false,
                createdAt = now,
                updatedAt = now,
                deepLinkDestination = NotificationDestination.SPLIT_DETAIL.name,
                deepLinkTargetId = bill.id,
                senderId = currentUserId,
                groupId = groupId,
            )

            val result = runCatching {
                notificationRepository.insertNotification(notification)
            }

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(paymentMessage = "Đã nhắc $memberName thanh toán")
                } else {
                    it.copy(
                        paymentMessage = result.exceptionOrNull()?.message
                            ?: "Không thể gửi nhắc thanh toán",
                    )
                }
            }
        }
    }

    fun deleteBill() {
        val state = _uiState.value
        if (state.isDeleting || state.isDeleted) return
        val bill = state.bill ?: return
        val userId = currentUserId.orEmpty()

        if (!state.canManageBill || bill.createdBy != userId) {
            _uiState.update {
                it.copy(paymentMessage = "Chỉ người tạo hóa đơn mới có quyền xóa hóa đơn")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, paymentMessage = null) }
            val result = repository.deleteBill(groupId, billId, userId)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isDeleting = false,
                        isDeleted = true,
                        bill = null,
                        canManageBill = false,
                    )
                } else {
                    it.copy(
                        isDeleting = false,
                        paymentMessage = result.exceptionOrNull()?.message
                            ?: "Không thể xóa hóa đơn",
                    )
                }
            }
        }
    }

    fun consumePaymentMessage() {
        _uiState.update { it.copy(paymentMessage = null) }
    }

    private fun observeBillDetail() {
        viewModelScope.launch {
            runCatching {
                combine(
                    repository.getBill(groupId, billId),
                    repository.getGroupMembers(groupId),
                ) { bill, members -> bill to members }
                    .collect { (bill, members) ->
                        val effectiveMembers = buildEffectiveMembers(members, bill)
                        _uiState.update {
                            it.copy(
                                bill = bill,
                                members = effectiveMembers,
                                currentMemberId = resolveCurrentMemberId(effectiveMembers),
                                isLoading = false,
                                isUpdatingPayment = false,
                                isDeleting = false,
                                canManageBill = bill?.createdBy?.isNotBlank() == true && bill.createdBy == currentUserId,
                                error = if (bill == null) "Không tìm thấy hóa đơn" else null,
                            )
                        }
                    }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isUpdatingPayment = false,
                        isDeleting = false,
                        canManageBill = false,
                        error = throwable.message ?: "Không thể tải chi tiết hóa đơn",
                    )
                }
            }
        }
    }

    private fun buildEffectiveMembers(
        firestoreMembers: List<Member>,
        bill: Bill?,
    ): List<Member> {
        val memberById = firestoreMembers.associateBy { it.id }
        val billMemberIds = bill?.let { it.shares.keys + it.payerId }.orEmpty()
        val ids =
            (firestoreMembers.map { it.id } + billMemberIds)
                .filter { it.isNotBlank() }
                .distinct()

        return ids.map { id ->
            memberById[id]?.copy(isMe = id == currentUserId) ?: run {
                val name = fallbackMemberName(id)
                Member(
                    id = id,
                    name = name,
                    initial = name.firstOrNull()?.uppercase().orEmpty(),
                    isMe = id == currentUserId,
                )
            }
        }
    }

    private fun resolveCurrentMemberId(members: List<Member>): String {
        return members.firstOrNull { it.id == currentUserId }?.id.orEmpty()
    }

    private fun fallbackMemberName(memberId: String): String {
        if (memberId == currentUserId) return "Bạn"

        return when (memberId) {
            "me" -> "Bạn"
            "minh" -> "Minh"
            "thanh_hang" -> "Thanh Hằng"
            "tuan_anh" -> "Tuấn Anh"
            else -> memberId
        }
    }

    private fun formatReminderAmount(amount: Double): String {
        return "%,.0f".format(Locale("vi", "VN"), amount).replace(",", ".")
    }

    private var qrJob: kotlinx.coroutines.Job? = null

    fun initiateQrPayment(amount: Double, receiverUid: String) {
        val state = _uiState.value
        val bill = state.bill ?: return
        val currentUid = currentUserId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPayment = true) }
            val paymentId = UUID.randomUUID().toString().take(8)
            val payment = QrPayment(
                id = paymentId,
                groupId = groupId,
                billId = billId,
                payerUid = currentUid,
                receiverUid = receiverUid,
                amount = amount,
                status = "PENDING",
                description = "DSPLIT $paymentId",
                paymentGateway = "vietqr_gateway",
                createdAt = Date()
            )

            val result = qrPaymentRepository.createQrPayment(payment)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        activeQrPayment = payment,
                        isUpdatingPayment = false
                    )
                }
                observeActiveQrPayment(paymentId)
            } else {
                _uiState.update {
                    it.copy(
                        isUpdatingPayment = false,
                        paymentMessage = result.exceptionOrNull()?.message ?: "Không thể khởi tạo QR"
                    )
                }
            }
        }
    }

    private fun observeActiveQrPayment(paymentId: String) {
        qrJob?.cancel()
        qrJob = viewModelScope.launch {
            qrPaymentRepository.observeQrPayment(paymentId).collectLatest { payment ->
                if (payment != null) {
                    _uiState.update { it.copy(activeQrPayment = payment) }
                    if (payment.status == "VERIFIED") {
                        // Success! Trigger mark member as paid locally & in DB
                        markMemberPaid(payment.payerUid)
                        _uiState.update { it.copy(activeQrPayment = null) }
                        qrJob?.cancel()
                    }
                }
            }
        }
    }

    fun cancelQrPayment() {
        qrJob?.cancel()
        _uiState.update { it.copy(activeQrPayment = null) }
    }

    fun simulateBankCallback(paymentId: String) {
        val payment = _uiState.value.activeQrPayment ?: return
        if (payment.id != paymentId) return

        viewModelScope.launch {
            qrPaymentRepository.updateQrPaymentStatus(
                paymentId = paymentId,
                status = "VERIFIED",
                bankTransactionRef = "BANK_REF_" + UUID.randomUUID().toString().take(6).uppercase()
            )
        }
    }
}
