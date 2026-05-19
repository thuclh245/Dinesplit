package com.example.dinesplit.presentation.personal

import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import java.util.Calendar

/** Chart-ready input for a pie chart by category. */
data class PieCategorySlice(
    val category: String,
    val amount: Double,
    val percentage: Float
)

/** Chart-ready input for a daily expense bar chart in a month. */
data class DailyExpenseBar(
    val dayOfMonth: Int,
    val amount: Double
)

/** Monthly totals used across dashboard, chart summary, and notifications. */
data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double
)

/** Combined chart state for Personal dashboard. Helps keep data transformation centralized and reusable. */
data class PersonalChartState(
    val pieSlices: List<PieCategorySlice> = emptyList(),
    val dailyExpenseBars: List<DailyExpenseBar> = emptyList(),
    val monthlySummary: MonthlySummary = MonthlySummary(0.0, 0.0, 0.0)
)

fun List<Transaction>.toMonthlySummary(): MonthlySummary {
    val income = filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense = filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    return MonthlySummary(
        totalIncome = income,
        totalExpense = expense,
        balance = income - expense
    )
}

fun List<Transaction>.toPieCategorySlices(): List<PieCategorySlice> {
    val expenseTransactions = filter { it.type == TransactionType.EXPENSE }
    if (expenseTransactions.isEmpty()) return emptyList()

    val totalsByCategory = expenseTransactions.groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }

    val totalAmount = totalsByCategory.values.sum()
    if (totalAmount <= 0.0) return emptyList()

    return totalsByCategory.entries
        .sortedByDescending { it.value }
        .map { (category, amount) ->
            PieCategorySlice(
                category = category,
                amount = amount,
                percentage = ((amount / totalAmount) * 100.0).toFloat()
            )
        }
}

fun List<Transaction>.toDailyExpenseBars(): List<DailyExpenseBar> {
    if (isEmpty()) return emptyList()

    val expenseByDay = filter { it.type == TransactionType.EXPENSE }
        .groupBy { transaction ->
            Calendar.getInstance().apply { timeInMillis = transaction.date }
                .get(Calendar.DAY_OF_MONTH)
        }
        .mapValues { (_, items) -> items.sumOf { it.amount } }

    return expenseByDay.entries
        .sortedBy { it.key }
        .map { (day, amount) ->
            DailyExpenseBar(
                dayOfMonth = day,
                amount = amount
            )
        }
}
