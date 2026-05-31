package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.ui.HomeTopBar
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.SplitMethod
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CURRENT_USER_ID = "me"

@Composable
fun SplitScreen(
    userAvatarUrl: String?,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onNewGroup: () -> Unit,
    onNewExpense: () -> Unit,
    onViewAllGroups: () -> Unit,
    onGroupClick: (String) -> Unit,
    onBillClick: (groupId: String, billId: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val viewModel = remember {
        SplitDashboardViewModel(AppContainer.splitRepository(context))
    }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            HomeTopBar(
                userAvatarUrl = userAvatarUrl,
                title = "Split Bill",
                onOpenNotifications = onOpenNotifications,
                onOpenSearch = onOpenSearch
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = if (uiState.groups.isEmpty()) onNewGroup else onNewExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(60.dp)
                    .shadow(24.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Thêm hóa đơn", modifier = Modifier.size(30.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                BalanceSummaryRow(
                    amountYouOwe = uiState.amountYouOwe,
                    amountYouAreOwed = uiState.amountYouAreOwed
                )
            }

            if (uiState.isLoading) {
                item {
                    LoadingBlock(
                        message = "Đang tải dữ liệu nhóm...",
                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg)
                    )
                }
            } else if (uiState.error != null) {
                item {
                    ErrorStateBlock(
                        title = "Không thể tải dữ liệu",
                        subtitle = uiState.error.orEmpty(),
                        onRetryClick = { /* ViewModel refresh if available */ },
                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg)
                    )
                }
            } else {
                item {
                    GroupsSection(
                        groups = uiState.groups,
                        billsByGroup = uiState.billsByGroup,
                        onViewAllGroups = onViewAllGroups,
                        onNewGroup = onNewGroup,
                        onGroupClick = onGroupClick
                    )
                }

                item {
                    RecentBillsSection(
                        recentBills = uiState.recentBills,
                        onBillClick = onBillClick
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceSummaryRow(
    amountYouOwe: Double,
    amountYouAreOwed: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BalanceCard(
            title = "BẠN ĐANG NỢ",
            amount = formatAmount(amountYouOwe),
            buttonText = "Settle Up",
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.primary
        )
        BalanceCard(
            title = "BẠN ĐƯỢC TRẢ",
            amount = formatAmount(amountYouAreOwed),
            buttonText = "Remind",
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.secondary,
            showLeftBorder = true
        )
    }
}

@Composable
private fun GroupsSection(
    groups: List<Group>,
    billsByGroup: Map<String, List<Bill>>,
    onViewAllGroups: () -> Unit,
    onNewGroup: () -> Unit,
    onGroupClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.screenHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "Nhóm của bạn",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                "Xem tất cả",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onViewAllGroups() }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AppDimens.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    CreateGroupDashboardCard(onClick = onNewGroup)
                }
            } else {
                items(groups.take(6), key = { it.id }) { group ->
                    val bills = billsByGroup[group.id].orEmpty()
                    GroupCard(
                        group = group,
                        bills = bills,
                        onClick = { onGroupClick(group.id) }
                    )
                }
                item {
                    CreateGroupDashboardCard(onClick = onNewGroup)
                }
            }
        }
    }
}

