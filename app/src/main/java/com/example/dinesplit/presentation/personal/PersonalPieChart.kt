package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppDimens

@Composable
fun PersonalPieChart(
    slices: List<PieCategorySlice>,
    modifier: Modifier = Modifier
) {
    val validSlices = slices.filter { it.percentage > 0f }
    val total = validSlices.sumOf { it.percentage.toDouble() }.toFloat()

    if (validSlices.isEmpty() || total <= 0f) {
        Box(
            modifier = modifier
                .height(180.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No chart data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        Color(0xFF4CAF50),
        Color(0xFFFF9800)
    )

    val coloredSlices = validSlices.mapIndexed { index, item ->
        item to palette[index % palette.size]
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val stroke = Stroke(width = 36.dp.toPx(), cap = StrokeCap.Butt)
                var startAngle = -90f
                coloredSlices.forEach { (slice, color) ->
                    val sweep = (slice.percentage / total) * 360f
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = stroke
                    )
                    startAngle += sweep
                }
            }

            Text(
                text = "100%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            coloredSlices.forEach { (slice, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(color)
                        )
                        Text(
                            text = slice.category,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "${(slice.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

