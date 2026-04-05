package com.example.dinesplit.presentation.personal

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

