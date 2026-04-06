package com.example.dinesplit.presentation.personal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.domain.model.TransactionType
import java.util.Calendar
import java.util.Locale

@Composable
fun PersonalRoute(
    onOpenAssistant: () -> Unit,
    onAddExpense: () -> Unit = {},
    onAddIncome: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenCategoryManagement: () -> Unit = {},
    viewModel: PersonalViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PersonalScreen(
        onOpenAssistant = onOpenAssistant,
        onAddExpense = onAddExpense,
        onAddIncome = onAddIncome,
        onOpenHistory = onOpenHistory,
        onOpenCategoryManagement = onOpenCategoryManagement,
        uiState = uiState.toDashboardUiState()
    )
}

private fun PersonalUiState.toDashboardUiState(): PersonalDashboardUiState {
    if (isLoading) {
        return PersonalDashboardUiState.Loading
    }

    if (transactions.isEmpty()) {
        return PersonalDashboardUiState.Empty
    }

    val monthlySummary = MonthlySummary(
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        balance = balance
    )
    val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
    val expenseTotalsByCategory = expenseTransactions
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
    val totalExpenseAmount = totalExpense.takeIf { it > 0.0 } ?: 1.0

    return PersonalDashboardUiState.HasData(
        summary = PersonalDashboardSummary(
            totalIncome = formatCurrencyVnd(totalIncome),
            totalExpense = formatCurrencyVnd(totalExpense),
            balance = formatCurrencyVnd(balance),
            balanceNote = if (balance >= 0) {
                "Positive balance this month"
            } else {
                "Balance is currently negative"
            }
        ),
        categoryBreakdowns = expenseTotalsByCategory.map { (category, amount) ->
            CategoryBreakdown(
                name = category,
                percentage = formatPercentage(amount / totalExpenseAmount),
                amount = formatCurrencyVnd(amount),
                progress = (amount / totalExpenseAmount).toFloat()
            )
        },
        pieChartData = expenseTotalsByCategory.map { (category, amount) ->
            PieCategorySlice(
                category = category,
                amount = amount,
                percentage = (amount / totalExpenseAmount).toFloat()
            )
        },
        dailyExpenseBars = expenseTransactions
            .groupBy { transaction -> dayOfMonth(transaction.date) }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .toSortedMap()
            .map { (day, amount) ->
                DailyExpenseBar(
                    dayOfMonth = day,
                    amount = amount
                )
            },
        monthlySummary = monthlySummary
    )
}

private fun formatCurrencyVnd(amount: Double): String {
    val grouped = String.format(Locale.US, "%,d", amount.toLong())
    return grouped.replace(',', '.') + "đ"
}

private fun formatPercentage(ratio: Double): String {
    return "${String.format(Locale.US, "%.0f", ratio * 100)}%"
}

private fun dayOfMonth(epochMillis: Long): Int {
    return Calendar.getInstance().apply {
        timeInMillis = epochMillis
    }.get(Calendar.DAY_OF_MONTH)
}

