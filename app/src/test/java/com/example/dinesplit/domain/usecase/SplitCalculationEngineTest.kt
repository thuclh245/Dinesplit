package com.example.dinesplit.domain.usecase

import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.SplitMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitCalculationEngineTest {
    @Test
    fun `settlement compresses unpaid balances across bills`() {
        val bills =
            listOf(
                Bill(
                    groupId = "g1",
                    name = "Dinner",
                    totalAmount = 300.0,
                    payerId = "a",
                    method = SplitMethod.EQUAL,
                    shares = mapOf("a" to 100.0, "b" to 100.0, "c" to 100.0),
                    paidMemberIds = listOf("a"),
                ),
                Bill(
                    groupId = "g1",
                    name = "Coffee",
                    totalAmount = 130.0,
                    payerId = "c",
                    method = SplitMethod.CUSTOM,
                    shares = mapOf("a" to 130.0, "c" to 0.0),
                    paidMemberIds = listOf("c"),
                ),
            )

        val balances =
            SplitCalculationEngine.calculateBalances(
                bills = bills,
                memberIds = listOf("a", "b", "c"),
            )
        val settlements = SplitCalculationEngine.calculateSettlements(balances)

        assertEquals(70.0, balances["a"] ?: 0.0, 0.001)
        assertEquals(-100.0, balances["b"] ?: 0.0, 0.001)
        assertEquals(30.0, balances["c"] ?: 0.0, 0.001)
        assertEquals(2, settlements.size)
        assertEquals("b", settlements[0].fromMemberId)
        assertEquals("a", settlements[0].toMemberId)
        assertEquals(70.0, settlements[0].amount, 0.001)
        assertEquals("b", settlements[1].fromMemberId)
        assertEquals("c", settlements[1].toMemberId)
        assertEquals(30.0, settlements[1].amount, 0.001)
    }

    @Test
    fun `paid members no longer contribute to balance`() {
        val bill =
            Bill(
                groupId = "g1",
                name = "Lunch",
                totalAmount = 300.0,
                payerId = "a",
                method = SplitMethod.EQUAL,
                shares = mapOf("a" to 100.0, "b" to 100.0, "c" to 100.0),
                paidMemberIds = listOf("a", "b"),
            )

        val balances =
            SplitCalculationEngine.calculateBalances(
                bills = listOf(bill),
                memberIds = listOf("a", "b", "c"),
            )

        assertEquals(100.0, balances["a"] ?: 0.0, 0.001)
        assertEquals(0.0, balances["b"] ?: 0.0, 0.001)
        assertEquals(-100.0, balances["c"] ?: 0.0, 0.001)
    }

    @Test
    fun `user balance keeps debt and receivable totals separate`() {
        val userId = "current_user"
        val bills = listOf(
            Bill(
                groupId = "g1",
                name = "Lunch",
                totalAmount = 90_000.0,
                payerId = "friend",
                method = SplitMethod.CUSTOM,
                shares = mapOf("friend" to 60_000.0, userId to 30_000.0),
                paidMemberIds = listOf("friend")
            ),
            Bill(
                groupId = "g1",
                name = "Dinner",
                totalAmount = 120_000.0,
                payerId = userId,
                method = SplitMethod.EQUAL,
                shares = mapOf(userId to 60_000.0, "friend" to 60_000.0),
                paidMemberIds = listOf(userId)
            )
        )

        val summary = SplitCalculationEngine.calculateUserBalance(
            bills = bills,
            userId = userId
        )

        assertEquals(30_000.0, summary.amountYouOwe, 0.001)
        assertEquals(60_000.0, summary.amountYouAreOwed, 0.001)
        assertEquals(30_000.0, summary.netBalance, 0.001)
    }
}
