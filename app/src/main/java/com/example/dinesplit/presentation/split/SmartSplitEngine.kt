package com.example.dinesplit.presentation.split

data class SplitMember(
    val id: String,
    val name: String,
    val initial: String,
    val isMe: Boolean = false,
)

data class BillItemInput(
    val id: String,
    val name: String,
    val price: Long,
    val sharedByMemberIds: List<String>,
)

data class SettlementResult(
    val fromUserId: String,
    val toUserId: String,
    val amount: Long,
)

object SmartSplitEngine {
    fun calculateEqualSplit(
        totalAmount: Long,
        memberIds: List<String>,
    ): Map<String, Long> {
        if (memberIds.isEmpty()) return emptyMap()

        val baseShare = totalAmount / memberIds.size
        val remainder = totalAmount % memberIds.size

        return memberIds.mapIndexed { index, memberId ->
            val amount = if (index == 0) baseShare + remainder else baseShare
            memberId to amount
        }.toMap()
    }

    fun calculateCustomSplit(
        totalAmount: Long,
        customAmounts: Map<String, Long>,
    ): Result<Map<String, Long>> {
        val customTotal = customAmounts.values.sum()

        return if (customTotal == totalAmount) {
            Result.success(customAmounts)
        } else {
            Result.failure(
                IllegalArgumentException("Tổng tiền custom phải bằng tổng hóa đơn"),
            )
        }
    }

    fun calculateItemizedSplit(
        items: List<BillItemInput>,
        memberIds: List<String>,
    ): Map<String, Long> {
        val result = memberIds.associateWith { 0L }.toMutableMap()

        items.forEach { item ->
            if (item.sharedByMemberIds.isNotEmpty()) {
                val baseShare = item.price / item.sharedByMemberIds.size
                val remainder = item.price % item.sharedByMemberIds.size

                item.sharedByMemberIds.forEachIndexed { index, memberId ->
                    val current = result[memberId] ?: 0L
                    val amount = if (index == 0) baseShare + remainder else baseShare
                    result[memberId] = current + amount
                }
            }
        }

        return result
    }

    fun calculateSettlement(
        payerId: String,
        shares: Map<String, Long>,
    ): List<SettlementResult> {
        return shares
            .filter { (memberId, amount) ->
                memberId != payerId && amount > 0
            }
            .map { (memberId, amount) ->
                SettlementResult(
                    fromUserId = memberId,
                    toUserId = payerId,
                    amount = amount,
                )
            }
    }

    fun validateCustomSplit(
        totalAmount: Long,
        customAmounts: Map<String, Long>,
    ): Boolean {
        return customAmounts.values.sum() == totalAmount
    }
}
