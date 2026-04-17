package com.example.dinesplit.presentation.personal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.domain.model.TransactionType
import java.util.Calendar
import java.text.SimpleDateFormat
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

    val categoriesById = categories.associateBy { it.id }
    val monthlySummary = MonthlySummary(
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        balance = balance
    )
    val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
    val expenseTotalsByCategory = expenseTransactions
        .groupBy { transaction -> categoriesById[transaction.categoryId]?.name ?: transaction.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
    val recentActivity = transactions
        .sortedByDescending { it.date }
        .take(3)
        .map { transaction ->
            val resolvedCategory = categoriesById[transaction.categoryId]
            val categoryName = resolvedCategory?.name ?: transaction.category
            RecentActivityUi(
                icon = resolvedCategory?.icon ?: categoryName.take(2).uppercase(Locale.US),
                title = categoryName,
                subtitle = "${activityLabel(categoryName, transaction.type)} • ${relativeDateTimeLabel(transaction.date)}",
                amount = formatSignedCurrencyVnd(transaction.type, transaction.amount),
                isIncome = transaction.type == TransactionType.INCOME
            )
        }
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
        recentActivity = recentActivity,
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

private fun formatSignedCurrencyVnd(type: TransactionType, amount: Double): String {
    val sign = if (type == TransactionType.INCOME) "+" else "-"
    return sign + formatCurrencyVnd(amount)
}


private fun relativeDateLabel(epochMillis: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    val dayDiff = dayOfYear(now) - dayOfYear(target)

    return when {
        sameYear && dayDiff == 0 -> "Today"
        sameYear && dayDiff == 1 -> "Yesterday"
        else -> String.format(Locale.getDefault(), "%02d/%02d", target.get(Calendar.DAY_OF_MONTH), target.get(Calendar.MONTH) + 1)
    }
}

private fun activityLabel(categoryName: String, type: TransactionType): String {
    if (type == TransactionType.INCOME) return "Income"

    val lowered = categoryName.lowercase(Locale.getDefault())
    return when {
        "dining" in lowered || "food" in lowered -> "Dining"
        "transit" in lowered || "travel" in lowered || "taxi" in lowered || "uber" in lowered -> "Transport"
        "grocery" in lowered || "shop" in lowered -> "Shopping"
        else -> "Personal"
    }
}

private fun relativeDateTimeLabel(epochMillis: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    val dayDiff = dayOfYear(now) - dayOfYear(target)
    val timeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(target.time)

    return when {
        sameYear && dayDiff == 0 -> "Today, $timeText"
        sameYear && dayDiff == 1 -> "Yesterday, $timeText"
        else -> {
            val dateText = String.format(
                Locale.getDefault(),
                "%02d/%02d",
                target.get(Calendar.DAY_OF_MONTH),
                target.get(Calendar.MONTH) + 1
            )
            "$dateText, $timeText"
        }
    }
}

private fun dayOfYear(calendar: Calendar): Int {
    return calendar.get(Calendar.DAY_OF_YEAR)
}

private fun dayOfMonth(epochMillis: Long): Int {
    return Calendar.getInstance().apply {
        timeInMillis = epochMillis
    }.get(Calendar.DAY_OF_MONTH)
}

