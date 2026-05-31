package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.NumberFormat
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
        title = "Chi tiết giao dịch",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            if (transaction == null) {
                AppCard {
                    Text(
                         text = "Không tìm thấy giao dịch: $transactionId",
                         style = MaterialTheme.typography.bodyLarge
                     )
                }
            } else {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = formatDetailAmount(transaction),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (transaction.type == TransactionType.INCOME) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                             text = "Loại: ${transaction.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                             style = MaterialTheme.typography.bodyMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                         Text(
                             text = "Ngày: ${formatDateTime(transaction.date)}",
                             style = MaterialTheme.typography.bodyMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                         if (!transaction.note.isNullOrBlank()) {
                             Text(
                                 text = "Ghi chú: ${transaction.note}",
                                 style = MaterialTheme.typography.bodyMedium
                             )
                         }
                        Text(
                            text = "Id: ${transaction.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

private fun formatDetailAmount(transaction: Transaction): String {
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "$sign${formatter.format(transaction.amount.toLong())} VND"
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
                userId = "user_1",
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
