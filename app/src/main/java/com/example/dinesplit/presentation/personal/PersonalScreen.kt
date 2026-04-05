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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.dinesplit.core.ui.StatCard
import com.example.dinesplit.ui.theme.DineSplitTheme

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
        fun default() = HasData(
            summary = PersonalDashboardSummary(
                totalIncome = "3.500.000đ",
                totalExpense = "1.250.000đ",
                balance = "2.250.000đ",
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
            monthlySummary = MonthlySummary(
                totalIncome = 3500000.0,
                totalExpense = 1250000.0,
                balance = 2250000.0
            )
        )
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
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCategoryManagement: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            Text(
                text = "Monthly overview",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Quick snapshot of your personal finance this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        StatCard(
            title = "Total income",
            value = summary.totalIncome,
            subtitle = "Money received this month"
        )

        StatCard(
            title = "Total expense",
            value = summary.totalExpense,
            subtitle = "Money spent this month"
        )

        StatCard(
            title = "Balance",
            value = summary.balance,
            subtitle = summary.balanceNote
        )

        AppCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                Text(
                    text = "Category breakdown",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Spending percentage by category this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PersonalPieChart(
                    slices = pieChartData,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    categoryBreakdowns.forEach { item ->
                        CategoryBreakdownRow(item = item)
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

        Spacer(modifier = Modifier.height(AppDimens.spaceXs))
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
