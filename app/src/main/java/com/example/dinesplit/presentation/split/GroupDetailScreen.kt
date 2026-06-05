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
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.ClickableAppCard
import com.example.dinesplit.core.ui.ElevatedAppCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
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
import com.example.dinesplit.core.ui.AppIconButton
import com.example.dinesplit.core.ui.TertiaryButton
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillStatus
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.SplitMethod
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.ui.theme.AppColors
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
            val headerMembers = buildHeaderMembers(
                members = uiState.members,
                balances = uiState.memberBalances,
            )
            DetailTopBar(
                groupName = uiState.group?.name ?: "Chi tiết nhóm",
                memberCount = maxOf(
                    headerMembers.size,
                    uiState.group?.memberCount ?: 0,
                ),
                members = headerMembers,
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
                        .background(brush = AppColors.primaryGradient)
                        .clickable { onNavigateToCreateBill() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm hóa đơn",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(AppDimens.iconLg),
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = AppDimens.spaceXl),
            contentPadding = PaddingValues(top = AppDimens.spaceLg, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            when {
                uiState.isLoading ->
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = AppDimens.space4Xl),
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

private fun buildHeaderMembers(
    members: List<Member>,
    balances: List<GroupMemberBalance>,
): List<Member> {
    val memberIds = members.mapTo(mutableSetOf()) { it.id }
    val missingMembers = balances
        .filter { balance -> balance.memberId !in memberIds }
        .map { balance ->
            Member(
                id = balance.memberId,
                name = balance.name,
                initial = balance.initial,
                isMe = balance.isMe,
            )
        }

    return members + missingMembers
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
                .padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            onClick = onBack,
            contentDescription = "Quay lại",
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colorScheme.outline,
                )
            }
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = AppDimens.spaceXs, end = AppDimens.spaceSm),
        ) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$memberCount thành viên",
                style = MaterialTheme.typography.bodySmall,
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
            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Rời nhóm",
                tint = colorScheme.outline,
                modifier = Modifier.size(AppDimens.spaceXl),
            )
        }

        if (isOwner) {
            AppIconButton(
                onClick = onDeleteClick,
                contentDescription = "Xóa nhóm",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                }
            )
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
    val visibleTargetCount = minOf(3, maxOf(fallbackCount, members.size).coerceAtLeast(1))
    Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
        visibleMembers.forEach { member ->
            DetailMemberAvatar(
                imageUrl = member.avatarUrl,
                name = member.name.ifBlank { member.initial },
                seed = member.id
            )
        }
        repeat(visibleTargetCount - visibleMembers.size) { index ->
            DetailMemberAvatar(
                imageUrl = null,
                name = fallbackName,
                seed = "$fallbackName-fallback-$index"
            )
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
        size = AppDimens.space2Xl,
        modifier = Modifier.border(
            width = 1.dp,
            color = colorScheme.surfaceContainerLowest,
            shape = CircleShape
        ),
        fallbackContainerColor = detailAvatarColor(seed),
        fallbackContentColor = MaterialTheme.colorScheme.onPrimary
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
            TertiaryButton(
                text = "Rời nhóm",
                onClick = onConfirm,
                enabled = !isLeaving,
                isLoading = isLeaving,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.error,
                    disabledContentColor = colorScheme.error.copy(alpha = 0.5f),
                )
            )
        },
        dismissButton = {
            TertiaryButton(
                text = "Hủy",
                onClick = onDismiss,
                enabled = !isLeaving,
            )
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
            TertiaryButton(
                text = "Xóa",
                onClick = onConfirm,
                enabled = !isDeleting,
                isLoading = isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.error,
                    disabledContentColor = colorScheme.error.copy(alpha = 0.5f),
                )
            )
        },
        dismissButton = {
            TertiaryButton(
                text = "Hủy",
                onClick = onDismiss,
                enabled = !isDeleting,
            )
        },
    )
}

@Composable
private fun DetailSummaryCard(
    totalExpense: Double,
    yourBalance: Double,
) {
    val colorScheme = MaterialTheme.colorScheme

    ElevatedAppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceXl),
    ) {
        Text(
            text = "TỔNG CHI TIÊU NHÓM",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
            color = colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.padding(top = AppDimens.spaceSm, bottom = AppDimens.spaceLg),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
            Text(
                text = "đ",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
            shape = AppShapes.full,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (yourBalance < 0.0) colorScheme.error else colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                Text(
                    text = formatBalanceLabel(yourBalance),
                    color = if (yourBalance < 0.0) colorScheme.error else colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
        shape = AppShapes.full,
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
                .clip(AppShapes.full)
                .then(
                    if (selected) {
                        Modifier.background(
                            brush = AppColors.primaryGradient,
                            shape = AppShapes.full,
                        )
                    } else {
                        Modifier.background(Color.Transparent)
                    },
                )
                .clickable { onClick() }
                .padding(vertical = AppDimens.spaceMd),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun DetailSectionTitle(title: String) {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        color = colorScheme.onSurface,
        modifier = Modifier.padding(start = AppDimens.spaceXs),
    )
}

@Composable
private fun DetailBillCard(
    bill: Bill,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    ClickableAppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(AppDimens.space4Xl)
                        .clip(AppShapes.large)
                        .background(colorScheme.primaryContainer.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(AppDimens.spaceXl),
                )
            }

            Spacer(modifier = Modifier.width(AppDimens.spaceMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                Text(
                    text = "${formatSplitMethod(bill.method)} • ${formatDate(bill.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(AppDimens.spaceMd))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatAmount(bill.totalAmount)} đ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = colorScheme.primary,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                Surface(
                    color =
                        if (bill.isSettled()) {
                            colorScheme.secondaryContainer
                        } else {
                            colorScheme.primaryContainer.copy(alpha = 0.18f)
                        },
                    shape = AppShapes.full,
                ) {
                    Text(
                        text = if (bill.isSettled()) "ĐÃ THANH TOÁN" else "OPEN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.spaceLg),
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
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(modifier = Modifier.width(AppDimens.spaceMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (balance.isMe) "${balance.name} (Bạn)" else balance.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
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
                    style = MaterialTheme.typography.bodySmall,
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
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
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

    ClickableAppCard(
        onClick = onOpenBill ?: {},
        enabled = onOpenBill != null,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.padding(AppDimens.spaceLg)) {
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
                Spacer(modifier = Modifier.width(AppDimens.spaceMd))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${settlement.fromName} trả ${settlement.toName}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Để cân bằng số dư nhóm",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${formatAmount(settlement.amount)} đ",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = colorScheme.primary,
                )
            }

            if (onOpenBill != null) {
                Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                Surface(
                    color = colorScheme.primaryContainer.copy(alpha = 0.18f),
                    shape = AppShapes.medium,
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
                                .padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceSm),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
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

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceLg),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
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
