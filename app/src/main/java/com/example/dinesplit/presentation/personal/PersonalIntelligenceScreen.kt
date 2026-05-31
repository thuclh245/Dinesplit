package com.example.dinesplit.presentation.personal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.domain.model.GoalStatus
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionSource
import com.example.dinesplit.domain.model.TransactionType
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun PersonalIntelligenceScreen(
    onBack: () -> Unit,
    uiState: PersonalUiState,
    chartState: PersonalChartState,
    reminderCount: Int,
    onOpenHistory: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenPlans: () -> Unit,
) {
    val monthMarker = remember { currentIntelligenceMonthMarker() }
    val anomalySignals =
        remember(uiState.transactions, chartState.monthlySummary, monthMarker) {
            buildIntelligenceAnomalySignals(
                transactions = uiState.transactions,
                summary = chartState.monthlySummary,
                monthMarker = monthMarker,
            )
        }
    var selectedScenario by rememberSaveable { mutableStateOf(CashflowScenario.DINNER_WEEKEND) }

    AppScaffold(
        title = "Personal Insights",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            Text(
                text = "Intelligence cockpit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )

            IntelligenceHeroCard(
                insightCount = chartState.insights.size,
                signalCount = anomalySignals.size,
                automationCount = uiState.recurringRules.size + uiState.goals.size + uiState.wallets.size + reminderCount,
                monthProgress = monthMarker.progress,
            )

            InsightSection(insights = chartState.insights)

            AnomalyRadarSection(
                signals = anomalySignals,
                onOpenHistory = onOpenHistory,
                onOpenReminders = onOpenReminders,
            )

            CashflowSimulatorCard(
                uiState = uiState,
                summary = chartState.monthlySummary,
                forecast = chartState.safeToSpend,
                selectedScenario = selectedScenario,
                onScenarioSelected = { selectedScenario = it },
            )

            AutomationControlCard(
                recurringCount = uiState.recurringRules.size,
                goalCount = uiState.goals.size,
                walletCount = uiState.wallets.size,
                reminderCount = reminderCount,
                onOpenReminders = onOpenReminders,
                onOpenPlans = onOpenPlans,
            )

            Spacer(modifier = Modifier.height(AppDimens.spaceXl))
        }
    }
}

@Composable
private fun IntelligenceHeroCard(
    insightCount: Int,
    signalCount: Int,
    automationCount: Int,
    monthProgress: Float,
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val animatedProgress =
        animateFloatAsState(
            targetValue = monthProgress,
            animationSpec = tween(durationMillis = 700),
            label = "insightMonthProgress",
        ).value

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.xLarge,
        color = Color.Transparent,
        shadowElevation = AppDimens.cardElevation,
    ) {
        Column(
            modifier =
                Modifier
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                    ),
                            ),
                        shape = AppShapes.xLarge,
                    )
                    .padding(AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                ) {
                    Text(
                        text = "Decision layer",
                        style = MaterialTheme.typography.labelMedium,
                        color = onAccent.copy(alpha = 0.78f),
                    )
                    Text(
                        text = "Insights, risk, and what-if checks",
                        style = MaterialTheme.typography.titleLarge,
                        color = onAccent,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = onAccent.copy(alpha = 0.14f),
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = null,
                        tint = onAccent,
                        modifier =
                            Modifier
                                .padding(AppDimens.spaceMd)
                                .size(22.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                    IntelligenceSticker(Icons.Default.Lightbulb, "$insightCount insights", onAccent)
                    IntelligenceSticker(Icons.Default.WarningAmber, "$signalCount signals", onAccent)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                    IntelligenceSticker(Icons.Default.Flag, "$automationCount controls", onAccent)
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                color = onAccent,
                trackColor = onAccent.copy(alpha = 0.24f),
            )
        }
    }
}

@Composable
private fun IntelligenceSticker(
    icon: ImageVector,
    label: String,
    contentColor: Color,
) {
    Surface(
        shape = AppShapes.full,
        color = contentColor.copy(alpha = 0.14f),
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = AppDimens.spaceSm,
                    vertical = AppDimens.spaceXs,
                ),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InsightSection(insights: List<PersonalInsight>) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            SectionHeader(title = "Monthly insights")
            insights.forEach { insight ->
                InsightCard(insight = insight)
            }
        }
    }
}

