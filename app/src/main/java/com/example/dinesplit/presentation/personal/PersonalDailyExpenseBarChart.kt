package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppDimens
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.max

@Composable
fun PersonalDailyExpenseBarChart(
    bars: List<DailyExpenseBar>,
    modifier: Modifier = Modifier,
) {
    val validBars = bars.filter { it.amount > 0.0 }.sortedBy { it.dayOfMonth }
    val maxAmount = validBars.maxOfOrNull { it.amount } ?: 0.0

    if (validBars.isEmpty() || maxAmount <= 0.0) {
        Box(
            modifier =
                modifier
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Không có dữ liệu chi tiêu hàng ngày",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val totalAmount = validBars.sumOf { it.amount }
    val barSummary = "Biểu đồ cột chi tiêu hàng ngày. Tổng chi tiêu trong tháng: ${totalAmount.toInt()} đồng."

    Column(
        modifier = modifier.semantics {
            contentDescription = barSummary
        },
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(170.dp),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            verticalAlignment = Alignment.Bottom,
        ) {
            validBars.forEach { item ->
                val fraction = (item.amount / maxAmount).toFloat().coerceIn(0.05f, 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Ngày ${item.dayOfMonth}: ${item.amount.toInt()} đồng"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                ) {
                    Text(
                        text = item.amount.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(max(18f, 110f * fraction).dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = item.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = "Ngày trong tháng",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
