package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.repository.SplitRepository
import com.example.dinesplit.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetLinkedBillSummaryUseCase(
    private val splitRepository: SplitRepository,
    private val profileRepository: ProfileRepository,
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

                // Check mutual followers (friendship) with bill creator or group owner
                var isFriend = false
                if (currentUserId.isNotBlank()) {
                    val creatorId = bill.createdBy
                    val ownerId = group?.ownerId.orEmpty()
                    val targetIds = listOfNotNull(
                        creatorId.takeIf { it.isNotBlank() && it != currentUserId },
                        ownerId.takeIf { it.isNotBlank() && it != currentUserId }
                    ).distinct()

                    for (targetId in targetIds) {
                        val followsTarget = profileRepository.isFollowing(currentUserId, targetId).getOrDefault(false)
                        val targetFollowsMe = profileRepository.isFollowing(targetId, currentUserId).getOrDefault(false)
                        if (followsTarget && targetFollowsMe) {
                            isFriend = true
                            break
                        }
                    }
                }

                val isAuthorized = isGroupMember || isParticipant || isBillCreator || isFriend

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
