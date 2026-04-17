package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.util.Locale

sealed interface PersonalDashboardUiState {
    data object Loading : PersonalDashboardUiState
    data object Empty : PersonalDashboardUiState

    data class HasData(
        val summary: PersonalDashboardSummary,
        val categoryBreakdowns: List<CategoryBreakdown>,
        val pieChartData: List<PieCategorySlice>,
        val dailyExpenseBars: List<DailyExpenseBar>,
        val monthlySummary: MonthlySummary
    ) : PersonalDashboardUiState

    companion object {
        fun default(): HasData {
            val monthlySummary = MonthlySummary(
                totalIncome = 3500000.0,
                totalExpense = 1250000.0,
                balance = 2250000.0
            )

            return HasData(
                summary = monthlySummary.toDashboardSummary(
                    balanceNote = "+18% compared with last month"
                ),
                categoryBreakdowns = listOf(
                    CategoryBreakdown(name = "Food", percentage = "42%", amount = "525.000đ", progress = 0.42f),
                    CategoryBreakdown(name = "Drink", percentage = "18%", amount = "225.000đ", progress = 0.18f),
                    CategoryBreakdown(name = "Travel", percentage = "15%", amount = "187.500đ", progress = 0.15f)
                ),
                pieChartData = listOf(
                    PieCategorySlice(category = "Food", amount = 525000.0, percentage = 0.42f),
                    PieCategorySlice(category = "Drink", amount = 225000.0, percentage = 0.18f),
                    PieCategorySlice(category = "Travel", amount = 187500.0, percentage = 0.15f)
                ),
                dailyExpenseBars = listOf(
                    DailyExpenseBar(dayOfMonth = 1, amount = 120000.0),
                    DailyExpenseBar(dayOfMonth = 3, amount = 75000.0),
                    DailyExpenseBar(dayOfMonth = 5, amount = 90000.0),
                    DailyExpenseBar(dayOfMonth = 9, amount = 55000.0)
                ),
                monthlySummary = monthlySummary
            )
        }
    }
}

data class PersonalDashboardSummary(
    val totalIncome: String,
    val totalExpense: String,
    val balance: String,
    val balanceNote: String
)

data class CategoryBreakdown(
    val name: String,
    val percentage: String,
    val amount: String,
    val progress: Float
)

private fun MonthlySummary.toDashboardSummary(
    balanceNote: String
): PersonalDashboardSummary {
    return PersonalDashboardSummary(
        totalIncome = formatCurrencyVnd(totalIncome),
        totalExpense = formatCurrencyVnd(totalExpense),
        balance = formatCurrencyVnd(balance),
        balanceNote = balanceNote
    )
}

private fun formatCurrencyVnd(amount: Double): String {
    val grouped = String.format(Locale.US, "%,d", amount.toLong())
    return grouped.replace(',', '.') + "đ"
}

@Composable
fun PersonalScreen(
    onOpenAssistant: () -> Unit,
    onAddExpense: () -> Unit = {},
    onAddIncome: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenCategoryManagement: () -> Unit = {},
    uiState: PersonalDashboardUiState = PersonalDashboardUiState.default()
) {
    AppScaffold(
        title = "Personal",
        actions = {
            TextButton(onClick = onOpenAssistant) {
                Text("AI")
            }
        }
    ) {
        when (uiState) {
            PersonalDashboardUiState.Loading -> LoadingBlock(message = "Loading personal finance...")
            PersonalDashboardUiState.Empty -> PersonalDashboardEmptyContent(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                onAddExpense = onAddExpense,
                onAddIncome = onAddIncome,
                onOpenHistory = onOpenHistory,
                onOpenCategoryManagement = onOpenCategoryManagement
            )
            is PersonalDashboardUiState.HasData -> PersonalDashboardContent(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                summary = uiState.summary,
                categoryBreakdowns = uiState.categoryBreakdowns,
                pieChartData = uiState.pieChartData,
                dailyExpenseBars = uiState.dailyExpenseBars,
                onAddExpense = onAddExpense,
                onAddIncome = onAddIncome,
                onOpenHistory = onOpenHistory,
                onOpenCategoryManagement = onOpenCategoryManagement
            )
        }
    }
}

