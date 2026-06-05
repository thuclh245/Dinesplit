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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.ClickableAppCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dinesplit.core.ui.SmallButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.SplitMethod
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SplitScreen(
    userAvatarUrl: String?,
    bottomPadding: Dp = 80.dp,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onNewGroup: () -> Unit,
    onNewExpense: () -> Unit,
    onViewAllGroups: () -> Unit,
    onGroupClick: (String) -> Unit,
    onBillClick: (groupId: String, billId: String) -> Unit = { _, _ -> },
    onNavigateToSettleSummary: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel =
        remember {
            SplitDashboardViewModel(AppContainer.splitRepository(context))
        }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = if (uiState.groups.isEmpty()) onNewGroup else onNewExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier =
                    Modifier
                        .padding(bottom = bottomPadding)
                        .size(60.dp)
                        .shadow(AppDimens.spaceXl, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = if (uiState.groups.isEmpty()) "Tạo nhóm mới" else "Thêm hóa đơn",
                    modifier = Modifier.size(30.dp),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
            contentPadding = PaddingValues(top = 72.dp, bottom = bottomPadding + 100.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space2Xl),
        ) {
            item {
                BalanceSummaryRow(
                    amountYouOwe = uiState.amountYouOwe,
                    amountYouAreOwed = uiState.amountYouAreOwed,
                    onSettleUpClick = {
                        val targetGroup = uiState.groups.firstOrNull { it.yourBalance < 0.0 } ?: uiState.groups.firstOrNull()
                        if (targetGroup != null) {
                            onNavigateToSettleSummary(targetGroup.id)
                        } else {
                            android.widget.Toast.makeText(context, "Vui lòng chọn hoặc tham gia một nhóm để thực hiện thanh toán.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRemindClick = {
                        android.widget.Toast.makeText(context, "Tính năng nhắc nợ đang được phát triển.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (uiState.isLoading) {
                item {
                    LoadingBlock(
                        message = "Đang tải dữ liệu nhóm...",
                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg),
                    )
                }
            } else if (uiState.error != null) {
                item {
                    ErrorStateBlock(
                        title = "Không thể tải dữ liệu",
                        subtitle = uiState.error.orEmpty(),
                        onRetryClick = { /* ViewModel refresh if available */ },
                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg),
                    )
                }
            } else {
                item {
                    GroupsSection(
                        groups = uiState.groups,
                        billsByGroup = uiState.billsByGroup,
                        onViewAllGroups = onViewAllGroups,
                        onNewGroup = onNewGroup,
                        onGroupClick = onGroupClick,
                    )
                }

                item {
                    RecentBillsSection(
                        recentBills = uiState.recentBills,
                        currentUserId = uiState.currentUserId,
                        onBillClick = onBillClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceSummaryRow(
    amountYouOwe: Double,
    amountYouAreOwed: Double,
    onSettleUpClick: () -> Unit,
    onRemindClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
    ) {
        BalanceCard(
            title = "BẠN ĐANG NỢ",
            amount = formatAmount(amountYouOwe),
            buttonText = "Chốt sổ",
            onClick = onSettleUpClick,
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.primary,
        )
        BalanceCard(
            title = "BẠN ĐƯỢC TRẢ",
            amount = formatAmount(amountYouAreOwed),
            buttonText = "Nhắc nợ",
            onClick = onRemindClick,
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.secondary,
            isSecondary = true,
        )
    }
}

@Composable
private fun GroupsSection(
    groups: List<Group>,
    billsByGroup: Map<String, List<Bill>>,
    onViewAllGroups: () -> Unit,
    onNewGroup: () -> Unit,
    onGroupClick: (String) -> Unit,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.screenHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "Nhóm của bạn",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            )
            Text(
                "Xem tất cả",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(role = Role.Button) { onViewAllGroups() },
            )
        }
        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AppDimens.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
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
                        onClick = { onGroupClick(group.id) },
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
    currentUserId: String?,
    onBillClick: (groupId: String, billId: String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
    ) {
        Text(
            "Hóa đơn gần đây",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        )

        if (recentBills.isEmpty()) {
            DashboardMessageCard(
                title = "Chưa có hóa đơn",
                message = "Tạo hóa đơn đầu tiên trong một nhóm để danh sách này tự cập nhật.",
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                recentBills.forEach { recentBill ->
                    BillItem(
                        recentBill = recentBill,
                        currentUserId = currentUserId,
                        onClick = {
                            onBillClick(recentBill.bill.groupId, recentBill.bill.id)
                        },
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    isSecondary: Boolean = false,
) {
    AppCard(
        modifier = modifier.height(160.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .padding(AppDimens.spaceLg)
                        .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            amount,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = contentColor,
                            maxLines = 2,
                        )
                        Text(
                            "đ",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = contentColor,
                            modifier = Modifier.padding(bottom = AppDimens.spaceXs, start = 2.dp),
                        )
                    }
                }
                SmallButton(
                    text = buttonText,
                    onClick = onClick,
                    shape = CircleShape,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = if (isSecondary) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            contentColor = if (isSecondary) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary,
                        ),
                    modifier =
                        Modifier.then(
                            if (!isSecondary) {
                                Modifier.background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
                                    ),
                                    CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: Group,
    bills: List<Bill>,
    onClick: () -> Unit,
) {
    val isSettled = bills.isNotEmpty() && bills.all { it.isSettled() }
    val openCount = bills.count { !it.isSettled() }
    val status =
        when {
            bills.isEmpty() -> "Chưa có hóa đơn"
            isSettled -> "Đã tất toán"
            else -> "$openCount hóa đơn mở"
        }

    ClickableAppCard(
        onClick = onClick,
        modifier =
            Modifier
                .width(240.dp)
                .height(192.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(AppDimens.spaceLg),
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    GroupInitialStack(group = group)
                    Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                    Text(
                        group.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${group.memberCount} thành viên",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = if (isSettled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = AppShapes.small,
                    ) {
                        Text(
                            status,
                            modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSettled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        imageVector = if (isSettled) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSettled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupInitialStack(group: Group) {
    val surfaceColor = MaterialTheme.colorScheme.background
    Row(horizontalArrangement = Arrangement.spacedBy(-AppDimens.spaceSm)) {
        val visibleCount = minOf(group.memberCount.coerceAtLeast(1), 3)
        repeat(visibleCount) { index ->
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .border(2.dp, surfaceColor, CircleShape)
                        .clip(CircleShape)
                        .background(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                            )[index % 3],
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = group.name.firstOrNull()?.uppercase().orEmpty(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        val extraMembers = group.memberCount - visibleCount
        if (extraMembers > 0) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .border(2.dp, surfaceColor, CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+$extraMembers", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun CreateGroupDashboardCard(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .width(240.dp)
                .height(192.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), AppShapes.xLarge)
                .clip(AppShapes.xLarge)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.GroupAdd,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tạo nhóm mới",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Chia hóa đơn ăn uống cùng bạn bè",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BillItem(
    recentBill: SplitDashboardRecentBill,
    currentUserId: String?,
    onClick: () -> Unit,
) {
    val bill = recentBill.bill
    val amountInfo = bill.dashboardAmountLabel(currentUserId)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = AppShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), AppShapes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (bill.method == SplitMethod.ITEMIZED) Icons.Default.Payments else Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column {
                    Text(
                        bill.name,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${formatDate(bill.date)} • ${recentBill.groupName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    amountInfo.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = amountInfo.color,
                )
                Surface(
                    color = amountInfo.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(AppDimens.radiusSm),
                ) {
                    Text(
                        if (bill.isSettled()) "SETTLED" else "OPEN",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                            ),
                        color = amountInfo.color,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMessageCard(
    title: String,
    message: String,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceLg),
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class DashboardAmountLabel(
    val label: String,
    val color: Color,
)

@Composable
private fun Bill.dashboardAmountLabel(currentUserId: String?): DashboardAmountLabel {
    val colorScheme = MaterialTheme.colorScheme
    val userId = currentUserId.orEmpty()
    val myShare = shares[userId] ?: 0.0
    val outstandingForMe = userId.isNotBlank() && payerId != userId && !paidMemberIds.contains(userId) && myShare > 0.0
    val owedToMe =
        if (userId.isNotBlank() && payerId == userId) {
            shares
                .filterKeys { memberId -> memberId != userId && !paidMemberIds.contains(memberId) }
                .values
                .sum()
        } else {
            0.0
        }

    return when {
        outstandingForMe -> DashboardAmountLabel("Bạn nợ ${formatShortAmount(myShare)}", colorScheme.error)
        owedToMe > 0.0 -> DashboardAmountLabel("Nhận ${formatShortAmount(owedToMe)}", colorScheme.secondary)
        userId.isNotBlank() && paidMemberIds.contains(userId) ->
            DashboardAmountLabel(
                "Đã trả ${formatShortAmount(myShare)}",
                colorScheme.secondary,
            )
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
            onGroupClick = {},
        )
    }
}
