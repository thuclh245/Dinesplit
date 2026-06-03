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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillStatus
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.SplitMethod
import com.example.dinesplit.ui.theme.BrandPrimary
import com.example.dinesplit.ui.theme.BrandPrimaryContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class GroupDetailTab {
    Bills,
    Balances,
}

@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    onNavigateToCreateBill: () -> Unit,
    onNavigateToBillDetail: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = remember(groupId) {
        GroupDetailViewModel(
            repository = AppContainer.splitRepository(context),
            profileRepository = AppContainer.profileRepository(context),
            groupId = groupId,
            currentUserId = FirebaseProviders.auth.currentUser?.uid
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(GroupDetailTab.Bills) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted, uiState.isLeft) {
        if (uiState.isDeleted || uiState.isLeft) {
            onBack()
        }
    }

    if (showLeaveDialog) {
        LeaveGroupConfirmDialog(
            groupName = uiState.group?.name.orEmpty(),
            isLeaving = uiState.isLeaving,
            onDismiss = {
                if (!uiState.isLeaving) {
                    showLeaveDialog = false
                }
            },
            onConfirm = viewModel::leaveGroup,
        )
    }

    if (showDeleteDialog) {
        DeleteGroupConfirmDialog(
            groupName = uiState.group?.name.orEmpty(),
            isDeleting = uiState.isDeleting,
            onDismiss = {
                if (!uiState.isDeleting) {
                    showDeleteDialog = false
                }
            },
            onConfirm = viewModel::deleteGroup,
        )
    }

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = {
            DetailTopBar(
                groupName = uiState.group?.name ?: "Chi tiết nhóm",
                memberCount = uiState.members.size.takeIf { it > 0 } ?: uiState.group?.memberCount ?: 0,
                members = uiState.members,
                isOwner = uiState.isCurrentUserOwner,
                onBack = onBack,
                onLeaveClick = { showLeaveDialog = true },
                onDeleteClick = { showDeleteDialog = true },
            )
        },
        floatingActionButton = {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(brush = Brush.linearGradient(listOf(BrandPrimaryContainer, BrandPrimary)))
                        .clickable { onNavigateToCreateBill() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm hóa đơn",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when {
                uiState.isLoading ->
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                uiState.error != null ->
                    item {
                        DetailMessageCard(
                            title = "Không thể tải nhóm",
                            message = uiState.error.orEmpty(),
                        )
                    }

                uiState.group != null -> {
                    item {
                        DetailSummaryCard(
                            totalExpense = uiState.totalExpense,
                            yourBalance = uiState.yourBalance,
                        )
                    }
                    item {
                        DetailTabNavigation(
                            selectedTab = selectedTab,
                            onTabSelected = { tab -> selectedTab = tab },
                        )
                    }

                    when (selectedTab) {
                        GroupDetailTab.Bills -> {
                            if (uiState.bills.isEmpty()) {
                                item {
                                    DetailMessageCard(
                                        title = "Chưa có hóa đơn",
                                        message = "Bấm nút + để tạo hóa đơn đầu tiên cho nhóm này.",
                                    )
                                }
                            } else {
                                item { DetailSectionTitle(title = "Hóa đơn gần đây") }
                                items(uiState.bills, key = { it.id }) { bill ->
                                    DetailBillCard(
                                        bill = bill,
                                        onClick = { onNavigateToBillDetail(bill.id) },
                                    )
                                }
                            }
                        }

                        GroupDetailTab.Balances -> {
                            if (uiState.memberBalances.isEmpty()) {
                                item {
                                    DetailMessageCard(
                                        title = "Chưa có số dư",
                                        message = "Tạo hóa đơn đầu tiên để DineSplit tính ai đang nợ ai.",
                                    )
                                }
                            } else {
                                item { DetailSectionTitle(title = "Số dư thành viên") }
                                items(uiState.memberBalances, key = { it.memberId }) { balance ->
                                    DetailBalanceCard(balance = balance)
                                }

                                item { DetailSectionTitle(title = "Gợi ý thanh toán") }
                                if (uiState.settlements.isEmpty()) {
                                    item {
                                        DetailMessageCard(
                                            title = "Nhóm đã cân bằng",
                                            message = "Không có khoản nợ nào cần thanh toán.",
                                        )
                                    }
                                } else {
                                    items(uiState.settlements) { settlement ->
                                        DetailSettlementCard(
                                            settlement = settlement,
                                            isYourPayment = settlement.fromMemberId == uiState.currentUserId,
                                            onOpenBill =
                                                settlement.relatedBillId?.let { billId ->
                                                    { onNavigateToBillDetail(billId) }
                                                },
                                        )
                                    }
                                }
                            }
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
    members: List<Member>,
    isOwner: Boolean,
    onBack: () -> Unit,
    onLeaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.98f))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colorScheme.outline,
            )
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 8.dp),
        ) {
            Text(
                text = groupName,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$memberCount thành viên",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        Row(
            modifier = Modifier.clickable(onClick = onLeaveClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailMemberAvatarStack(
                members = members,
                fallbackCount = memberCount,
                fallbackName = groupName
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Rời nhóm",
                tint = colorScheme.outline,
                modifier = Modifier.size(24.dp),
            )
        }

        if (isOwner) {
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa nhóm",
                    tint = colorScheme.error,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailMemberAvatarStack(
    members: List<Member>,
    fallbackCount: Int,
    fallbackName: String
) {
    val visibleMembers = members.take(3)
    Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
        if (visibleMembers.isEmpty()) {
            repeat(minOf(3, fallbackCount.coerceAtLeast(1))) { index ->
                DetailMemberAvatar(
                    imageUrl = null,
                    name = fallbackName,
                    seed = "$fallbackName-$index"
                )
            }
        } else {
            visibleMembers.forEach { member ->
                DetailMemberAvatar(
                    imageUrl = member.avatarUrl,
                    name = member.name.ifBlank { member.initial },
                    seed = member.id
                )
            }
        }
    }
}

@Composable
private fun DetailMemberAvatar(
    imageUrl: String?,
    name: String,
    seed: String
) {
    val colorScheme = MaterialTheme.colorScheme
    DineAvatarImage(
        imageUrl = imageUrl,
        name = name,
        size = 32.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = colorScheme.surfaceContainerLowest,
            shape = CircleShape
        ),
        fallbackContainerColor = detailAvatarColor(seed),
        fallbackContentColor = Color.White
    )
}

@Composable
private fun detailAvatarColor(seed: String): Color {
    val colorScheme = MaterialTheme.colorScheme
    val colors = listOf(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary,
        colorScheme.error
    )
    return colors[(seed.hashCode() and Int.MAX_VALUE) % colors.size]
}

@Composable
private fun LeaveGroupConfirmDialog(
    groupName: String,
    isLeaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val displayName = groupName.ifBlank { "nhóm này" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rời nhóm?",
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = "Bạn sẽ rời khỏi nhóm \"$displayName\". Nhóm và hóa đơn vẫn được giữ lại cho các thành viên còn lại.",
                color = colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLeaving,
            ) {
                if (isLeaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Rời nhóm",
                        color = colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLeaving,
            ) {
                Text("Hủy")
            }
        },
    )
}

@Composable
private fun DeleteGroupConfirmDialog(
    groupName: String,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val displayName = groupName.ifBlank { "nhóm này" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Xóa nhóm?",
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = "Nhóm \"$displayName\" cùng toàn bộ hóa đơn và thành viên sẽ bị xóa khỏi Firebase.",
                color = colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Xóa",
                        color = colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
            ) {
                Text("Hủy")
            }
        },
    )
}

