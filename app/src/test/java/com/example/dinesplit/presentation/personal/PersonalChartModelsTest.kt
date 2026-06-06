package com.example.dinesplit.presentation.personal

import com.example.dinesplit.domain.model.GoalStatus
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.RecurringCadence
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PersonalChartModelsTest {
    @Test
    fun `safe to spend does not double count recurring transaction already posted today`() {
        val referenceMillis = juneSixth2026()
        val categories =
            mapOf(
                "c_food" to TransactionType.EXPENSE,
                "c_salary" to TransactionType.INCOME,
            )
        val recurringRules =
            listOf(
                recurringRule(
                    id = "r_dining",
                    amount = 15_000.0,
                    categoryId = "c_food",
                    nextRunAt = julySixth2026(),
                ),
            )
        val goals =
            listOf(
                goal(
                    id = "g_running",
                    targetAmount = 80_000.0,
                    currentAmount = 60_000.0,
                    categoryId = "c_food",
                    status = GoalStatus.ACTIVE,
                ),
                goal(
                    id = "g_eat",
                    targetAmount = 60_000.0,
                    currentAmount = 10_000.0,
                    categoryId = null,
                    status = GoalStatus.ACTIVE,
                ),
                goal(
                    id = "g_playing",
                    targetAmount = 80_000.0,
                    currentAmount = 80_000.0,
                    categoryId = "c_food",
                    status = GoalStatus.COMPLETED,
                ),
            )

        val planReserve =
            goals.toPlanReserve(
                categoryTypesById = categories,
                recurringRules = recurringRules,
                referenceMillis = referenceMillis,
            )
        val upcomingRecurringExpense =
            recurringRules.toUpcomingRecurringExpense(referenceMillis = referenceMillis)
        val forecast =
            listOf(
                transaction(
                    id = "income",
                    amount = 2_000_000.0,
                    type = TransactionType.INCOME,
                    categoryId = "c_salary",
                    category = "Salary",
                    date = referenceMillis,
                ),
                transaction(
                    id = "expense",
                    amount = 100_000.0,
                    type = TransactionType.EXPENSE,
                    categoryId = "c_food",
                    category = "Dining Out",
                    date = referenceMillis,
                ),
            ).toSafeToSpendForecast(
                referenceMillis = referenceMillis,
                upcomingRecurringExpense = upcomingRecurringExpense,
                savingsGoal = planReserve,
            )

        assertEquals(0.0, upcomingRecurringExpense, 0.001)
        assertEquals(70_000.0, planReserve, 0.001)
        assertEquals(25, forecast.daysLeft)
        assertEquals(73_200.0, forecast.dailyAmount, 0.001)
    }

    @Test
    fun `future recurring in current month covers matching expense goal reserve`() {
        val referenceMillis = juneSixth2026()
        val categories = mapOf("c_food" to TransactionType.EXPENSE)
        val recurringRules =
            listOf(
                recurringRule(
                    id = "r_dining",
                    amount = 15_000.0,
                    categoryId = "c_food",
                    nextRunAt = juneTwentieth2026(),
                ),
            )
        val goals =
            listOf(
                goal(
                    id = "g_running",
                    targetAmount = 80_000.0,
                    currentAmount = 60_000.0,
                    categoryId = "c_food",
                    status = GoalStatus.ACTIVE,
                ),
            )

        val upcomingRecurringExpense =
            recurringRules.toUpcomingRecurringExpense(referenceMillis = referenceMillis)
        val planReserve =
            goals.toPlanReserve(
                categoryTypesById = categories,
                recurringRules = recurringRules,
                referenceMillis = referenceMillis,
            )

        assertEquals(15_000.0, upcomingRecurringExpense, 0.001)
        assertEquals(5_000.0, planReserve, 0.001)
    }

    private fun juneSixth2026(): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JUNE, 6, 12, 0, 0)
        }.timeInMillis

    private fun juneTwentieth2026(): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JUNE, 20, 9, 0, 0)
        }.timeInMillis

    private fun julySixth2026(): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JULY, 6, 9, 0, 0)
        }.timeInMillis

    private fun transaction(
        id: String,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        category: String,
        date: Long,
    ): Transaction =
        Transaction(
            id = id,
            userId = "user",
            amount = amount,
            type = type,
            categoryId = categoryId,
            category = category,
            note = null,
            date = date,
            createdAt = date,
        )

    private fun recurringRule(
        id: String,
        amount: Double,
        categoryId: String,
        nextRunAt: Long,
    ): RecurringRule =
        RecurringRule(
            id = id,
            userId = "user",
            name = "Dining recurring",
            amount = amount,
            type = TransactionType.EXPENSE,
            categoryId = categoryId,
            categoryName = "Dining Out",
            cadence = RecurringCadence.MONTHLY,
            dayOfMonth = 6,
            nextRunAt = nextRunAt,
            isEnabled = true,
            createdAt = 0L,
            updatedAt = 0L,
        )

    private fun goal(
        id: String,
        targetAmount: Double,
        currentAmount: Double,
        categoryId: String?,
        status: GoalStatus,
    ): PersonalGoal =
        PersonalGoal(
            id = id,
            userId = "user",
            title = id,
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            categoryId = categoryId,
            deadlineAt = 0L,
            status = status,
            createdAt = 0L,
            updatedAt = 0L,
        )
}
