package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.ui.theme.DineSplitTheme

enum class HistoryFilterType(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense")
}

enum class HistoryTransactionType {
    INCOME,
    EXPENSE
}

data class HistoryTransactionItem(
    val id: String,
    val categoryIcon: String,
    val category: String,
    val amount: String,
    val date: String,
    val month: String,
    val type: HistoryTransactionType,
    val note: String?
)

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    transactions: List<HistoryTransactionItem> = defaultHistoryTransactions()
) {
    val months = remember(transactions) {
        transactions.map { it.month }.distinct()
    }
    var selectedMonth by rememberSaveable { mutableStateOf(months.firstOrNull().orEmpty()) }
    var selectedType by rememberSaveable { mutableStateOf(HistoryFilterType.ALL.name) }

    val filteredTransactions = remember(transactions, selectedMonth, selectedType) {
        transactions.filter { item ->
            val monthMatched = selectedMonth.isBlank() || item.month == selectedMonth
            val typeMatched = when (HistoryFilterType.valueOf(selectedType)) {
                HistoryFilterType.ALL -> true
                HistoryFilterType.INCOME -> item.type == HistoryTransactionType.INCOME
                HistoryFilterType.EXPENSE -> item.type == HistoryTransactionType.EXPENSE
            }
            monthMatched && typeMatched
        }
    }

    AppScaffold(
        title = "Transaction History",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            if (months.isNotEmpty()) {
                MonthFilterChips(
                    months = months,
                    selectedMonth = selectedMonth,
                    onMonthSelected = { selectedMonth = it }
                )
            }

            TypeFilterChips(
                selectedType = HistoryFilterType.valueOf(selectedType),
                onTypeSelected = { selectedType = it.name }
            )

            if (filteredTransactions.isEmpty()) {
                EmptyStateBlock(
                    title = "No transactions found",
                    subtitle = "Try another month or transaction type."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    items(
                        items = filteredTransactions,
                        key = { it.id }
                    ) { item ->
                        HistoryTransactionRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthFilterChips(
    months: List<String>,
    selectedMonth: String,
    onMonthSelected: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
    ) {
        Text(
            text = "Month",
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            months.forEach { month ->
                FilterChip(
                    selected = selectedMonth == month,
                    onClick = { onMonthSelected(month) },
                    label = { Text(month) }
                )
            }
        }
    }
}

@Composable
private fun TypeFilterChips(
    selectedType: HistoryFilterType,
    onTypeSelected: (HistoryFilterType) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
    ) {
        Text(
            text = "Type",
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            HistoryFilterType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.label) }
                )
            }
        }
    }
}

@Composable
private fun HistoryTransactionRow(
    item: HistoryTransactionItem
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.categoryIcon,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!item.note.isNullOrBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val amountColor = if (item.type == HistoryTransactionType.INCOME) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }

            Text(
                text = item.amount,
                style = MaterialTheme.typography.titleSmall,
                color = amountColor
            )
        }
    }
}

private fun defaultHistoryTransactions(): List<HistoryTransactionItem> {
    return listOf(
        HistoryTransactionItem(
            id = "tx_1",
            categoryIcon = "FD",
            category = "Food",
            amount = "-525,000",
            date = "05 Apr 2026",
            month = "Apr 2026",
            type = HistoryTransactionType.EXPENSE,
            note = "Lunch with team"
        ),
        HistoryTransactionItem(
            id = "tx_2",
            categoryIcon = "TR",
            category = "Travel",
            amount = "-187,500",
            date = "04 Apr 2026",
            month = "Apr 2026",
            type = HistoryTransactionType.EXPENSE,
            note = null
        ),
        HistoryTransactionItem(
            id = "tx_3",
            categoryIcon = "SL",
            category = "Salary",
            amount = "+3,500,000",
            date = "01 Apr 2026",
            month = "Apr 2026",
            type = HistoryTransactionType.INCOME,
            note = "Monthly salary"
        ),
        HistoryTransactionItem(
            id = "tx_4",
            categoryIcon = "DR",
            category = "Drink",
            amount = "-220,000",
            date = "20 Mar 2026",
            month = "Mar 2026",
            type = HistoryTransactionType.EXPENSE,
            note = null
        ),
        HistoryTransactionItem(
            id = "tx_5",
            categoryIcon = "BS",
            category = "Bonus",
            amount = "+750,000",
            date = "15 Mar 2026",
            month = "Mar 2026",
            type = HistoryTransactionType.INCOME,
            note = "Project reward"
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistoryScreenPreview() {
    DineSplitTheme {
        HistoryScreen(onBack = {})
    }
}