@Composable
private fun DetailSummaryCard(
    totalExpense: Double,
    yourBalance: Double,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Text(
                text = "TỔNG CHI TIÊU NHÓM",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
            )

            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = formatAmount(totalExpense),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "đ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                )
            }

            Surface(
                color =
                    if (yourBalance < 0.0) {
                        colorScheme.errorContainer.copy(alpha = 0.2f)
                    } else {
                        colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    },
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (yourBalance < 0.0) colorScheme.error else colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatBalanceLabel(yourBalance),
                        color = if (yourBalance < 0.0) colorScheme.error else colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTabNavigation(
    selectedTab: GroupDetailTab,
    onTabSelected: (GroupDetailTab) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailTabButton(
                text = "Hóa đơn",
                selected = selectedTab == GroupDetailTab.Bills,
                onClick = { onTabSelected(GroupDetailTab.Bills) },
                modifier = Modifier.weight(1f),
            )
            DetailTabButton(
                text = "Số dư",
                selected = selectedTab == GroupDetailTab.Balances,
                onClick = { onTabSelected(GroupDetailTab.Balances) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .then(
                    if (selected) {
                        Modifier.background(
                            brush = Brush.linearGradient(listOf(BrandPrimaryContainer, BrandPrimary)),
                            shape = RoundedCornerShape(50),
                        )
                    } else {
                        Modifier.background(Color.Transparent)
                    },
                )
                .clickable { onClick() }
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
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
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun DetailBillCard(
    bill: Bill,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp),
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
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatSplitMethod(bill.method)} • ${formatDate(bill.date)}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatAmount(bill.totalAmount)} đ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.primary,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color =
                        if (bill.isSettled()) {
                            colorScheme.secondaryContainer
                        } else {
                            colorScheme.primaryContainer.copy(alpha = 0.18f)
                        },
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = if (bill.isSettled()) "ĐÃ THANH TOÁN" else "OPEN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bill.isSettled()) colorScheme.secondary else colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailBalanceCard(balance: GroupMemberBalance) {
    val colorScheme = MaterialTheme.colorScheme
    val isPositive = balance.balance > 0.0
    val isNegative = balance.balance < 0.0
    val amountColor =
        when {
            isPositive -> colorScheme.secondary
            isNegative -> colorScheme.error
            else -> colorScheme.onSurfaceVariant
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (balance.isMe) colorScheme.primary else colorScheme.outlineVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = balance.initial,
                    color = colorScheme.surfaceContainerLowest,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (balance.isMe) "${balance.name} (Bạn)" else balance.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        when {
                            isPositive -> "Được nhận"
                            isNegative -> "Đang nợ"
                            else -> "Đã cân bằng"
                        },
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text =
                    when {
                        isPositive -> "+${formatAmount(balance.balance)} đ"
                        isNegative -> "-${formatAmount(abs(balance.balance))} đ"
                        else -> "0 đ"
                    },
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DetailSettlementCard(
    settlement: SettlementSuggestion,
    isYourPayment: Boolean,
    onOpenBill: (() -> Unit)?,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onOpenBill != null) {
                        Modifier.clickable(onClick = onOpenBill)
                    } else {
                        Modifier
                    },
                ),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${settlement.fromName} trả ${settlement.toName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Để cân bằng số dư nhóm",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${formatAmount(settlement.amount)} đ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.primary,
                )
            }

            if (onOpenBill != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = colorScheme.primaryContainer.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text =
                            if (isYourPayment) {
                                "Chạm để mở hóa đơn và đánh dấu thanh toán"
                            } else {
                                "Chạm để xem hóa đơn chưa thanh toán liên quan"
                            },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMessageCard(
    title: String,
    message: String,
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariant,
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

private fun Bill.isSettled(): Boolean {
    return status == BillStatus.SETTLED
}

private fun formatAmount(amount: Double): String {
    return "%,.0f".format(amount).replace(",", ".")
}
