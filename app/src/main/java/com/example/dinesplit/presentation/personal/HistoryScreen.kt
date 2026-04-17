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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.ui.theme.DineSplitTheme

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
    transactions: List<HistoryTransactionItem> = defaultHistoryTransactions(),
    onTransactionClick: (HistoryTransactionItem) -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }

    val filteredTransactions = remember(transactions, query) {
        transactions.filter { item ->
            if (query.isBlank()) {
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
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { historyGroupLabel(it) }
    }

    AppScaffold(
        title = "Ledger",
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
            Text(
                text = "Ledger.",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

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

            if (groupedTransactions.isEmpty()) {
                EmptyStateBlock(
                    title = "No transactions found",
                    subtitle = "Try a different keyword."
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

private fun defaultHistoryTransactions(): List<HistoryTransactionItem> {
    return listOf(
        HistoryTransactionItem(
            id = "tx_1",
            categoryIcon = "FD",
            category = "The Continental",
            amount = "-84.50",
            date = "Today, 8:30 PM",
            month = "Apr 2026",
            type = TransactionType.EXPENSE,
            note = "Dinner with Sarah"
        ),
        HistoryTransactionItem(
            id = "tx_2",
            categoryIcon = "PM",
            category = "Sarah M.",
            amount = "+42.25",
            date = "Today, 9:00 PM",
            month = "Apr 2026",
            type = TransactionType.INCOME,
            note = "Venmo transfer"
        ),
        HistoryTransactionItem(
            id = "tx_3",
            categoryIcon = "CF",
            category = "Blue Bottle",
            amount = "-6.80",
            date = "Today, 7:45 AM",
            month = "Apr 2026",
            type = TransactionType.EXPENSE,
            note = "Morning coffee"
        ),
        HistoryTransactionItem(
            id = "tx_4",
            categoryIcon = "SP",
            category = "Whole Foods Market",
            amount = "-142.90",
            date = "Yesterday, 2:15 PM",
            month = "Apr 2026",
            type = TransactionType.EXPENSE,
            note = "Groceries"
        ),
        HistoryTransactionItem(
            id = "tx_5",
            categoryIcon = "TR",
            category = "Uber",
            amount = "-38.50",
            date = "Yesterday, 10:00 AM",
            month = "Apr 2026",
            type = TransactionType.EXPENSE,
            note = "Ride to airport"
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
