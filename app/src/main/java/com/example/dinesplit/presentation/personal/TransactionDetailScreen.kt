package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    transaction: Transaction?,
    onBack: () -> Unit
) {
    AppScaffold(
        title = "Transaction Detail",
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
            if (transaction == null) {
                AppCard {
                    Text(
                        text = "Transaction not found: $transactionId",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        Text(text = transaction.category, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = formatDetailAmount(transaction),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(text = "Type: ${transaction.type.displayLabel()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Date: ${formatDateTime(transaction.date)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!transaction.note.isNullOrBlank()) {
                            Text(text = "Note: ${transaction.note}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(text = "Id: ${transaction.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

private fun formatDetailAmount(transaction: Transaction): String {
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
    return "$sign${String.format(Locale.US, "%,.0f", transaction.amount).replace(',', '.')}đ"
}

private fun formatDateTime(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionDetailScreenPreview() {
    DineSplitTheme {
        TransactionDetailScreen(
            transactionId = "tx_1",
            transaction = Transaction(
                id = "tx_1",
                userId = "preview_user",
                amount = 525000.0,
                type = TransactionType.EXPENSE,
                categoryId = "c_food",
                category = "Dining Out",
                note = "Dinner with team",
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            ),
            onBack = {}
        )
    }
}
