package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.domain.model.QrPayment
import com.example.dinesplit.domain.model.QrPaymentStatus
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
import java.net.URLEncoder
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
    val isUnauthorized: Boolean = false,
    val isNotFound: Boolean = false,
    val qrPayments: List<QrPayment> = emptyList(),
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
        val state = _uiState.value
        val bill = state.bill ?: return
        val amount = bill.shares[state.currentMemberId] ?: 0.0
        initiateQrPayment(amount, bill.payerId)
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
                deepLinkDestination = "SPLIT_DETAIL",
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
                    repository.getGroup(groupId),
                    repository.getGroupMembers(groupId),
                    qrPaymentRepository.observeBillPayments(groupId, billId),
                ) { bill, group, members, payments -> (bill to group) to (members to payments) }
                    .collect { (billAndGroup, membersAndPayments) ->
                        val (bill, group) = billAndGroup
                        val (members, payments) = membersAndPayments
                        if (bill == null) {
                            _uiState.update {
                                it.copy(
                                    bill = null,
                                    members = emptyList(),
                                    qrPayments = emptyList(),
                                    isLoading = false,
                                    isUnauthorized = false,
                                    isNotFound = true,
                                    error = "Không tìm thấy hóa đơn",
                                )
                            }
                            return@collect
                        }

                        val userId = currentUserId.orEmpty()
                        val isGroupMember = group != null && (userId in group.memberIds && userId !in group.leftMemberIds || group.ownerId == userId)
                        val isGroupMemberFromList = members.any { it.id == userId }
                        val isBillParticipant = userId in bill.shares.keys
                        val isBillCreatorOrPayer = bill.payerId == userId || bill.createdBy == userId

                        val isAuthorized = userId.isNotBlank() && (
                            isGroupMember || isGroupMemberFromList || isBillParticipant || isBillCreatorOrPayer
                        )

                        if (!isAuthorized) {
                            _uiState.update {
                                it.copy(
                                    bill = null,
                                    members = emptyList(),
                                    qrPayments = emptyList(),
                                    isLoading = false,
                                    isUnauthorized = true,
                                    isNotFound = false,
                                    error = "Bạn không có quyền xem hóa đơn này.",
                                )
                            }
                            return@collect
                        }

                        val effectiveMembers = buildEffectiveMembers(members, bill)
                        _uiState.update {
                            it.copy(
                                bill = bill,
                                members = effectiveMembers,
                                qrPayments = payments,
                                currentMemberId = resolveCurrentMemberId(effectiveMembers),
                                isLoading = false,
                                isUpdatingPayment = false,
                                isDeleting = false,
                                isUnauthorized = false,
                                isNotFound = false,
                                canManageBill = bill.createdBy.isNotBlank() && bill.createdBy == currentUserId,
                                error = null,
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
                        isUnauthorized = false,
                        isNotFound = false,
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
        if (currentUid == receiverUid || amount <= 0.0 || currentUid in bill.paidMemberIds) return
        if (!bill.hasPaymentQr) {
            _uiState.update {
                it.copy(paymentMessage = "Hóa đơn này chưa có QR nhận tiền. Người tạo bill cần cập nhật thông tin QR.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPayment = true) }
            val paymentId = paymentIdFor(groupId, billId, currentUid)
            val now = Date()
            val description = "DINESPLIT ${bill.id.takeLast(6).uppercase()}"
            val qrContent = buildPaymentQrUrl(bill, amount, description)
            val payment = QrPayment(
                id = paymentId,
                groupId = groupId,
                billId = billId,
                payerUid = currentUid,
                receiverUid = receiverUid,
                amount = amount,
                status = QrPaymentStatus.PENDING,
                qrContent = qrContent,
                description = description,
                paymentGateway = "vietqr_manual",
                createdAt = now,
                updatedAt = now,
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

    private fun buildPaymentQrUrl(
        bill: Bill,
        amount: Double,
        description: String,
    ): String {
        val bankCode = bill.paymentQrBankCode.trim().uppercase()
        val accountNumber = bill.paymentQrAccountNumber.trim()
        val accountName = bill.paymentQrAccountName.trim()
        if (bankCode.isBlank() || accountNumber.isBlank() || accountName.isBlank()) return ""

        return "https://img.vietqr.io/image/$bankCode-$accountNumber-compact2.png" +
            "?amount=${amount.toLong()}" +
            "&addInfo=${encodeQrParam(description)}" +
            "&accountName=${encodeQrParam(accountName)}"
    }

    private fun encodeQrParam(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private fun observeActiveQrPayment(paymentId: String) {
        qrJob?.cancel()
        qrJob = viewModelScope.launch {
            qrPaymentRepository.observeQrPayment(paymentId).collectLatest { payment ->
                if (payment != null) {
                    _uiState.update { it.copy(activeQrPayment = payment) }
                    if (payment.status == QrPaymentStatus.MARKED_PAID || payment.status == QrPaymentStatus.CONFIRMED) {
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

    fun markActivePaymentTransferred() {
        val payment = _uiState.value.activeQrPayment ?: return

        viewModelScope.launch {
            val result =
                qrPaymentRepository.updateQrPaymentStatus(
                    paymentId = payment.id,
                    status = QrPaymentStatus.MARKED_PAID,
                    bankTransactionRef = "USER_MARKED_" + UUID.randomUUID().toString().take(6).uppercase()
                )
            if (result.isSuccess) {
                notifyPaymentMarked(payment)
                _uiState.update {
                    it.copy(
                        activeQrPayment = null,
                        paymentMessage = "ÄÃ£ gá»­i yÃªu cáº§u xÃ¡c nháº­n thanh toÃ¡n"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(paymentMessage = result.exceptionOrNull()?.message ?: "KhÃ´ng thá»ƒ bÃ¡o Ä‘Ã£ thanh toÃ¡n")
                }
            }
        }
    }

    fun confirmQrPayment(paymentId: String) {
        val payment = _uiState.value.qrPayments.firstOrNull { it.id == paymentId } ?: return
        val bill = _uiState.value.bill ?: return
        if (currentUserId != payment.receiverUid || payment.payerUid in bill.paidMemberIds) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPayment = true, paymentMessage = null) }
            val statusResult =
                qrPaymentRepository.updateQrPaymentStatus(
                    paymentId = payment.id,
                    status = QrPaymentStatus.CONFIRMED,
                    bankTransactionRef = payment.bankTransactionRef.ifBlank { "CONFIRMED_BY_RECEIVER" }
                )
            val paidResult =
                if (statusResult.isSuccess) {
                    repository.markBillMemberPaid(groupId, billId, payment.payerUid)
                } else {
                    Result.failure(statusResult.exceptionOrNull() ?: IllegalStateException("KhÃ´ng thá»ƒ xÃ¡c nháº­n thanh toÃ¡n"))
                }

            if (paidResult.isSuccess) {
                notifyPaymentConfirmed(payment)
                _uiState.update {
                    it.copy(isUpdatingPayment = false, paymentMessage = "ÄÃ£ xÃ¡c nháº­n Ä‘Ã£ nháº­n tiá»n")
                }
            } else {
                _uiState.update {
                    it.copy(
                        isUpdatingPayment = false,
                        paymentMessage = paidResult.exceptionOrNull()?.message ?: "KhÃ´ng thá»ƒ xÃ¡c nháº­n thanh toÃ¡n"
                    )
                }
            }
        }
    }

    fun rejectQrPayment(paymentId: String) {
        val payment = _uiState.value.qrPayments.firstOrNull { it.id == paymentId } ?: return
        if (currentUserId != payment.receiverUid) return

        viewModelScope.launch {
            val result =
                qrPaymentRepository.updateQrPaymentStatus(
                    paymentId = payment.id,
                    status = QrPaymentStatus.REJECTED,
                    bankTransactionRef = payment.bankTransactionRef.ifBlank { "REJECTED_BY_RECEIVER" }
                )
            if (result.isSuccess) {
                notifyPaymentRejected(payment)
                _uiState.update { it.copy(paymentMessage = "ÄÃ£ tá»« chá»‘i xÃ¡c nháº­n thanh toÃ¡n") }
            } else {
                _uiState.update {
                    it.copy(paymentMessage = result.exceptionOrNull()?.message ?: "KhÃ´ng thá»ƒ tá»« chá»‘i thanh toÃ¡n")
                }
            }
        }
    }

    private suspend fun notifyPaymentMarked(payment: QrPayment) {
        val bill = _uiState.value.bill ?: return
        val payerName = _uiState.value.members.firstOrNull { it.id == payment.payerUid }?.name
            ?: fallbackMemberName(payment.payerUid)
        val now = System.currentTimeMillis()
        notificationRepository.insertNotification(
            Notification(
                id = "${now}_${bill.id}_${payment.payerUid}_marked_paid",
                userId = payment.receiverUid,
                title = "$payerName Ä‘Ã£ bÃ¡o Ä‘Ã£ thanh toÃ¡n",
                subtitle = "${bill.name} - ${formatReminderAmount(payment.amount)} Ä‘",
                type = NotificationType.PAYMENT_PENDING,
                relatedId = bill.id,
                isRead = false,
                createdAt = now,
                updatedAt = now,
                deepLinkDestination = "SPLIT_DETAIL",
                deepLinkTargetId = bill.id,
                senderId = payment.payerUid,
                groupId = groupId,
            )
        )
    }

    private suspend fun notifyPaymentConfirmed(payment: QrPayment) {
        val bill = _uiState.value.bill ?: return
        val receiverName = _uiState.value.members.firstOrNull { it.id == payment.receiverUid }?.name
            ?: fallbackMemberName(payment.receiverUid)
        val now = System.currentTimeMillis()
        notificationRepository.insertNotification(
            Notification(
                id = "${now}_${bill.id}_${payment.payerUid}_confirmed",
                userId = payment.payerUid,
                title = "$receiverName Ä‘Ã£ xÃ¡c nháº­n thanh toÃ¡n",
                subtitle = "${bill.name} - ${formatReminderAmount(payment.amount)} Ä‘",
                type = NotificationType.PAYMENT_COMPLETED,
                relatedId = bill.id,
                isRead = false,
                createdAt = now,
                updatedAt = now,
                deepLinkDestination = "SPLIT_DETAIL",
                deepLinkTargetId = bill.id,
                senderId = payment.receiverUid,
                groupId = groupId,
            )
        )
    }

    private suspend fun notifyPaymentRejected(payment: QrPayment) {
        val bill = _uiState.value.bill ?: return
        val receiverName = _uiState.value.members.firstOrNull { it.id == payment.receiverUid }?.name
            ?: fallbackMemberName(payment.receiverUid)
        val now = System.currentTimeMillis()
        notificationRepository.insertNotification(
            Notification(
                id = "${now}_${bill.id}_${payment.payerUid}_rejected",
                userId = payment.payerUid,
                title = "$receiverName cáº§n kiá»ƒm tra láº¡i thanh toÃ¡n",
                subtitle = "${bill.name} - ${formatReminderAmount(payment.amount)} Ä‘",
                type = NotificationType.PAYMENT_PENDING,
                relatedId = bill.id,
                isRead = false,
                createdAt = now,
                updatedAt = now,
                deepLinkDestination = "SPLIT_DETAIL",
                deepLinkTargetId = bill.id,
                senderId = payment.receiverUid,
                groupId = groupId,
            )
        )
    }

    private fun paymentIdFor(groupId: String, billId: String, payerUid: String): String {
        return "pay_${groupId}_${billId}_$payerUid"
            .replace("/", "_")
            .replace("\\", "_")
    }
}
