package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.domain.repository.NotificationRepository
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class BillDetailUiState(
    val bill: Bill? = null,
    val members: List<Member> = emptyList(),
    val currentMemberId: String = "",
    val isLoading: Boolean = true,
    val isUpdatingPayment: Boolean = false,
    val paymentMessage: String? = null,
    val error: String? = null,
)

class BillDetailViewModel(
    private val repository: SplitRepository,
    private val notificationRepository: NotificationRepository,
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

    fun deleteBill(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPayment = true) }
            val result = repository.deleteBill(groupId, billId)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isUpdatingPayment = false,
                        paymentMessage = "Đã xóa hóa đơn",
                    )
                } else {
                    it.copy(
                        isUpdatingPayment = false,
                        paymentMessage = result.exceptionOrNull()?.message ?: "Không thể xóa hóa đơn",
                    )
                }
            }
            if (result.isSuccess) {
                onSuccess()
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
                                error = if (bill == null) "Không tìm thấy hóa đơn" else null,
                            )
                        }
                    }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isUpdatingPayment = false,
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
}
