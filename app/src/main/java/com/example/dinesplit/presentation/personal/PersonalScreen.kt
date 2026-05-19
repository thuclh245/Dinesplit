package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.HomeTopBar
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PersonalScreen(
    userAvatarUrl: String?,
    uiState: PersonalUiState = PersonalUiState(isLoading = false),
    onOpenSearch: () -> Unit,
    onAddTransaction: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val expenseSlices = remember(uiState.transactions, uiState.categories) {
        buildExpenseSlices(uiState.transactions, uiState.categories)
    }
    val dailyBars = remember(uiState.transactions) {
        buildDailyExpenseBars(uiState.transactions)
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                userAvatarUrl = userAvatarUrl,
                title = "Personal",
                onOpenSearch = onOpenSearch
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(
                start = AppDimens.screenHorizontal,
                end = AppDimens.screenHorizontal,
                top = AppDimens.screenVertical,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            if (uiState.isLoading) {
                item {
                    LoadingBlock(message = "Loading personal finance data...")
                }
            } else {
                uiState.errorMessage?.let { message ->
                    item {
                        ErrorStateBlock(
                            title = "Cannot load Personal data",
                            subtitle = message,
                            onRetryClick = onRefresh
                        )
                    }
                }

                item {
                    BalanceCard(
                        balance = uiState.balance,
                        income = uiState.totalIncome,
                        expense = uiState.totalExpense
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                    ) {
                        QuickActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.History,
                            label = "Ledger",
                            value = "${uiState.transactions.size} entries",
                            onClick = onOpenHistory
                        )
                        QuickActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Category,
                            label = "Categories",
                            value = "${uiState.categories.size} active",
                            onClick = onOpenCategories
                        )
                    }
                }

                item {
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                            SectionHeader(title = "Spending by category", actionLabel = "Refresh", onAction = onRefresh)
                            PersonalPieChart(slices = expenseSlices)
                        }
                    }
                }

                item {
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                            SectionHeader(title = "Daily expense")
                            PersonalDailyExpenseBarChart(bars = dailyBars)
                        }
                    }
                }

                item {
                    SectionHeader(title = "Recent transactions", actionLabel = "View all", onAction = onOpenHistory)
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        EmptyStateBlock(
                            title = "No transactions yet",
                            subtitle = "Add an income or expense entry to start tracking your real Firebase data.",
                            actionText = "Add transaction",
                            onActionClick = onAddTransaction
                        )
                    }
                } else {
                    items(uiState.transactions.take(5), key = { it.id }) { transaction ->
                        TransactionRow(transaction = transaction)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppDimens.spaceXl))
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)) {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
                Text(
                    text = "Total balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMoney(balance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (balance >= 0.0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                MetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowDownward,
                    title = "Income",
                    amount = income,
                    color = MaterialTheme.colorScheme.primary
                )
                MetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowUpward,
                    title = "Expense",
                    amount = expense,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    amount: Double,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMoney(amount),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    AppCard(
        modifier = modifier,
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        Column(
            modifier = Modifier.clickable(onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            TextButton(onClick = onAction) {
                if (actionLabel == "Refresh") {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                Text(text = transaction.category, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = listOfNotNull(formatDate(transaction.date), transaction.note).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatSignedMoney(transaction),
                style = MaterialTheme.typography.titleSmall,
                color = if (transaction.type == TransactionType.INCOME) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

private fun buildExpenseSlices(
    transactions: List<Transaction>,
    categories: List<StoredCategory>
): List<PieCategorySlice> {
    val categoryNames = categories.associate { it.id to it.name }
    val expenseByCategory = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.categoryId }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
    val totalExpense = expenseByCategory.values.sum()

    if (totalExpense <= 0.0) return emptyList()

    return expenseByCategory
        .entries
        .sortedByDescending { it.value }
        .map { (categoryId, amount) ->
            PieCategorySlice(
                category = categoryNames[categoryId] ?: "Unknown",
                amount = amount,
                percentage = (amount / totalExpense).toFloat()
            )
        }
}

private fun buildDailyExpenseBars(transactions: List<Transaction>): List<DailyExpenseBar> {
    return transactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { transaction ->
            Calendar.getInstance().apply { timeInMillis = transaction.date }
                .get(Calendar.DAY_OF_MONTH)
        }
        .map { (day, items) ->
            DailyExpenseBar(dayOfMonth = day, amount = items.sumOf { it.amount })
        }
        .sortedBy { it.dayOfMonth }
}

private fun formatSignedMoney(transaction: Transaction): String {
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
    return "$sign${formatMoney(transaction.amount)}"
}

private fun formatMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())}đ"
}

private fun formatDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PersonalScreenPreview() {
    DineSplitTheme {
        PersonalScreen(
            userAvatarUrl = null,
            onOpenSearch = {},
            onAddTransaction = {}
        )
    }
}
