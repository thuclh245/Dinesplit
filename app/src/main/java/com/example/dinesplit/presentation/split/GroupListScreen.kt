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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Group
import com.example.dinesplit.core.ui.IconEmptyStateBlock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.ClickableAppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.AppIconButton
import com.example.dinesplit.core.ui.SmallButton
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import kotlin.math.abs

data class GroupItem(
    val title: String,
    val time: String,
    val statusText: String,
    val isSettled: Boolean,
    val statusType: GroupStatusType,
    val memberAvatars: List<Member>,
    val avatarCount: Int
)

enum class GroupStatusType { OWE, RECEIVE, SETTLED }

@Composable
fun GroupListScreen(
    onNavigateToGroupDetail: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToAllGroups: () -> Unit,
    bottomPadding: Dp = 80.dp,
    onBack: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    notificationUnreadCount: Int = 0,
    onNavigateToSettleSummary: (String) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val viewModel = remember {
        GroupListViewModel(
            repository = AppContainer.splitRepository(context),
            profileRepository = AppContainer.profileRepository(context)
        )
    }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = {
            GroupTopBar(
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onOpenNotifications = onOpenNotifications,
                notificationUnreadCount = notificationUnreadCount,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateGroup,
                containerColor = colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = bottomPadding),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo nhóm", tint = colorScheme.onPrimaryContainer)
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = AppDimens.screenHorizontal),
            contentPadding = PaddingValues(top = AppDimens.spaceLg, bottom = bottomPadding + 100.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            item {
                FinancialSummaryCard(
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

            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "Hoạt động gần đây",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                    )
                    Text(
                        text = "Xem tất cả",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToAllGroups() },
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = AppDimens.space2Xl),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                uiState.error != null -> {
                    item {
                        GroupMessageCard(
                            title = "Không thể tải nhóm",
                            message = uiState.error.orEmpty(),
                        )
                    }
                }

                uiState.groups.isEmpty() -> {
                    item {
                        IconEmptyStateBlock(
                            title = "Chưa có nhóm nào",
                            subtitle = "Tạo nhóm đầu tiên để chia tiền ăn uống với bạn bè.",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(64.dp)
                                )
                            },
                            actionText = "Tạo nhóm ngay",
                            onActionClick = onNavigateToCreateGroup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppDimens.spaceLg)
                        )
                    }
                }

                else -> {
                    items(uiState.groups, key = { group -> group.id }) { group ->
                        GroupCardItem(
                            group = group.toGroupItem(
                                members = uiState.membersByGroup[group.id].orEmpty()
                            ),
                            onClick = { onNavigateToGroupDetail(group.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTopBar(
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    notificationUnreadCount: Int,
) {
    val colorScheme = MaterialTheme.colorScheme
    TopAppBar(
        navigationIcon = {
            AppIconButton(
                onClick = onBack,
                contentDescription = "Quay lại",
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            )
        },
        title = { Text("Nhóm của bạn", fontWeight = FontWeight.Bold) },
        actions = {
            AppIconButton(
                onClick = onOpenSearch,
                contentDescription = "Tìm kiếm",
                icon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            AppIconButton(
                onClick = onOpenNotifications,
                contentDescription = "Thông báo",
                icon = { NotificationBellIcon(notificationUnreadCount = notificationUnreadCount) }
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.surface.copy(alpha = 0.9f),
            ),
    )
}

@Composable
private fun NotificationBellIcon(notificationUnreadCount: Int) {
    BadgedBox(
        badge = {
            if (notificationUnreadCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Text(
                        text = if (notificationUnreadCount > 99) "99+" else notificationUnreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription =
                if (notificationUnreadCount > 0) {
                    "Thông báo, $notificationUnreadCount chưa đọc"
                } else {
                    "Thông báo"
                },
        )
    }
}

@Composable
fun FinancialSummaryCard(
    amountYouOwe: Double,
    amountYouAreOwed: Double,
    onSettleUpClick: () -> Unit,
    onRemindClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
    ) {
        AppCard(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(AppDimens.spaceLg),
        ) {
            Column {
                Text(
                    text = "BẠN ĐANG NỢ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                Text(
                    text = formatVnd(amountYouOwe),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                SmallButton(
                    text = "Chốt sổ",
                    onClick = onSettleUpClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    shape = CircleShape,
                )
            }
        }

        AppCard(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(AppDimens.spaceLg),
        ) {
            Column {
                Text(
                    text = "BẠN ĐƯỢC TRẢ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                Text(
                    text = formatVnd(amountYouAreOwed),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                SmallButton(
                    text = "Nhắc nợ",
                    onClick = onRemindClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondary,
                        contentColor = colorScheme.onSecondary
                    ),
                    shape = CircleShape,
                )
            }
        }
    }
}

@Composable
private fun GroupMessageCard(
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

@Composable
fun GroupCardItem(
    group: GroupItem,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    val statusColor =
        when (group.statusType) {
            GroupStatusType.OWE -> colorScheme.error
            GroupStatusType.RECEIVE -> colorScheme.secondary
            GroupStatusType.SETTLED -> colorScheme.onSurfaceVariant
        }

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
                        .size(AppDimens.textFieldMinHeight)
                        .clip(AppShapes.large)
                        .background(colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = colorScheme.outline)
            }

            Spacer(modifier = Modifier.width(AppDimens.spaceLg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupMemberAvatarStack(
                        members = group.memberAvatars,
                        fallbackCount = group.avatarCount,
                        fallbackName = group.title
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                    Text(text = group.time, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                }
            }

            if (group.isSettled) {
                Box(
                    modifier =
                        Modifier
                            .clip(AppShapes.full)
                            .background(colorScheme.surfaceContainerHigh)
                            .padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
                ) {
                    Text(
                        text = group.statusText.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = group.statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = statusColor,
                )
            }
        }
    }
}

@Composable
private fun GroupMemberAvatarStack(
    members: List<Member>,
    fallbackCount: Int,
    fallbackName: String
) {
    val visibleMembers = members.take(3)
    Row(horizontalArrangement = Arrangement.spacedBy(-AppDimens.spaceSm)) {
        if (visibleMembers.isEmpty()) {
            repeat(minOf(3, fallbackCount.coerceAtLeast(1))) { index ->
                GroupMemberAvatar(
                    imageUrl = null,
                    name = fallbackName,
                    seed = "$fallbackName-$index"
                )
            }
        } else {
            visibleMembers.forEach { member ->
                GroupMemberAvatar(
                    imageUrl = member.avatarUrl,
                    name = member.name.ifBlank { member.initial },
                    seed = member.id
                )
            }
        }
    }
}

@Composable
private fun GroupMemberAvatar(
    imageUrl: String?,
    name: String,
    seed: String
) {
    val colorScheme = MaterialTheme.colorScheme
    DineAvatarImage(
        imageUrl = imageUrl,
        name = name,
        size = AppDimens.spaceXl,
        modifier = Modifier.border(
            width = 1.dp,
            color = colorScheme.surfaceContainerLowest,
            shape = CircleShape
        ),
        fallbackContainerColor = avatarColor(seed),
        fallbackContentColor = MaterialTheme.colorScheme.onPrimary
    )
}

@Composable
private fun avatarColor(seed: String): Color {
    val colorScheme = MaterialTheme.colorScheme
    val colors = listOf(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary,
        colorScheme.error
    )
    return colors[(seed.hashCode() and Int.MAX_VALUE) % colors.size]
}

private fun Group.toGroupItem(members: List<Member>): GroupItem {
    val statusType = when {
        yourBalance < 0.0 -> GroupStatusType.OWE
        yourBalance > 0.0 -> GroupStatusType.RECEIVE
        else -> GroupStatusType.SETTLED
    }

    return GroupItem(
        title = name,
        time = formatGroupAge(createdAt),
        statusText = formatGroupStatus(yourBalance),
        isSettled = yourBalance == 0.0,
        statusType = statusType,
        memberAvatars = members,
        avatarCount = memberCount.coerceAtLeast(1)
    )
}

private fun formatGroupStatus(balance: Double): String {
    return when {
        balance < 0.0 -> "Bạn nợ ${formatVnd(abs(balance))}"
        balance > 0.0 -> "Nhận ${formatVnd(balance)}"
        else -> "Đã thanh toán"
    }
}

private fun formatVnd(amount: Double): String {
    return "%,.0f đ".format(amount).replace(",", ".")
}

private fun formatGroupAge(createdAt: Long): String {
    if (createdAt <= 0L) return "Vừa xong"

    val diffMillis = System.currentTimeMillis() - createdAt
    val minutes = diffMillis / 60_000
    val hours = diffMillis / 3_600_000
    val days = diffMillis / 86_400_000

    return when {
        minutes < 1 -> "Vừa xong"
        minutes < 60 -> "$minutes phút trước"
        hours < 24 -> "$hours giờ trước"
        days == 1L -> "Hôm qua"
        days < 7 -> "$days ngày trước"
        else -> "Trước đó"
    }
}
