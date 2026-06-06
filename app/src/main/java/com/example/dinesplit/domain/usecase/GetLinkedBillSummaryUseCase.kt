package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetLinkedBillSummaryUseCase(
    private val splitRepository: SplitRepository,
) {
    operator fun invoke(
        groupId: String,
        billId: String,
        currentUserId: String,
    ): Flow<LinkedBillSummary?> {
        return combine(
            splitRepository.getBill(groupId, billId),
            splitRepository.getGroup(groupId)
        ) { bill, group ->
            if (bill == null) {
                null
            } else {
                val isIPayer = currentUserId == bill.payerId
                val myShare = bill.shares[currentUserId] ?: 0.0
                val isMyPaid = currentUserId in bill.paidMemberIds
                val isSettled = bill.status == com.example.dinesplit.domain.model.BillStatus.SETTLED
                val isParticipant = isIPayer || bill.shares.containsKey(currentUserId)

                val isGroupMember = group != null && (currentUserId in group.memberIds && currentUserId !in group.leftMemberIds || group.ownerId == currentUserId)
                val isBillCreator = currentUserId == bill.createdBy
                val isAuthorized = isGroupMember || isParticipant || isBillCreator

                LinkedBillSummary(
                    billId = bill.id,
                    groupId = bill.groupId,
                    billName = bill.name,
                    totalAmount = bill.totalAmount,
                    isSettled = isSettled,
                    myShare = myShare,
                    isMyPaid = isMyPaid,
                    isIPayer = isIPayer,
                    isParticipant = isParticipant,
                    isAuthorized = isAuthorized,
                )
            }
        }
    }
}
