package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillItem
import com.example.dinesplit.domain.model.Settlement
import kotlin.math.abs
import kotlin.math.roundToLong

object SplitCalculationEngine {
    private const val MONEY_EPSILON = 0.5

    fun calculateEqualShares(
        totalAmount: Double,
        memberIds: List<String>,
    ): Result<Map<String, Double>> {
        val cleanMemberIds = memberIds.filter { it.isNotBlank() }.distinct()
        if (totalAmount <= 0.0) return Result.failure(IllegalArgumentException("Total amount must be positive"))
        if (cleanMemberIds.isEmpty()) return Result.failure(IllegalArgumentException("At least one member is required"))

        val total = totalAmount.roundToLong()
        val baseShare = total / cleanMemberIds.size
        val remainder = total % cleanMemberIds.size

        return Result.success(
            cleanMemberIds.mapIndexed { index, memberId ->
                val amount = baseShare + if (index < remainder) 1 else 0
                memberId to amount.toDouble()
            }.toMap(),
        )
    }

    fun calculateCustomShares(
        totalAmount: Double,
        memberIds: List<String>,
        customAmounts: Map<String, Double>,
    ): Result<Map<String, Double>> {
        val cleanMemberIds = memberIds.filter { it.isNotBlank() }.distinct()
        if (totalAmount <= 0.0) return Result.failure(IllegalArgumentException("Total amount must be positive"))
        if (cleanMemberIds.isEmpty()) return Result.failure(IllegalArgumentException("At least one member is required"))

        val shares =
            cleanMemberIds.associateWith { memberId ->
                customAmounts[memberId] ?: return Result.failure(
                    IllegalArgumentException("Each selected member needs a custom amount"),
                )
            }

        if (shares.values.any { it < 0.0 }) {
            return Result.failure(IllegalArgumentException("Custom amounts cannot be negative"))
        }

        val customTotal = shares.values.sum()
        if (!moneyEquals(customTotal, totalAmount)) {
            return Result.failure(IllegalArgumentException("Custom amounts must add up to the bill total"))
        }

        return Result.success(shares.mapValues { (_, amount) -> amount.roundToLong().toDouble() })
    }

    fun calculateItemizedShares(
        items: List<BillItem>,
        memberIds: List<String>,
    ): Result<Map<String, Double>> {
        val cleanMemberIds = memberIds.filter { it.isNotBlank() }.distinct()
        if (cleanMemberIds.isEmpty()) return Result.failure(IllegalArgumentException("At least one member is required"))
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("At least one item is required"))

        val validMemberIds = cleanMemberIds.toSet()
        val shares = cleanMemberIds.associateWith { 0L }.toMutableMap()

        items.forEach { item ->
            if (item.name.isBlank()) return Result.failure(IllegalArgumentException("Item name is required"))
            if (item.price <= 0.0) return Result.failure(IllegalArgumentException("Item price must be positive"))
            if (item.sharedByMemberIds.isEmpty()) {
                return Result.failure(IllegalArgumentException("Each item needs at least one sharer"))
            }
            if (item.sharedByMemberIds.any { it !in validMemberIds }) {
                return Result.failure(IllegalArgumentException("Item sharers must be selected members"))
            }

            val price = item.price.roundToLong()
            val sharers = item.sharedByMemberIds.distinct()
            val baseShare = price / sharers.size
            val remainder = price % sharers.size
            sharers.forEachIndexed { index, memberId ->
                shares[memberId] = shares.getValue(memberId) + baseShare + if (index < remainder) 1 else 0
            }
        }

        return Result.success(shares.mapValues { (_, amount) -> amount.toDouble() })
    }

    fun calculateBalances(
        bills: List<Bill>,
        memberIds: List<String>,
    ): Map<String, Double> {
        val balances = memberIds.filter { it.isNotBlank() }.distinct().associateWith { 0.0 }.toMutableMap()

        bills.forEach { bill ->
            if (bill.payerId.isBlank()) return@forEach
            balances.putIfAbsent(bill.payerId, 0.0)

            bill.shares.forEach { (memberId, amount) ->
                balances.putIfAbsent(memberId, 0.0)
                val isPayer = memberId == bill.payerId
                val isPaid = memberId in bill.paidMemberIds
                if (!isPayer && !isPaid && amount > MONEY_EPSILON) {
                    balances[memberId] = balances.getValue(memberId) - amount
                    balances[bill.payerId] = balances.getValue(bill.payerId) + amount
                }
            }
        }

        return balances.mapValues { (_, amount) -> if (abs(amount) < MONEY_EPSILON) 0.0 else amount }
    }

    fun calculateSettlements(balances: Map<String, Double>): List<Settlement> {
        val debtors =
            balances
                .filter { it.value < -MONEY_EPSILON }
                .map { it.key to abs(it.value) }
                .toMutableList()
        val creditors =
            balances
                .filter { it.value > MONEY_EPSILON }
                .map { it.key to it.value }
                .toMutableList()
        val settlements = mutableListOf<Settlement>()

        var debtorIndex = 0
        var creditorIndex = 0
        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val debtor = debtors[debtorIndex]
            val creditor = creditors[creditorIndex]
            val amount = minOf(debtor.second, creditor.second)

            if (amount > MONEY_EPSILON) {
                settlements +=
                    Settlement(
                        fromMemberId = debtor.first,
                        toMemberId = creditor.first,
                        amount = amount,
                    )
            }

            debtors[debtorIndex] = debtor.first to (debtor.second - amount)
            creditors[creditorIndex] = creditor.first to (creditor.second - amount)

            if (debtors[debtorIndex].second <= MONEY_EPSILON) debtorIndex++
            if (creditors[creditorIndex].second <= MONEY_EPSILON) creditorIndex++
        }

        return settlements
    }

    fun moneyEquals(
        left: Double,
        right: Double,
    ): Boolean {
        return abs(left.roundToLong() - right.roundToLong()) <= 0
    }
}