@Composable
private fun PersonalDashboardContent(
    modifier: Modifier = Modifier,
    summary: PersonalDashboardSummary,
    categoryBreakdowns: List<CategoryBreakdown>,
    pieChartData: List<PieCategorySlice>,
    dailyExpenseBars: List<DailyExpenseBar>,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCategoryManagement: () -> Unit
) {
    val recentActivity = rememberRecentActivity(summary)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
    ) {
        AppCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                Text(
                    text = "Total wealth",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary.balance,
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = summary.balanceNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            SummaryInfoCard(
                modifier = Modifier.weight(1f),
                title = "Income",
                value = summary.totalIncome
            )

            SummaryInfoCard(
                modifier = Modifier.weight(1f),
                title = "Spent",
                value = summary.totalExpense
            )
        }

        AppCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                Text(
                    text = "Cash Flow",
                    style = MaterialTheme.typography.titleMedium
                )

                PersonalDailyExpenseBarChart(
                    bars = dailyExpenseBars,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        AppCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium
                )

                recentActivity.forEachIndexed { index, item ->
                    RecentActivityRow(item = item)
                    if (index != recentActivity.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }

        AppCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                Text(
                    text = "Quick actions",
                    style = MaterialTheme.typography.titleMedium
                )

                PrimaryButton(
                    text = "+ Add Expense",
                    onClick = onAddExpense
                )

                SecondaryButton(
                    text = "+ Add Income",
                    onClick = onAddIncome
                )

                TextButton(onClick = onOpenHistory) {
                    Text("View Transaction History")
                }

                TextButton(onClick = onOpenCategoryManagement) {
                    Text("Manage Categories")
                }
            }
        }

        if (categoryBreakdowns.isNotEmpty() || pieChartData.isNotEmpty()) {
            AppCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (pieChartData.isNotEmpty()) {
                        PersonalPieChart(
                            slices = pieChartData,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    categoryBreakdowns.forEach { item ->
                        CategoryBreakdownRow(item = item)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceXs))
    }
}

@Composable
private fun SummaryInfoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    AppCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

private data class RecentActivityUi(
    val icon: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val isIncome: Boolean
)

@Composable
private fun rememberRecentActivity(
    summary: PersonalDashboardSummary
): List<RecentActivityUi> {
    return remember(summary) {
        listOf(
            RecentActivityUi(
                icon = "FD",
                title = "Dinner with team",
                subtitle = "Dining • Today",
                amount = "-124,500",
                isIncome = false
            ),
            RecentActivityUi(
                icon = "SL",
                title = "Salary deposit",
                subtitle = "Income • Yesterday",
                amount = "+${summary.totalIncome}",
                isIncome = true
            ),
            RecentActivityUi(
                icon = "TR",
                title = "Taxi ride",
                subtitle = "Transport • 2 days ago",
                amount = "-24,000",
                isIncome = false
            )
        )
    }
}

@Composable
private fun RecentActivityRow(
    item: RecentActivityUi
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.icon,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = item.amount,
            style = MaterialTheme.typography.titleSmall,
            color = if (item.isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PersonalDashboardEmptyContent(
    modifier: Modifier = Modifier,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCategoryManagement: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
    ) {
        AppCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                Text(
                    text = "No transactions yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Start by adding your first income or expense. The dashboard will show summary, balance, and category spending here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PrimaryButton(
                    text = "+ Add Expense",
                    onClick = onAddExpense
                )

                SecondaryButton(
                    text = "+ Add Income",
                    onClick = onAddIncome
                )

                TextButton(onClick = onOpenHistory) {
                    Text("Open Transaction History")
                }

                TextButton(onClick = onOpenCategoryManagement) {
                    Text("Open Category Management")
                }
            }
        }

        AppCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                Text(
                    text = "What you'll see here",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Income, expense, balance, and a simple chart once you have data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    item: CategoryBreakdown
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${item.percentage} • ${item.amount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PersonalDashboardPreview() {
    DineSplitTheme {
        PersonalScreen(
            onOpenAssistant = {},
            uiState = PersonalDashboardUiState.default()
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PersonalDashboardLoadingPreview() {
    DineSplitTheme {
        PersonalScreen(
            onOpenAssistant = {},
            uiState = PersonalDashboardUiState.Loading
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PersonalDashboardEmptyPreview() {
    DineSplitTheme {
        PersonalScreen(
            onOpenAssistant = {},
            uiState = PersonalDashboardUiState.Empty
        )
    }
}