@Composable
private fun InsightCard(insight: PersonalInsight) {
    val toneColor =
        when (insight.tone) {
            PersonalInsightTone.POSITIVE -> MaterialTheme.colorScheme.secondary
            PersonalInsightTone.WARNING -> MaterialTheme.colorScheme.error
            PersonalInsightTone.INFO -> MaterialTheme.colorScheme.primary
        }

    SignalRow(
        icon =
            when (insight.tone) {
                PersonalInsightTone.POSITIVE -> Icons.Default.Flag
                PersonalInsightTone.WARNING -> Icons.Default.ArrowUpward
                PersonalInsightTone.INFO -> Icons.Default.Lightbulb
            },
        title = insight.title,
        message = insight.message,
        metric = null,
        color = toneColor,
        actionLabel = null,
        onAction = {},
    )
}

@Composable
private fun AnomalyRadarSection(
    signals: List<IntelligenceAnomalySignal>,
    onOpenHistory: () -> Unit,
    onOpenReminders: () -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            SectionHeader(title = "Anomaly radar")
            Text(
                text = "Tracks unusual spend, concentration, split-heavy months, and rising daily pace.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            signals.forEach { signal ->
                SignalRow(
                    icon = Icons.Default.WarningAmber,
                    title = signal.title,
                    message = signal.message,
                    metric = signal.metric,
                    color = toneColor(signal.tone),
                    actionLabel = signal.actionLabel,
                    onAction = {
                        when (signal.target) {
                            IntelligenceTarget.HISTORY -> onOpenHistory()
                            IntelligenceTarget.REMINDERS -> onOpenReminders()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CashflowSimulatorCard(
    uiState: PersonalUiState,
    summary: MonthlySummary,
    forecast: SafeToSpendForecast,
    selectedScenario: CashflowScenario,
    onScenarioSelected: (CashflowScenario) -> Unit,
) {
    val projection =
        remember(uiState, summary, forecast, selectedScenario) {
            buildCashflowProjection(
                uiState = uiState,
                summary = summary,
                forecast = forecast,
                scenario = selectedScenario,
            )
        }
    val projectionColor =
        when (projection.status) {
            CashflowStatus.COMFORTABLE -> MaterialTheme.colorScheme.secondary
            CashflowStatus.TIGHT -> MaterialTheme.colorScheme.tertiary
            CashflowStatus.BLOCKED -> MaterialTheme.colorScheme.error
        }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                ) {
                    Text(
                        text = "Cashflow what-if",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Simulate planned spending against recurring rules and goals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(
                    label = projection.status.label,
                    color = projectionColor,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            ) {
                CashflowScenario.entries.forEach { scenario ->
                    FilterChip(
                        selected = selectedScenario == scenario,
                        onClick = { onScenarioSelected(scenario) },
                        label = { Text(scenario.label) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                ProjectionMetric(
                    modifier = Modifier.weight(1f),
                    label = "Scenario",
                    value = formatMoney(selectedScenario.amount),
                    color = MaterialTheme.colorScheme.primary,
                )
                ProjectionMetric(
                    modifier = Modifier.weight(1f),
                    label = "End balance",
                    value = formatMoney(projection.projectedBalance),
                    color = projectionColor,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                ProjectionMetric(
                    modifier = Modifier.weight(1f),
                    label = "Daily after",
                    value = formatMoney(projection.adjustedDaily),
                    color = projectionColor,
                )
                ProjectionMetric(
                    modifier = Modifier.weight(1f),
                    label = "Fixed + goals",
                    value = formatMoney(projection.reservedAmount),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                text = projection.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AutomationControlCard(
    recurringCount: Int,
    goalCount: Int,
    walletCount: Int,
    reminderCount: Int,
    onOpenReminders: () -> Unit,
    onOpenPlans: () -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            SectionHeader(
                title = "Automation control",
                actionLabel = "Open plans",
                onAction = onOpenPlans,
            )
            Text(
                text = "Review how reminders, recurring rules, goals, and wallets affect the intelligence layer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                AutomationMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.NotificationsActive,
                    title = "Budget guard",
                    value = "$reminderCount reminders",
                    onClick = onOpenReminders,
                )
                AutomationMetric(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Flag,
                    title = "Plan stack",
                    value = "${recurringCount + goalCount + walletCount} items",
                    onClick = onOpenPlans,
                )
            }
        }
    }
}

@Composable
private fun ProjectionMetric(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color,
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.large,
        color = color.copy(alpha = 0.10f),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AutomationMetric(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = AppShapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SignalRow(
    icon: ImageVector,
    title: String,
    message: String,
    metric: String?,
    color: Color,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        color = color.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.14f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier =
                        Modifier
                            .padding(AppDimens.spaceSm)
                            .size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    metric?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!actionLabel.isNullOrBlank()) {
                    TextButton(
                        onClick = onAction,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: Color,
) {
    Surface(
        shape = AppShapes.full,
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = label,
            modifier =
                Modifier.padding(
                    horizontal = AppDimens.spaceMd,
                    vertical = AppDimens.spaceSm,
                ),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

private data class IntelligenceMonthMarker(
    val progress: Float,
)

private data class IntelligenceAnomalySignal(
    val title: String,
    val message: String,
    val metric: String,
    val tone: SignalTone,
    val actionLabel: String,
    val target: IntelligenceTarget,
)

private data class CashflowProjection(
    val projectedBalance: Double,
    val adjustedDaily: Double,
    val reservedAmount: Double,
    val status: CashflowStatus,
    val message: String,
)

private enum class SignalTone {
    POSITIVE,
    INFO,
    WARNING,
    DANGER,
}

private enum class IntelligenceTarget {
    HISTORY,
    REMINDERS,
}

private enum class CashflowScenario(
    val label: String,
    val amount: Double,
) {
    QUICK("Quick", 120_000.0),
    DINNER_WEEKEND("Dinner", 350_000.0),
    GROUP_NIGHT("Group", 750_000.0),
}

private enum class CashflowStatus(val label: String) {
    COMFORTABLE("Feasible"),
    TIGHT("Tight"),
    BLOCKED("Blocked"),
}

private fun buildIntelligenceAnomalySignals(
    transactions: List<Transaction>,
    summary: MonthlySummary,
    monthMarker: IntelligenceMonthMarker,
): List<IntelligenceAnomalySignal> {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    if (expenses.isEmpty()) {
        return listOf(
            IntelligenceAnomalySignal(
                title = "No anomaly yet",
                message = "Add expenses to let the radar compare outliers, pace, and split impact.",
                metric = "Idle",
                tone = SignalTone.INFO,
                actionLabel = "Open ledger",
                target = IntelligenceTarget.HISTORY,
            ),
        )
    }

    val signals = mutableListOf<IntelligenceAnomalySignal>()
    val averageExpense = expenses.map { it.amount }.average().takeUnless { it.isNaN() } ?: 0.0
    val largestExpense = expenses.maxByOrNull { it.amount }
    if (largestExpense != null && averageExpense > 0.0 && largestExpense.amount >= averageExpense * 1.8) {
        signals +=
            IntelligenceAnomalySignal(
                title = "Outlier transaction",
                message = "${largestExpense.category} is ${formatRatio(largestExpense.amount / averageExpense)}x higher than your average expense.",
                metric = formatMoney(largestExpense.amount),
                tone = SignalTone.WARNING,
                actionLabel = "Review history",
                target = IntelligenceTarget.HISTORY,
            )
    }

    val topCategory =
        expenses
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .maxByOrNull { it.value }
    if (topCategory != null && summary.totalExpense > 0.0) {
        val categoryShare = topCategory.value / summary.totalExpense
        if (categoryShare >= 0.45) {
            signals +=
                IntelligenceAnomalySignal(
                    title = "Category concentration",
                    message = "${topCategory.key} owns ${(categoryShare * 100).toInt()}% of this month's expense.",
                    metric = "${(categoryShare * 100).toInt()}%",
                    tone = SignalTone.WARNING,
                    actionLabel = "Add reminder",
                    target = IntelligenceTarget.REMINDERS,
                )
        }
    }

    val splitExpense =
        expenses
            .filter { it.source == TransactionSource.SPLIT }
            .sumOf { it.amount }
    if (summary.totalExpense > 0.0 && splitExpense / summary.totalExpense >= 0.35) {
        signals +=
            IntelligenceAnomalySignal(
                title = "Split-heavy month",
                message = "Split bills are driving ${(splitExpense / summary.totalExpense * 100).toInt()}% of your expense.",
                metric = formatMoney(splitExpense),
                tone = SignalTone.INFO,
                actionLabel = "Review ledger",
                target = IntelligenceTarget.HISTORY,
            )
    }

    val dailyTotals =
        expenses
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
        signals +=
            IntelligenceAnomalySignal(
                title = "Three-day climb",
                message = "Your daily expense increased three tracked days in a row.",
                metric = "3d",
                tone = SignalTone.DANGER,
                actionLabel = "Set guardrail",
                target = IntelligenceTarget.REMINDERS,
            )
    }

    return signals.take(3).ifEmpty {
        listOf(
            IntelligenceAnomalySignal(
                title = "Radar clean",
                message = "No outlier, concentration, or rising-streak signal detected this month.",
                metric = "OK",
                tone = SignalTone.POSITIVE,
                actionLabel = "Open ledger",
                target = IntelligenceTarget.HISTORY,
            ),
        )
    }
}

private fun buildCashflowProjection(
    uiState: PersonalUiState,
    summary: MonthlySummary,
    forecast: SafeToSpendForecast,
    scenario: CashflowScenario,
): CashflowProjection {
    val recurringReserve =
        uiState.recurringRules
            .filter { it.isEnabled && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    val goalRemaining =
        uiState.goals
            .filter { it.status == GoalStatus.ACTIVE }
            .sumOf { (it.targetAmount - it.currentAmount).coerceAtLeast(0.0) }
    val goalReserveCap = if (summary.totalIncome > 0.0) summary.totalIncome * 0.25 else 0.0
    val goalReserve = goalRemaining.coerceAtMost(goalReserveCap)
    val reservedAmount = recurringReserve + goalReserve
    val projectedBalance = summary.balance - reservedAmount - scenario.amount
    val adjustedDaily =
        if (forecast.daysLeft > 0) {
            (projectedBalance / forecast.daysLeft).coerceAtLeast(0.0)
        } else {
            0.0
        }
    val status =
        when {
            projectedBalance <= 0.0 -> CashflowStatus.BLOCKED
            adjustedDaily < 100_000.0 -> CashflowStatus.TIGHT
            else -> CashflowStatus.COMFORTABLE
        }
    val walletBuffer =
        uiState.wallets
            .filterNot { it.isArchived }
            .sumOf { it.balance }
    val message =
        when (status) {
            CashflowStatus.COMFORTABLE ->
                "Scenario fits the month. Wallet buffer: ${formatMoney(walletBuffer)}."
            CashflowStatus.TIGHT ->
                "Scenario works, but daily safe amount becomes tight after fixed plans."
            CashflowStatus.BLOCKED ->
                "Scenario breaks the monthly buffer unless income or wallet coverage changes."
        }

    return CashflowProjection(
        projectedBalance = projectedBalance,
        adjustedDaily = adjustedDaily,
        reservedAmount = reservedAmount,
        status = status,
        message = message,
    )
}

@Composable
private fun toneColor(tone: SignalTone): Color {
    return when (tone) {
        SignalTone.POSITIVE -> MaterialTheme.colorScheme.secondary
        SignalTone.INFO -> MaterialTheme.colorScheme.primary
        SignalTone.WARNING -> MaterialTheme.colorScheme.tertiary
        SignalTone.DANGER -> MaterialTheme.colorScheme.error
    }
}

private fun currentIntelligenceMonthMarker(): IntelligenceMonthMarker {
    val calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    return IntelligenceMonthMarker(
        progress = (day.toFloat() / maxDay.toFloat()).coerceIn(0f, 1f),
    )
}

private fun transactionDayOfMonth(epochMillis: Long): Int {
    return Calendar.getInstance().apply { timeInMillis = epochMillis }
        .get(Calendar.DAY_OF_MONTH)
}

private fun formatMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} VND"
}

private fun formatRatio(value: Double): String {
    return String.format(Locale.US, "%.1f", value)
}
