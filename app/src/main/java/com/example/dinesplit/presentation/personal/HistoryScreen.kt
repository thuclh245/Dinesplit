package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.NumberFormat
import java.util.Locale

data class HistoryTransactionItem(
    val id: String,
    val categoryIcon: String,
    val category: String,
    val amount: String,
    val date: String,
    val month: String,
    val type: TransactionType,
    val note: String?
)

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    transactions: List<HistoryTransactionItem> = emptyList(),
    onTransactionClick: (HistoryTransactionItem) -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTypeFilter by rememberSaveable { mutableStateOf<TransactionType?>(null) }

    val filteredTransactions = remember(transactions, query, selectedTypeFilter) {
        transactions.filter { item ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val needle = query.trim().lowercase()
                listOfNotNull(
                    item.category,
                    item.amount,
                    item.date,
                    item.month,
                    item.note,
                    item.type.name
                ).any { value -> value.lowercase().contains(needle) }
            }

            val matchesType = selectedTypeFilter == null || item.type == selectedTypeFilter

            matchesQuery && matchesType
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { historyGroupLabel(it) }
    }

    val summaryStats = remember(filteredTransactions) {
        val income = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { parseAmount(it.amount) }
        val expense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { parseAmount(it.amount) }
        Pair(income, expense)
    }

    AppScaffold(
        title = "Ledger",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Text(
                text = "Ledger.",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Summary card
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Income", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "+${formatHistoryMoney(summaryStats.first)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Expense", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "-${formatHistoryMoney(summaryStats.second)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        Text("Net:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            formatHistoryMoney(summaryStats.first - summaryStats.second),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search transactions...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Type filter buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.INCOME,
                    onClick = { selectedTypeFilter = TransactionType.INCOME },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.EXPENSE,
                    onClick = { selectedTypeFilter = TransactionType.EXPENSE },
                    label = { Text("Expense") }
                )
            }

            if (groupedTransactions.isEmpty()) {
                EmptyStateBlock(
                    title = "No transactions found",
                    subtitle = if (transactions.isEmpty()) {
                        "Add a transaction to build your Firebase ledger."
                    } else {
                        "Try a different keyword."
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
                ) {
                    groupedTransactions.forEach { (sectionTitle, itemsInSection) ->
                        item(key = "header_$sectionTitle") {
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = AppDimens.spaceSm)
                            )
                        }

                        item(key = "section_$sectionTitle") {
                            AppCard {
                                Column {
                                    itemsInSection.forEachIndexed { index, item ->
                                        HistoryTransactionRow(
                                            item = item,
                                            onClick = { onTransactionClick(item) }
                                        )
                                        if (index != itemsInSection.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun historyGroupLabel(item: HistoryTransactionItem): String {
    val dateLower = item.date.lowercase()
    return when {
        dateLower.contains("today") -> "TODAY"
        dateLower.contains("yesterday") -> "YESTERDAY"
        else -> item.month.uppercase()
    }
}

private fun parseAmount(amountStr: String): Double {
    return amountStr.replace(Regex("[^\\d.-]"), "").toDoubleOrNull() ?: 0.0
}

private fun formatHistoryMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} VND"
}

@Composable
private fun HistoryTransactionRow(
    item: HistoryTransactionItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AppDimens.spaceMd),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    shape = MaterialTheme.shapes.large
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.categoryIcon,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
        ) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.note ?: item.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.amount,
                style = MaterialTheme.typography.titleLarge,
                color = if (item.type == TransactionType.INCOME) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = if (item.type == TransactionType.INCOME) "RECEIVED" else "PERSONAL",
                style = MaterialTheme.typography.labelSmall,
                color = if (item.type == TransactionType.INCOME) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistoryScreenPreview() {
    DineSplitTheme {
        HistoryScreen(onBack = {})
    }
}
