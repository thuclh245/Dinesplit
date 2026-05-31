package com.example.dinesplit.presentation.personal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.HomeTopBar
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionSource
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
    chartState: PersonalChartState = PersonalChartState(),
    reminderCount: Int = 0,
    bottomPadding: Dp = 80.dp,
    onOpenSearch: () -> Unit,
    onAddTransaction: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenMonthlySummary: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenPlans: () -> Unit = {},
    onOpenRecurringPlans: () -> Unit = onOpenPlans,
    onOpenGoalPlans: () -> Unit = onOpenPlans,
    onOpenWalletPlans: () -> Unit = onOpenPlans,
    onRefresh: () -> Unit = {}
) {
    val expenseSlices = remember(chartState.pieSlices) {
        chartState.pieSlices
    }
    val dailyBars = remember(chartState.dailyExpenseBars) {
        chartState.dailyExpenseBars
    }
    val automationCount = uiState.recurringRules.size + uiState.goals.size + uiState.wallets.size
    val personalScore = remember(
        chartState.monthlySummary,
        chartState.safeToSpend,
        chartState.insights,
        reminderCount,
        automationCount,
        uiState.transactions.size
    ) {
        buildPersonalScore(
            summary = chartState.monthlySummary,
            forecast = chartState.safeToSpend,
            insights = chartState.insights,
            reminderCount = reminderCount,
            automationCount = automationCount,
            transactionCount = uiState.transactions.size
        )
    }
    val monthMarker = remember { currentMonthMarker() }
    val topCategory = remember(expenseSlices) {
        expenseSlices.maxByOrNull { it.amount }
    }
    val anomalySignals = remember(uiState.transactions, chartState.monthlySummary, monthMarker) {
        buildAnomalySignals(
            transactions = uiState.transactions,
            summary = chartState.monthlySummary,
            monthMarker = monthMarker
        )
    }
    val prioritySignals = remember(anomalySignals) {
        anomalySignals
            .filter { it.tone != AdvancedSignalTone.POSITIVE && it.metric != "Idle" }
            .take(3)
    }
    val autopilotActions = remember(
        uiState.transactions,
        uiState.recurringRules,
        uiState.goals,
        uiState.wallets,
        reminderCount,
        topCategory,
        personalScore
    ) {
        buildAutopilotActions(
            uiState = uiState,
            topCategory = topCategory,
            reminderCount = reminderCount,
            score = personalScore
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = bottomPadding)
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
                top = 72.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            if (uiState.isLoading) {
                item {
                    LoadingBlock(message = "Đang tải dữ liệu tài chính...")
                }
            } else {
                uiState.errorMessage?.let { message ->
                    item {
                        ErrorStateBlock(
                            title = "Không thể tải dữ liệu",
                            subtitle = message,
                            onRetryClick = onRefresh
                        )
                    }
                }

                item {
                    MonthlyCommandCard(
                        balance = chartState.monthlySummary.balance,
                        income = chartState.monthlySummary.totalIncome,
                        expense = chartState.monthlySummary.totalExpense,
                        forecast = chartState.safeToSpend,
                        score = personalScore,
                        monthMarker = monthMarker
                    )
                }

                item {
                    PersonalSignalGrid(
                        score = personalScore,
                        forecast = chartState.safeToSpend,
                        topCategory = topCategory,
                        reminderCount = reminderCount,
                        automationCount = automationCount
                    )
                }

                if (prioritySignals.isNotEmpty()) {
                    item {
                        PriorityAlertSection(
                            signals = prioritySignals,
                            onOpenHistory = onOpenHistory,
                            onOpenReminders = onOpenReminders
                        )
                    }
                }

                if (autopilotActions.isNotEmpty()) {
                    item {
                        AutopilotQueue(
                            actions = autopilotActions,
                            onOpenHistory = onOpenHistory,
                            onOpenReminders = onOpenReminders,
                            onOpenRecurringPlans = onOpenRecurringPlans,
                            onOpenGoalPlans = onOpenGoalPlans,
                            onOpenWalletPlans = onOpenWalletPlans
                        )
                    }
                }

                item {
                    PersonalActionGrid(
                        transactionCount = uiState.transactions.size,
                        categoryCount = uiState.categories.size,
                        reminderCount = reminderCount,
                        planCount = automationCount,
                        onOpenHistory = onOpenHistory,
                        onOpenMonthlySummary = onOpenMonthlySummary,
                        onOpenCategories = onOpenCategories,
                        onOpenReminders = onOpenReminders,
                        onOpenInsights = onOpenInsights,
                        onOpenPlans = onOpenPlans
                    )
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        EmptyStateBlock(
                            title = "Chưa có giao dịch nào",
                            subtitle = "Thêm khoản thu hoặc chi để bắt đầu theo dõi tài chính của bạn.",
                            actionText = "Thêm giao dịch",
                            onActionClick = onAddTransaction
                        )
                    }
                } else {
                    item {
                        SpendingSnapshotCard(
                            expenseSlices = expenseSlices,
                            dailyBars = dailyBars,
                            onOpenHistory = onOpenHistory
                        )
                    }

                    item {
                        SectionHeader(title = "Recent transactions", actionLabel = "View all", onAction = onOpenHistory)
                    }

                    items(uiState.transactions.take(3), key = { it.id }) { transaction ->
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
private fun MonthlyCommandCard(
    balance: Double,
    income: Double,
    expense: Double,
    forecast: SafeToSpendForecast,
    score: PersonalScore,
    monthMarker: MonthMarker
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val scoreColor = scoreBandColor(score.band)
    val runwayProgress = animateFloatAsState(
        targetValue = monthMarker.progress,
        animationSpec = tween(durationMillis = 700),
        label = "monthRunway"
    ).value

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.xLarge,
        color = Color.Transparent,
        shadowElevation = AppDimens.cardElevation
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = AppShapes.xLarge
                )
                .padding(AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
                ) {
                    Text(
                        text = "Monthly command center",
                        style = MaterialTheme.typography.labelMedium,
                        color = onAccent.copy(alpha = 0.76f)
                    )
                    Text(
                        text = formatMoney(balance),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = onAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Safe today ${formatMoney(forecast.dailyAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = onAccent.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        HeroSticker(
                            icon = Icons.Default.Flag,
                            label = score.label,
                            contentColor = onAccent
                        )
                        HeroSticker(
                            icon = Icons.Default.Savings,
                            label = forecast.status.name.lowercase().replaceFirstChar { it.uppercase() },
                            contentColor = onAccent
                        )
                    }
                }

                Surface(
                    shape = AppShapes.full,
                    color = scoreColor.copy(alpha = 0.22f)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AppDimens.spaceMd,
                            vertical = AppDimens.spaceSm
                        ),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = onAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${score.value}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = onAccent
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                CommandMetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowDownward,
                    title = "Income",
                    amount = income,
                    contentColor = onAccent
                )
                CommandMetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowUpward,
                    title = "Expense",
                    amount = expense,
                    contentColor = onAccent
                )
            }

            MonthRunwayBar(
                progress = runwayProgress,
                label = monthMarker.label,
                contentColor = onAccent
            )
        }
    }
}

