package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dinesplit.core.ui.HomeTopBar
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun PersonalScreen(
    userAvatarUrl: String?,
    onOpenSearch: () -> Unit,
    onAddTransaction: () -> Unit
) {
    Scaffold(
        topBar = {
            HomeTopBar(
                userAvatarUrl = userAvatarUrl,
                title = "Personal",
                onOpenSearch = onOpenSearch
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 115.dp) // Đẩy lên 115dp
                    .size(60.dp) // Đồng bộ 60dp
                    .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(30.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp), // Đệm 120dp
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Month Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = CircleShape,
                        onClick = { }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Tháng này", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shadowElevation = 2.dp,
                        onClick = { }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Total Balance Card
            item {
                TotalBalanceCard(amount = "42.850.000", trend = "+12% so với tháng trước")
            }

            // Income & Expense Summary
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryCard(
                        title = "THU NHẬP",
                        amount = "15.200.000 ₫",
                        icon = Icons.Default.ArrowDownward,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "CHI TIÊU",
                        amount = "8.450.000 ₫",
                        icon = Icons.Default.ArrowUpward,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Spending Insights
            item {
                SpendingInsights()
            }

            // Recent Transactions
            item {
                RecentTransactions()
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(amount: String, trend: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)))
                .padding(32.dp)
        ) {
            // Decorative Glow
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 48.dp, y = (-48).dp)
                    .size(192.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .blur(48.dp)
            )

            Column {
                Text("TỔNG SỐ DƯ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = Color.White.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(amount, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black), color = Color.White)
                    Text(" ₫", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal), color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(trend, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = contentColor.copy(alpha = 0.7f))
                Text(amount, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = contentColor)
            }
        }
    }
}

@Composable
private fun SpendingInsights() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Phân tích chi tiêu", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Custom Donut Chart
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        val primary = MaterialTheme.colorScheme.primary
                        val secondary = MaterialTheme.colorScheme.secondary
                        val tertiary = MaterialTheme.colorScheme.tertiary
                        val track = MaterialTheme.colorScheme.surfaceContainerLow
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = track, style = Stroke(width = 8.dp.toPx()))
                            drawArc(color = primary, startAngle = -90f, sweepAngle = 216f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(color = tertiary, startAngle = 126f, sweepAngle = 90f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(color = secondary, startAngle = 216f, sweepAngle = 54f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                        }
                        Text("Tháng 10", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                    }
                    
                    Spacer(modifier = Modifier.width(32.dp))
                    
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InsightItem(label = "Ăn uống", percentage = "60%", color = MaterialTheme.colorScheme.primary)
                        InsightItem(label = "Giải trí", percentage = "25%", color = MaterialTheme.colorScheme.tertiary)
                        InsightItem(label = "Di chuyển", percentage = "15%", color = MaterialTheme.colorScheme.secondary)
                    }
                }

                // Goal Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                            Text("MỤC TIÊU TIẾT KIỆM", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("85%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { 0.85f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightItem(label: String, percentage: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium))
        }
        Text(percentage, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun RecentTransactions() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Giao dịch gần đây", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text("Xem tất cả", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TransactionItem(title = "Phở Thìn Lò Đúc", time = "Hôm nay, 12:30", amount = "-120.000 ₫", category = "Ăn uống", icon = Icons.Default.Restaurant, iconColor = MaterialTheme.colorScheme.primary)
            TransactionItem(title = "Lương tháng 10", time = "Hôm qua, 09:00", amount = "+15.200.000 ₫", category = "Thu nhập", icon = Icons.Default.Payments, iconColor = MaterialTheme.colorScheme.secondary, isIncome = true)
            TransactionItem(title = "CGV Cinema", time = "20 Th10, 20:15", amount = "-350.000 ₫", category = "Giải trí", icon = Icons.Default.Movie, iconColor = MaterialTheme.colorScheme.tertiary)
            TransactionItem(title = "Grab Bike", time = "19 Th10, 18:45", amount = "-45.000 ₫", category = "Di chuyển", icon = Icons.Default.DirectionsCar, iconColor = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TransactionItem(
    title: String,
    time: String,
    amount: String,
    category: String,
    icon: ImageVector,
    iconColor: Color,
    isIncome: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).background(iconColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                Surface(
                    color = if (isIncome) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainer,
                    shape = CircleShape
                ) {
                    Text(
                        category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PersonalScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        PersonalScreen(userAvatarUrl = null, onOpenSearch = {}, onAddTransaction = {})
    }
}