@Composable
private fun RecentBillsSection(
    recentBills: List<SplitDashboardRecentBill>,
    onBillClick: (groupId: String, billId: String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Hóa đơn gần đây",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
        )

        if (recentBills.isEmpty()) {
            DashboardMessageCard(
                title = "Chưa có hóa đơn",
                message = "Tạo hóa đơn đầu tiên trong một nhóm để danh sách này tự cập nhật."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                recentBills.forEach { recentBill ->
                    BillItem(
                        recentBill = recentBill,
                        onClick = {
                            onBillClick(recentBill.bill.groupId, recentBill.bill.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    title: String,
    amount: String,
    buttonText: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    showLeftBorder: Boolean = false
) {
    Card(
        modifier = modifier.height(160.dp),
        shape = AppShapes.xLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showLeftBorder) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(contentColor)
                        .align(Alignment.CenterStart)
                )
            }
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            amount,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = contentColor,
                            maxLines = 2
                        )
                        Text(
                            "đ",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = contentColor,
                            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                        )
                    }
                }
                Button(
                    onClick = { },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showLeftBorder) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                        contentColor = if (showLeftBorder) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                    ),
                    modifier = Modifier.then(
                        if (!showLeftBorder) {
                            Modifier.background(
                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)),
                                CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: Group,
    bills: List<Bill>,
    onClick: () -> Unit
) {
    val isSettled = bills.isNotEmpty() && bills.all { it.isSettled() }
    val openCount = bills.count { !it.isSettled() }
    val status = when {
        bills.isEmpty() -> "Chưa có hóa đơn"
        isSettled -> "Đã tất toán"
        else -> "$openCount hóa đơn mở"
    }

    Card(
        modifier = Modifier
            .width(240.dp)
            .height(192.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSettled) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    GroupInitialStack(group = group)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        group.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${group.memberCount} thành viên",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isSettled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = if (isSettled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (isSettled) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSettled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupInitialStack(group: Group) {
    val surfaceColor = MaterialTheme.colorScheme.background
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        val visibleCount = minOf(group.memberCount.coerceAtLeast(1), 3)
        repeat(visibleCount) { index ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(2.dp, surfaceColor, CircleShape)
                    .clip(CircleShape)
                    .background(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary
                        )[index % 3]
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.name.firstOrNull()?.uppercase().orEmpty(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        val extraMembers = group.memberCount - visibleCount
        if (extraMembers > 0) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(2.dp, surfaceColor, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+$extraMembers", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun CreateGroupDashboardCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(192.dp)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            Text("Tạo nhóm mới", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun BillItem(
    recentBill: SplitDashboardRecentBill,
    onClick: () -> Unit
) {
    val bill = recentBill.bill
    val amountInfo = bill.dashboardAmountLabel()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = AppShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (bill.method == SplitMethod.ITEMIZED) Icons.Default.Payments else Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        bill.name,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${formatDate(bill.date)} • ${recentBill.groupName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amountInfo.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = amountInfo.color
                )
                Surface(
                    color = amountInfo.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (bill.isSettled()) "SETTLED" else "OPEN",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                        color = amountInfo.color
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMessageCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = AppShapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class DashboardAmountLabel(
    val label: String,
    val color: Color
)

@Composable
private fun Bill.dashboardAmountLabel(): DashboardAmountLabel {
    val colorScheme = MaterialTheme.colorScheme
    val myShare = shares[CURRENT_USER_ID] ?: 0.0
    val outstandingForMe = payerId != CURRENT_USER_ID && !paidMemberIds.contains(CURRENT_USER_ID) && myShare > 0.0
    val owedToMe = if (payerId == CURRENT_USER_ID) {
        shares
            .filterKeys { memberId -> memberId != CURRENT_USER_ID && !paidMemberIds.contains(memberId) }
            .values
            .sum()
    } else {
        0.0
    }

    return when {
        outstandingForMe -> DashboardAmountLabel("Bạn nợ ${formatShortAmount(myShare)}", colorScheme.error)
        owedToMe > 0.0 -> DashboardAmountLabel("Nhận ${formatShortAmount(owedToMe)}", colorScheme.secondary)
        paidMemberIds.contains(CURRENT_USER_ID) -> DashboardAmountLabel("Đã trả ${formatShortAmount(myShare)}", colorScheme.secondary)
        else -> DashboardAmountLabel(formatShortAmount(totalAmount), colorScheme.primary)
    }
}

private fun Bill.isSettled(): Boolean {
    return shares.keys
        .filter { it != payerId }
        .all { paidMemberIds.contains(it) }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Chưa có ngày"
    return SimpleDateFormat("dd/MM", Locale("vi", "VN")).format(Date(timestamp))
}

private fun formatAmount(amount: Double): String {
    return "%,.0f".format(amount).replace(",", ".")
}

private fun formatShortAmount(amount: Double): String {
    return if (amount >= 1000.0) {
        "${formatAmount(amount / 1000.0)}k"
    } else {
        formatAmount(amount)
    }
}

@Preview(showBackground = true)
@Composable
fun SplitScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        SplitScreen(
            userAvatarUrl = null,
            onOpenNotifications = {},
            onOpenSearch = {},
            onNewGroup = {},
            onNewExpense = {},
            onViewAllGroups = {},
            onGroupClick = {}
        )
    }
}