@Composable
private fun HeroSticker(
    icon: ImageVector,
    label: String,
    contentColor: Color
) {
    Surface(
        shape = AppShapes.full,
        color = contentColor.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.spaceSm,
                vertical = AppDimens.spaceXs
            ),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CommandMetricPill(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    amount: Double,
    contentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = contentColor.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.76f)
                )
                Text(
                    text = formatMoney(amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MonthRunwayBar(
    progress: Float,
    label: String,
    contentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f)
            )
            Text(
                text = "Month runway",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f)
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = contentColor,
            trackColor = contentColor.copy(alpha = 0.24f)
        )
    }
}

@Composable
private fun PersonalSignalGrid(
    score: PersonalScore,
    forecast: SafeToSpendForecast,
    topCategory: PieCategorySlice?,
    reminderCount: Int,
    automationCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
        SectionHeader(title = "Financial signals")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            SignalTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AutoGraph,
                label = "Personal score",
                value = score.value.toString(),
                subtitle = score.label,
                color = scoreBandColor(score.band)
            )
            SignalTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Savings,
                label = "Runway",
                value = formatMoney(forecast.dailyAmount),
                subtitle = "${forecast.daysLeft} days left",
                color = forecastStatusColor(forecast.status)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            SignalTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Category,
                label = "Top spend",
                value = topCategory?.category ?: "No data",
                subtitle = topCategory?.let { formatMoney(it.amount) } ?: "Add expenses",
                color = MaterialTheme.colorScheme.tertiary
            )
            SignalTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.NotificationsActive,
                label = "C engine",
                value = "${reminderCount + automationCount}",
                subtitle = "alerts and plans",
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun SignalTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    color: Color
) {
    AppCard(
        modifier = modifier,
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .padding(AppDimens.spaceSm)
                            .size(18.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PriorityAlertSection(
    signals: List<AnomalySignal>,
    onOpenHistory: () -> Unit,
    onOpenReminders: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            SectionHeader(title = "Priority alerts")
            Text(
                text = "Review these before adding more spending.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            signals.forEach { signal ->
                IntelligenceSignalRow(
                    icon = Icons.Default.WarningAmber,
                    title = signal.title,
                    message = signal.message,
                    metric = signal.metric,
                    tone = signal.tone,
                    actionLabel = signal.actionLabel,
                    onAction = {
                        when (signal.target) {
                            PersonalActionTarget.HISTORY -> onOpenHistory()
                            PersonalActionTarget.REMINDERS -> onOpenReminders()
                            else -> Unit
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AutopilotQueue(
    actions: List<AutopilotAction>,
    onOpenHistory: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenRecurringPlans: () -> Unit,
    onOpenGoalPlans: () -> Unit,
    onOpenWalletPlans: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            SectionHeader(title = "Autopilot queue")
            Text(
                text = "Concrete setup steps ranked by priority.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            actions.forEach { action ->
                IntelligenceSignalRow(
                    icon = Icons.Default.Lightbulb,
                    title = action.title,
                    message = action.message,
                    metric = action.priority,
                    tone = action.tone,
                    actionLabel = action.actionLabel,
                    onAction = {
                        when (action.target) {
                            PersonalActionTarget.HISTORY -> onOpenHistory()
                            PersonalActionTarget.REMINDERS -> onOpenReminders()
                            PersonalActionTarget.RECURRING_PLANS -> onOpenRecurringPlans()
                            PersonalActionTarget.GOALS -> onOpenGoalPlans()
                            PersonalActionTarget.WALLETS -> onOpenWalletPlans()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun IntelligenceSignalRow(
    icon: ImageVector,
    title: String,
    message: String,
    metric: String,
    tone: AdvancedSignalTone,
    actionLabel: String?,
    onAction: () -> Unit
) {
    val color = advancedToneColor(tone)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(AppDimens.spaceSm)
                        .size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = metric,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!actionLabel.isNullOrBlank()) {
                    TextButton(
                        onClick = onAction,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalActionGrid(
    transactionCount: Int,
    categoryCount: Int,
    reminderCount: Int,
    planCount: Int,
    onOpenHistory: () -> Unit,
    onOpenMonthlySummary: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenPlans: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
        SectionHeader(title = "Manage")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.History,
                label = "Ledger",
                value = "$transactionCount entries",
                color = MaterialTheme.colorScheme.primary,
                onClick = onOpenHistory
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AutoGraph,
                label = "Insights",
                value = "Radar and simulator",
                color = MaterialTheme.colorScheme.secondary,
                onClick = onOpenInsights
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Savings,
                label = "Summary",
                value = "Monthly report",
                color = MaterialTheme.colorScheme.tertiary,
                onClick = onOpenMonthlySummary
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Category,
                label = "Categories",
                value = "$categoryCount active",
                color = MaterialTheme.colorScheme.primary,
                onClick = onOpenCategories
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.NotificationsActive,
                label = "Reminders",
                value = "$reminderCount active",
                color = MaterialTheme.colorScheme.error,
                onClick = onOpenReminders
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EventRepeat,
                label = "Plans",
                value = "$planCount active",
                color = MaterialTheme.colorScheme.secondary,
                onClick = onOpenPlans
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    AppCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(AppDimens.spaceSm)
                        .size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SpendingSnapshotCard(
    expenseSlices: List<PieCategorySlice>,
    dailyBars: List<DailyExpenseBar>,
    onOpenHistory: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            SectionHeader(
                title = "Spending snapshot",
                actionLabel = "View ledger",
                onAction = onOpenHistory
            )
            if (expenseSlices.isEmpty() && dailyBars.isEmpty()) {
                Text(
                    text = "No expense chart yet. Add expense entries to unlock category and daily spend views.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (expenseSlices.isNotEmpty()) {
                    PersonalPieChart(slices = expenseSlices)
                }
                if (dailyBars.isNotEmpty()) {
                    PersonalDailyExpenseBarChart(bars = dailyBars)
                }
            }
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
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
    val amountColor = if (transaction.type == TransactionType.INCOME) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    AppCard(contentPadding = PaddingValues(AppDimens.spaceMd)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = amountColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = transaction.category.take(2).uppercase(),
                    modifier = Modifier.padding(AppDimens.spaceSm),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.source != TransactionSource.MANUAL) {
                        SourceBadge(source = transaction.source)
                    }
                }
                Text(
                    text = listOfNotNull(formatDate(transaction.date), transaction.note).joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatSignedMoney(transaction),
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SourceBadge(source: TransactionSource) {
    Surface(
        shape = AppShapes.full,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    ) {
        Text(
            text = source.displayLabel(),
            modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1
        )
    }
}

private fun formatSignedMoney(transaction: Transaction): String {
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
    return "$sign${formatMoney(transaction.amount)}"
}

private fun formatMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} VND"
}

private fun formatDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

private data class PersonalScore(
    val value: Int,
    val band: PersonalScoreBand,
    val label: String
)

private enum class PersonalScoreBand {
    EXCELLENT,
    STABLE,
    WATCH,
    RISK
}

private data class MonthMarker(
    val label: String,
    val progress: Float
)

private fun buildPersonalScore(
    summary: MonthlySummary,
    forecast: SafeToSpendForecast,
    insights: List<PersonalInsight>,
    reminderCount: Int,
    automationCount: Int,
    transactionCount: Int
): PersonalScore {
    if (transactionCount == 0) {
        return PersonalScore(
            value = 20,
            band = PersonalScoreBand.RISK,
            label = "Needs data"
        )
    }

    var score = 48
    val spendRatio = if (summary.totalIncome > 0.0) {
        summary.totalExpense / summary.totalIncome
    } else {
        Double.POSITIVE_INFINITY
    }

    score += when {
        summary.totalIncome <= 0.0 -> -8
        spendRatio < 0.65 -> 20
        spendRatio < 0.85 -> 14
        spendRatio <= 1.0 -> 4
        else -> -16
    }

    score += when (forecast.status) {
        SafeToSpendStatus.HEALTHY -> 16
        SafeToSpendStatus.WATCH -> 5
        SafeToSpendStatus.OVER -> -14
    }

    if (reminderCount > 0) score += 7
    if (automationCount > 0) score += 7
    if (transactionCount >= 8) score += 5
    score -= insights.count { it.tone == PersonalInsightTone.WARNING } * 6

    val normalized = score.coerceIn(0, 100)
    val band = when {
        normalized >= 82 -> PersonalScoreBand.EXCELLENT
        normalized >= 64 -> PersonalScoreBand.STABLE
        normalized >= 42 -> PersonalScoreBand.WATCH
        else -> PersonalScoreBand.RISK
    }
    val label = when (band) {
        PersonalScoreBand.EXCELLENT -> "Excellent control"
        PersonalScoreBand.STABLE -> "Stable month"
        PersonalScoreBand.WATCH -> "Watch spending"
        PersonalScoreBand.RISK -> "Needs attention"
    }

    return PersonalScore(
        value = normalized,
        band = band,
        label = label
    )
}

private fun currentMonthMarker(): MonthMarker {
    val calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    return MonthMarker(
        label = "Day $day of $maxDay",
        progress = (day.toFloat() / maxDay.toFloat()).coerceIn(0f, 1f)
    )
}

@Composable
private fun scoreBandColor(band: PersonalScoreBand): Color {
    return when (band) {
        PersonalScoreBand.EXCELLENT -> MaterialTheme.colorScheme.secondary
        PersonalScoreBand.STABLE -> MaterialTheme.colorScheme.primary
        PersonalScoreBand.WATCH -> MaterialTheme.colorScheme.tertiary
        PersonalScoreBand.RISK -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun forecastStatusColor(status: SafeToSpendStatus): Color {
    return when (status) {
        SafeToSpendStatus.HEALTHY -> MaterialTheme.colorScheme.secondary
        SafeToSpendStatus.WATCH -> MaterialTheme.colorScheme.tertiary
        SafeToSpendStatus.OVER -> MaterialTheme.colorScheme.error
    }
}

private fun TransactionSource.displayLabel(): String {
    return when (this) {
        TransactionSource.MANUAL -> "Manual"
        TransactionSource.SPLIT -> "Split"
        TransactionSource.RECURRING -> "Recurring"
        TransactionSource.RECEIPT -> "Receipt"
    }
}

private data class AnomalySignal(
    val title: String,
    val message: String,
    val metric: String,
    val tone: AdvancedSignalTone,
    val actionLabel: String,
    val target: PersonalActionTarget
)

private data class AutopilotAction(
    val title: String,
    val message: String,
    val priority: String,
    val tone: AdvancedSignalTone,
    val actionLabel: String,
    val target: PersonalActionTarget
)

private enum class AdvancedSignalTone {
    POSITIVE,
    INFO,
    WARNING,
    DANGER
}

private enum class PersonalActionTarget {
    HISTORY,
    REMINDERS,
    RECURRING_PLANS,
    GOALS,
    WALLETS
}

private fun buildAnomalySignals(
    transactions: List<Transaction>,
    summary: MonthlySummary,
    monthMarker: MonthMarker
): List<AnomalySignal> {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    if (expenses.isEmpty()) {
        return listOf(
            AnomalySignal(
                title = "No anomaly yet",
                message = "Add expenses to let the radar compare outliers, pace, and split impact.",
                metric = "Idle",
                tone = AdvancedSignalTone.INFO,
                actionLabel = "Open ledger",
                target = PersonalActionTarget.HISTORY
            )
        )
    }

    val signals = mutableListOf<AnomalySignal>()
    val averageExpense = expenses.map { it.amount }.average().takeUnless { it.isNaN() } ?: 0.0
    val largestExpense = expenses.maxByOrNull { it.amount }
    if (largestExpense != null && averageExpense > 0.0 && largestExpense.amount >= averageExpense * 1.8) {
        signals += AnomalySignal(
            title = "Outlier transaction",
            message = "${largestExpense.category} is ${formatRatio(largestExpense.amount / averageExpense)}x higher than your average expense.",
            metric = formatMoney(largestExpense.amount),
            tone = AdvancedSignalTone.WARNING,
            actionLabel = "Review history",
            target = PersonalActionTarget.HISTORY
        )
    }

    val topCategory = expenses
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .maxByOrNull { it.value }
    if (topCategory != null && summary.totalExpense > 0.0) {
        val categoryShare = topCategory.value / summary.totalExpense
        if (categoryShare >= 0.45) {
            signals += AnomalySignal(
                title = "Category concentration",
                message = "${topCategory.key} owns ${(categoryShare * 100).toInt()}% of this month's expense.",
                metric = "${(categoryShare * 100).toInt()}%",
                tone = AdvancedSignalTone.WARNING,
                actionLabel = "Add reminder",
                target = PersonalActionTarget.REMINDERS
            )
        }
    }

    val splitExpense = expenses
        .filter { it.source == TransactionSource.SPLIT }
        .sumOf { it.amount }
    if (summary.totalExpense > 0.0 && splitExpense / summary.totalExpense >= 0.35) {
        signals += AnomalySignal(
            title = "Split-heavy month",
            message = "Split bills are driving ${(splitExpense / summary.totalExpense * 100).toInt()}% of your expense.",
            metric = formatMoney(splitExpense),
            tone = AdvancedSignalTone.INFO,
            actionLabel = "Review ledger",
            target = PersonalActionTarget.HISTORY
        )
    }

    val dailyTotals = expenses
        .groupBy { transactionDayOfMonth(it.date) }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toSortedMap()
        .values
        .toList()
        .takeLast(3)
    if (
        dailyTotals.size == 3 &&
        dailyTotals[0] < dailyTotals[1] &&
        dailyTotals[1] < dailyTotals[2] &&
        monthMarker.progress > 0.2f
    ) {
        signals += AnomalySignal(
            title = "Three-day climb",
            message = "Your daily expense increased three tracked days in a row.",
            metric = "3d",
            tone = AdvancedSignalTone.DANGER,
            actionLabel = "Set guardrail",
            target = PersonalActionTarget.REMINDERS
        )
    }

    return signals.take(3).ifEmpty {
        listOf(
            AnomalySignal(
                title = "Radar clean",
                message = "No outlier, concentration, or rising-streak signal detected this month.",
                metric = "OK",
                tone = AdvancedSignalTone.POSITIVE,
                actionLabel = "Open ledger",
                target = PersonalActionTarget.HISTORY
            )
        )
    }
}

private fun buildAutopilotActions(
    uiState: PersonalUiState,
    topCategory: PieCategorySlice?,
    reminderCount: Int,
    score: PersonalScore
): List<AutopilotAction> {
    val actions = mutableListOf<AutopilotAction>()
    val expenses = uiState.transactions.filter { it.type == TransactionType.EXPENSE }
    val totalExpense = expenses.sumOf { it.amount }
    val splitExpense = expenses
        .filter { it.source == TransactionSource.SPLIT }
        .sumOf { it.amount }

    if (score.band == PersonalScoreBand.RISK || score.band == PersonalScoreBand.WATCH) {
        actions += AutopilotAction(
            title = "Run defensive month",
            message = "Your score band suggests using a stricter reminder before adding more plans.",
            priority = "High",
            tone = AdvancedSignalTone.DANGER,
            actionLabel = "Open reminders",
            target = PersonalActionTarget.REMINDERS
        )
    }

    if (reminderCount == 0 && topCategory != null) {
        actions += AutopilotAction(
            title = "Guard ${topCategory.category}",
            message = "Create a spending reminder for the category currently leading your expense.",
            priority = "High",
            tone = AdvancedSignalTone.WARNING,
            actionLabel = "Create guard",
            target = PersonalActionTarget.REMINDERS
        )
    }

    if (uiState.recurringRules.isEmpty()) {
        actions += AutopilotAction(
            title = "Set recurring bill",
            message = "Add rent, salary, subscriptions, or fixed bills to improve forecast accuracy.",
            priority = "Medium",
            tone = AdvancedSignalTone.INFO,
            actionLabel = "Add recurring",
            target = PersonalActionTarget.RECURRING_PLANS
        )
    }

    if (totalExpense > 0.0 && splitExpense / totalExpense >= 0.3) {
        actions += AutopilotAction(
            title = "Audit split impact",
            message = "Split bills are a large part of this month. Check whether all reimbursements are reflected.",
            priority = "Medium",
            tone = AdvancedSignalTone.INFO,
            actionLabel = "Review ledger",
            target = PersonalActionTarget.HISTORY
        )
    }

    if (uiState.goals.isEmpty()) {
        val goalSubject = topCategory?.category ?: "monthly spending"
        actions += AutopilotAction(
            title = "Create goal for $goalSubject",
            message = "Set a target around $goalSubject so the score reacts to the plan you actually want.",
            priority = "Medium",
            tone = AdvancedSignalTone.POSITIVE,
            actionLabel = "Create goal",
            target = PersonalActionTarget.GOALS
        )
    }

    if (uiState.wallets.isEmpty()) {
        actions += AutopilotAction(
            title = "Map wallet coverage",
            message = "Add cash, bank, e-wallet, or credit balances to improve the balance forecast.",
            priority = "Low",
            tone = AdvancedSignalTone.INFO,
            actionLabel = "Add wallet",
            target = PersonalActionTarget.WALLETS
        )
    }

    return actions.take(3)
}

@Composable
private fun advancedToneColor(tone: AdvancedSignalTone): Color {
    return when (tone) {
        AdvancedSignalTone.POSITIVE -> MaterialTheme.colorScheme.secondary
        AdvancedSignalTone.INFO -> MaterialTheme.colorScheme.primary
        AdvancedSignalTone.WARNING -> MaterialTheme.colorScheme.tertiary
        AdvancedSignalTone.DANGER -> MaterialTheme.colorScheme.error
    }
}

private fun transactionDayOfMonth(epochMillis: Long): Int {
    return Calendar.getInstance().apply { timeInMillis = epochMillis }
        .get(Calendar.DAY_OF_MONTH)
}

private fun formatRatio(value: Double): String {
    return String.format(Locale.US, "%.1f", value)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PersonalScreenPreview() {
    DineSplitTheme {
        PersonalScreen(
            userAvatarUrl = null,
            uiState = PersonalUiState(isLoading = false),
            chartState = PersonalChartState(),
            onOpenSearch = {},
            onAddTransaction = {}
        )
    }
}
