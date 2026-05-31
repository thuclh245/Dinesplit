package com.example.dinesplit.presentation.personal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.BackNavigationButton
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
        title = "Tổng hợp hàng tháng",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            SummaryHeroCard(
                monthLabel = "$monthName $year",
                summary = summary
            )

            if (categorySpending.isNotEmpty()) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                        Text(
                             text = "Chi tiêu chi tiết",
                             style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.Bold
                         )
                        categorySpending.forEachIndexed { index, slice ->
                            CategorySpendRow(
                                slice = slice,
                                color = summaryToneColor(index)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeroCard(
    monthLabel: String,
    summary: MonthlySummary
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val balanceColor = if (summary.balance >= 0.0) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }

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
                        text = monthLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = onAccent.copy(alpha = 0.78f)
                    )
                    Text(
                        text = formatCurrency(summary.balance),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = onAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = balanceColor.copy(alpha = 0.24f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = null,
                        tint = onAccent,
                        modifier = Modifier
                            .padding(AppDimens.spaceMd)
                            .size(22.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                MetricPill(
                     modifier = Modifier.weight(1f),
                     icon = Icons.Filled.ArrowDownward,
                     title = "Thu nhập",
                     amount = summary.totalIncome,
                     color = onAccent
                 )
                 MetricPill(
                     modifier = Modifier.weight(1f),
                     icon = Icons.Filled.ArrowUpward,
                     title = "Chi tiêu",
                     amount = summary.totalExpense,
                     color = onAccent
                 )
            }
        }
    }
}

@Composable
private fun CategorySpendRow(
    slice: PieCategorySlice,
    color: Color
) {
    val animatedProgress = animateFloatAsState(
        targetValue = (slice.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "categorySpend"
    ).value

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slice.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = color,
            trackColor = color.copy(alpha = 0.16f)
        )
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
        color = color.copy(alpha = 0.12f)
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
                    color = color.copy(alpha = 0.76f)
                )
                Text(
                    text = formatCurrency(amount),
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun summaryToneColor(index: Int): Color {
    return when (index % 3) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
}

