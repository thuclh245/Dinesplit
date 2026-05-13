package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.SplitMethod
import com.example.dinesplit.ui.theme.BrandPrimary
import com.example.dinesplit.ui.theme.BrandPrimaryContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    onNavigateToCreateBill: () -> Unit,
    onNavigateToBillDetail: (String) -> Unit,
    onNavigateToSettleSummary: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember(groupId) {
        GroupDetailViewModel(
            repository = AppContainer.splitRepository(context),
            groupId = groupId
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = {
            DetailTopBar(
                groupName = uiState.group?.name ?: "Chi tiết nhóm",
                memberCount = uiState.group?.memberCount ?: 0,
                onBack = onBack
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(brush = Brush.linearGradient(listOf(BrandPrimaryContainer, BrandPrimary)))
                    .clickable { onNavigateToCreateBill() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm hóa đơn",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when {
                uiState.isLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> item {
                    DetailMessageCard(
                        title = "Không thể tải nhóm",
                        message = uiState.error.orEmpty()
                    )
                }

                uiState.group != null -> {
                    val group = uiState.group!!

                    item { DetailSummaryCard(group = group) }
                    item { DetailTabNavigation(onNavigateToSettleSummary = onNavigateToSettleSummary) }

                    if (uiState.bills.isEmpty()) {
                        item {
                            DetailMessageCard(
                                title = "Chưa có hóa đơn",
                                message = "Bấm nút + để tạo hóa đơn đầu tiên cho nhóm này."
                            )
                        }
                    } else {
                        item { DetailSectionTitle(title = "Hóa đơn gần đây") }
                        items(uiState.bills, key = { it.id }) { bill ->
                            DetailBillCard(
                                bill = bill,
                                onClick = { onNavigateToBillDetail(bill.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    groupName: String,
    memberCount: Int,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.98f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colorScheme.outline
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp)
        ) {
            Text(
                text = groupName,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$memberCount thành viên",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                repeat(minOf(3, memberCount.coerceAtLeast(1))) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainerHigh)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Cài đặt",
                tint = colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DetailSummaryCard(group: Group) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Text(
                text = "TỔNG CHI TIÊU NHÓM",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatAmount(group.totalExpense),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "đ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }

            Surface(
                color = if (group.yourBalance < 0.0) {
                    colorScheme.errorContainer.copy(alpha = 0.2f)
                } else {
                    colorScheme.secondaryContainer.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (group.yourBalance < 0.0) colorScheme.error else colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatBalanceLabel(group.yourBalance),
                        color = if (group.yourBalance < 0.0) colorScheme.error else colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTabNavigation(onNavigateToSettleSummary: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        brush = Brush.linearGradient(listOf(BrandPrimaryContainer, BrandPrimary)),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hóa đơn",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToSettleSummary() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Số dư",
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DetailSectionTitle(title: String) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun DetailBillCard(
    bill: Bill,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.primaryContainer.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatSplitMethod(bill.method)} • ${formatDate(bill.date)}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${formatAmount(bill.totalAmount)} đ",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DetailMessageCard(
    title: String,
    message: String
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatBalanceLabel(balance: Double): String {
    return when {
        balance < 0.0 -> "Bạn đang nợ: ${formatAmount(abs(balance))} đ"
        balance > 0.0 -> "Bạn được trả: ${formatAmount(balance)} đ"
        else -> "Nhóm chưa phát sinh số dư"
    }
}

private fun formatSplitMethod(method: SplitMethod): String {
    return when (method) {
        SplitMethod.EQUAL -> "Chia đều"
        SplitMethod.CUSTOM -> "Tự nhập"
        SplitMethod.ITEMIZED -> "Theo món"
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Chưa có ngày"
    return SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(timestamp))
}

private fun formatAmount(amount: Double): String {
    return "%,.0f".format(amount).replace(",", ".")
}
