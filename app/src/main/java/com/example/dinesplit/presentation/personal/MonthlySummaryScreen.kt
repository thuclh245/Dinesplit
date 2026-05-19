package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Monthly summary screen showing income, expense, and balance for a specific month.
 * Part of week 2 deliverables.
 */
@Composable
fun MonthlySummaryScreen(
    onBack: () -> Unit,
    summary: MonthlySummary = MonthlySummary(0.0, 0.0, 0.0),
    categorySpending: List<PieCategorySlice> = emptyList(),
    month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    year: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(
        Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.YEAR, year)
        }.time
    )

    AppScaffold(
        title = "Monthly Summary",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            // Month header
            Text(
                text = "$monthName $year",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal)
            )

            // Main summary card
            AppCard(
                modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)) {
                    // Balance
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
                        Text(
                            text = "Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(summary.balance),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.balance >= 0.0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    // Income and Expense pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                    ) {
                        MetricPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.ArrowDownward,
                            title = "Income",
                            amount = summary.totalIncome,
                            color = MaterialTheme.colorScheme.primary
                        )
                        MetricPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.ArrowUpward,
                            title = "Expense",
                            amount = summary.totalExpense,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Category breakdown
            if (categorySpending.isNotEmpty()) {
                Text(
                    text = "Spending Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal)
                )

                AppCard(
                    modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                        categorySpending.forEach { slice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppDimens.spaceMd),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slice.category,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${slice.percentage.toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatCurrency(slice.amount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} VND"
}

@Composable
private fun MetricPill(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color
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
                    text = formatCurrency(amount),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

