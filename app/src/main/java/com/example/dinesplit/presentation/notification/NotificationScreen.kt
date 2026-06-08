package com.example.dinesplit.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType

/**
 * Màn hình Thông báo (Notification Screen)
 * Hiển thị danh sách thông báo của người dùng như: Nhắc nhở chi tiêu, cập nhật chia tiền, và hoạt động bảng tin.
 * Cho phép lọc thông báo, đánh dấu đã đọc/chưa đọc, làm mới danh sách và xem thống kê tổng quát.
 */
@Composable
fun NotificationScreen(
    onBack: () -> Unit = {},
    onNotificationClick: (Notification) -> Unit = {},
) {
    val viewModel: NotificationViewModel = viewModel()
    // Flow danh sách thông báo và trạng thái UI từ ViewModel
    val notifications by viewModel.notifications.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // Bộ lọc thông báo hiện tại (Tất cả, Chưa đọc, Nhắc nhở, Chia tiền, Bảng tin)
    var selectedFilter by rememberSaveable { mutableStateOf(NotificationFilter.ALL) }
    
    // Danh sách thông báo đã qua bộ lọc và sắp xếp theo thời gian mới nhất trước
    val filteredNotifications =
        notifications
            .filter { it.matches(selectedFilter) }
            .sortedByDescending { it.createdAt }
            
    // Phân vùng thông báo mới (chưa đọc hoặc được nhận trong vòng 24 giờ qua) và thông báo trước đó
    val recentCutoffMillis = System.currentTimeMillis() - RECENT_NOTIFICATION_WINDOW_MS
    val (newNotifications, earlierNotifications) =
        filteredNotifications.partition { notification ->
            !notification.isRead || notification.createdAt >= recentCutoffMillis
        }

    AppScaffold(
        title = "Thông báo",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        },
        actions = {
            // Nút Làm mới danh sách thông báo
            IconButton(onClick = viewModel::refreshNotifications) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Làm mới thông báo"
                )
            }
            // Nút Đánh dấu tất cả thông báo là đã đọc (Chỉ khả dụng khi có thông báo chưa đọc)
            IconButton(
                onClick = viewModel::markAllAsRead,
                enabled = uiState.unreadCount > 0,
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Đánh dấu tất cả các thông báo là đã đọc"
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppDimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            // Hiển thị trạng thái đang tải (Loading)
            if (uiState.isLoading) {
                item {
                    LoadingBlock(message = "Đang tải thông báo...")
                }
            } else {
                // Hiển thị lỗi nếu có
                uiState.errorMessage?.let { message ->
                    item {
                        ErrorStateBlock(
                            title = "Không thể tải thông báo",
                            subtitle = message,
                            onRetryClick = { viewModel.refreshNotifications() },
                        )
                    }
                }

                // Card tổng quan chỉ số thông báo (chưa đọc, loại nhắc nhở, chia tiền,...)
                item {
                    NotificationOverviewCard(
                        unreadCount = uiState.unreadCount,
                        totalCount = notifications.size,
                        personalCount = notifications.count { it.isPersonalAlert() },
                        splitCount = notifications.count { it.isSplitAlert() },
                        activityCount = notifications.count { it.type == NotificationType.ACTIVITY_UPDATE },
                    )
                }

                // Thanh bộ lọc thông báo
                item {
                    NotificationFilterBar(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it },
                        notifications = notifications,
                    )
                }

                // Hiển thị màn hình trống nếu không có thông báo nào khớp bộ lọc
                if (filteredNotifications.isEmpty()) {
                    item {
                        EmptyStateBlock(
                            title = emptyTitleFor(selectedFilter),
                            subtitle = "Nhắc nhở chi tiêu, cập nhật chia tiền và hoạt động bảng tin sẽ xuất hiện ở đây khi chúng được tạo.",
                        )
                    }
                } else {
                    // Phân mục: Thông báo "Mới"
                    if (newNotifications.isNotEmpty()) {
                        notificationSection(
                            title = "Mới",
                            notifications = newNotifications,
                            viewModel = viewModel,
                            onNotificationClick = onNotificationClick,
                        )
                    }

                    // Phân mục: Thông báo "Trước đó"
                    if (earlierNotifications.isNotEmpty()) {
                        notificationSection(
                            title = "Trước đó",
                            notifications = earlierNotifications,
                            viewModel = viewModel,
                            onNotificationClick = onNotificationClick,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card Tổng quan thông báo
 * Hiển thị số lượng thông báo chưa đọc, tổng lượng và phân tích cụ thể
 * số lượng thông báo theo từng nhóm: Nhắc nhở, Chia tiền, Bảng tin.
 */
@Composable
private fun NotificationOverviewCard(
    unreadCount: Int,
    totalCount: Int,
    personalCount: Int,
    splitCount: Int,
    activityCount: Int,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Biểu tượng chuông thông báo hoạt động
                Box(
                    modifier =
                        Modifier
                            .size(AppDimens.space4Xl)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                ) {
                    Text(
                        text = "Trạng thái hộp thư",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "$unreadCount chưa đọc trong $totalCount thông báo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Hàng chỉ số metrics thứ nhất: Nhắc nhở chi tiêu & Chia tiền
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                NotificationMetric(
                    modifier = Modifier.weight(1f),
                    label = "Nhắc nhở",
                    value = personalCount.toString(),
                    color = MaterialTheme.colorScheme.secondary,
                )
                NotificationMetric(
                    modifier = Modifier.weight(1f),
                    label = "Chia tiền",
                    value = splitCount.toString(),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            // Hàng chỉ số metrics thứ hai: Bảng tin & Chưa đọc
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                NotificationMetric(
                    modifier = Modifier.weight(1f),
                    label = "Bảng tin",
                    value = activityCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                )
                NotificationMetric(
                    modifier = Modifier.weight(1f),
                    label = "Chưa đọc",
                    value = unreadCount.toString(),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Component hiển thị một ô chỉ số thông báo (Metric Box) có màu sắc tương ứng với loại thông báo
 */
@Composable
private fun NotificationMetric(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color,
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.large,
        color = color.copy(alpha = 0.10f),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Thanh bộ lọc các loại thông báo (Tất cả, Chưa đọc, Nhắc nhở, Chia tiền, Bảng tin)
 */
@Composable
private fun NotificationFilterBar(
    selectedFilter: NotificationFilter,
    onFilterSelected: (NotificationFilter) -> Unit,
    notifications: List<Notification>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        Text(
            text = "Bộ lọc",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        ) {
            items(NotificationFilter.entries) { filter ->
                NotificationFilterChip(
                    filter = filter,
                    selected = selectedFilter == filter,
                    count = notifications.count { it.matches(filter) },
                    onClick = { onFilterSelected(filter) },
                )
            }
        }
    }
}

/**
 * Chip bộ lọc hiển thị nhãn và số lượng tương ứng với từng loại bộ lọc
 */
@Composable
private fun NotificationFilterChip(
    filter: NotificationFilter,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    FilterChip(
        modifier = Modifier.widthIn(min = 92.dp),
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = "${filter.label} $count",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

/**
 * Helper thêm một phân vùng nhóm thông báo (Ví dụ phân vùng Mới/Trước đó) vào LazyColumn
 */
private fun LazyListScope.notificationSection(
    title: String,
    notifications: List<Notification>,
    viewModel: NotificationViewModel,
    onNotificationClick: (Notification) -> Unit,
) {
    item(key = "${title}_header") {
        NotificationSectionHeader(title = title, count = notifications.size)
    }

    items(notifications, key = { it.id }) { notification ->
        NotificationItemCard(
            notification = notification,
            onToggleRead = {
                // Đảo trạng thái đã đọc/chưa đọc của thông báo
                if (notification.isRead) {
                    viewModel.markAsUnread(notification.id)
                } else {
                    viewModel.markAsRead(notification.id)
                }
            },
            onClick = {
                // Đánh dấu là đã đọc và điều hướng đến chi tiết thông báo
                if (!notification.isRead) {
                    viewModel.markAsRead(notification.id)
                }
                onNotificationClick(notification)
            },
        )
    }
}

/**
 * Tiêu đề của phân mục thông báo (Ví dụ: "Mới 2" hoặc "Trước đó 15")
 */
@Composable
private fun NotificationSectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.spaceXs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Thẻ hiển thị một thông báo riêng lẻ (Notification Item Card).
 * Hiển thị chỉ báo chưa đọc, icon tương ứng theo loại thông báo, tiêu đề, thời gian trôi qua,
 * nhãn phân loại, nội dung phụ, và nút thay đổi trạng thái đọc/chưa đọc.
 */
@Composable
private fun NotificationItemCard(
    notification: Notification,
    onToggleRead: () -> Unit,
    onClick: () -> Unit,
) {
    val isUnread = !notification.isRead
    val icon = notificationIcon(notification.type)
    val accentColor = notificationAccentColor(notification)
    // Màu nền thay đổi theo trạng thái chưa đọc (sáng hơn) và đã đọc
    val containerColor =
        if (isUnread) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    val titleColor =
        if (isUnread) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        color = containerColor,
        tonalElevation = if (isUnread) AppDimens.level1 else AppDimens.level0,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = "Xem chi tiết thông báo",
                        onClick = onClick,
                    ),
        ) {
            // Thanh màu dọc nhỏ bên trái chỉ báo trạng thái chưa đọc
            if (isUnread) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(AppDimens.spaceXs)
                            .height(72.dp)
                            .clip(AppShapes.full)
                            .background(accentColor),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = AppDimens.spaceLg,
                            top = AppDimens.spaceMd,
                            end = AppDimens.spaceMd,
                            bottom = AppDimens.spaceMd,
                        ),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                // Icon của thông báo đặt trong vòng tròn màu nhạt
                Box(
                    modifier =
                        Modifier
                            .size(AppDimens.space4Xl)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (isUnread) 0.14f else 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(AppDimens.spaceXl),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Tiêu đề thông báo (In đậm hơn nếu chưa đọc)
                        Text(
                            modifier = Modifier.weight(1f),
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = titleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // Thời gian nhận thông báo (dạng 5p, 2g, 3n...)
                        Text(
                            modifier = Modifier.widthIn(min = 36.dp),
                            text = formatTimeAgo(notification.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Nội dung chi tiết của thông báo
                    Text(
                        text = notification.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Nhãn phân loại loại thông báo (Ví dụ: Nhắc nhở, Chia tiền, Bảng tin)
                        NotificationTypePill(
                            label = notificationTypeLabel(notification),
                            isUnread = isUnread,
                            color = accentColor,
                        )
                        // Nút chuyển đổi nhanh trạng thái đã đọc / chưa đọc thông báo
                        IconButton(
                            onClick = onToggleRead,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = if (notification.isRead) {
                                    Icons.Default.MarkEmailUnread
                                } else {
                                    Icons.Default.MarkEmailRead
                                },
                                contentDescription = if (notification.isRead) {
                                    "Đánh dấu thông báo là chưa đọc"
                                } else {
                                    "Đánh dấu thông báo là đã đọc"
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Nhãn phân loại hình tròn hiển thị loại thông báo (Ví dụ: Nhắc nhở, Chia tiền, Bảng tin...)
 */
@Composable
private fun NotificationTypePill(
    label: String,
    isUnread: Boolean,
    color: Color,
) {
    Surface(
        shape = AppShapes.full,
        color = color.copy(alpha = 0.10f),
        contentColor = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceSm),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Hiển thị chấm tròn nhỏ màu đỏ/nhấn mạnh nếu là thông báo chưa đọc
            if (isUnread) {
                Box(
                    modifier =
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(color),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Xác định Icon phù hợp cho mỗi loại thông báo (NotificationType)
 */
private fun notificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.PAYMENT_COMPLETED -> Icons.Filled.CheckCircle
        NotificationType.PAYMENT_PENDING -> Icons.Filled.WarningAmber
        NotificationType.BILL_CREATED -> Icons.Filled.Payments
        NotificationType.SPLIT_COMPLETED -> Icons.Filled.CheckCircle
        NotificationType.TRANSACTION_ALERT -> Icons.Filled.Savings
        NotificationType.ACTIVITY_UPDATE -> Icons.Filled.NotificationsActive
        NotificationType.OTHER -> Icons.Filled.NotificationsActive
    }
}

/**
 * Xác định màu sắc nhấn mạnh (Accent Color) tương ứng cho mỗi loại thông báo chưa đọc.
 * Nếu đã đọc, trả về màu Outline xám trung tính.
 */
@Composable
private fun notificationAccentColor(notification: Notification): Color {
    if (notification.isRead) return MaterialTheme.colorScheme.outline
    return when {
        notification.isPersonalAlert() -> MaterialTheme.colorScheme.secondary
        notification.isSplitAlert() -> MaterialTheme.colorScheme.tertiary
        notification.type == NotificationType.ACTIVITY_UPDATE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Lấy nhãn chuỗi đại diện tiếng Việt cho từng nhóm thông báo
 */
private fun notificationTypeLabel(notification: Notification): String {
    return when {
        notification.isPersonalAlert() -> "Nhắc nhở"
        notification.isSplitAlert() -> "Chia tiền"
        notification.type == NotificationType.ACTIVITY_UPDATE -> "Bảng tin"
        else -> "Hệ thống"
    }
}

/**
 * Định nghĩa Enum cho các lựa chọn Bộ lọc thông báo trên UI
 */
private enum class NotificationFilter(val label: String) {
    ALL("Tất cả"),
    UNREAD("Chưa đọc"),
    PERSONAL("Nhắc nhở"),
    SPLIT("Chia tiền"),
    FEED("Bảng tin"),
}

/**
 * Kiểm tra xem một thông báo có khớp với điều kiện lọc đang chọn hay không
 */
private fun Notification.matches(filter: NotificationFilter): Boolean {
    return when (filter) {
        NotificationFilter.ALL -> true
        NotificationFilter.UNREAD -> !isRead
        NotificationFilter.PERSONAL -> isPersonalAlert()
        NotificationFilter.SPLIT -> isSplitAlert()
        NotificationFilter.FEED -> type == NotificationType.ACTIVITY_UPDATE
    }
}

/**
 * Kiểm tra xem thông báo này có phải loại Nhắc nhở cá nhân (Ví dụ nhắc nhở chi tiêu)
 */
private fun Notification.isPersonalAlert(): Boolean {
    return type == NotificationType.TRANSACTION_ALERT
}

/**
 * Kiểm tra xem thông báo này có thuộc nhóm Chia tiền / Hóa đơn nhóm hay không
 */
private fun Notification.isSplitAlert(): Boolean {
    return type in
        setOf(
            NotificationType.PAYMENT_COMPLETED,
            NotificationType.PAYMENT_PENDING,
            NotificationType.BILL_CREATED,
            NotificationType.SPLIT_COMPLETED,
        )
}

/**
 * Chuẩn bị tiêu đề hiển thị phù hợp khi danh sách thông báo trống theo từng bộ lọc
 */
private fun emptyTitleFor(filter: NotificationFilter): String {
    return when (filter) {
        NotificationFilter.ALL -> "Chưa có thông báo nào"
        NotificationFilter.UNREAD -> "Không có thông báo chưa đọc"
        NotificationFilter.PERSONAL -> "Không có nhắc nhở chi tiêu"
        NotificationFilter.SPLIT -> "Không có cập nhật chia tiền"
        NotificationFilter.FEED -> "Không có cập nhật bảng tin"
    }
}

/**
 * Định dạng thời gian tương đối trôi qua (Ví dụ: "vừa xong", "15p", "2g", "4n", "2t"...)
 */
private fun formatTimeAgo(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - epochMillis
    val diffMinutes = diffMillis / (1000 * 60)
    val diffHours = diffMillis / (1000 * 60 * 60)
    val diffDays = diffMillis / (1000 * 60 * 60 * 24)

    return when {
        diffMinutes < 1 -> "vừa xong"
        diffMinutes < 60 -> "${diffMinutes}p"
        diffHours < 24 -> "${diffHours}g"
        diffDays < 7 -> "${diffDays}n"
        else -> "${diffDays / 7}t"
    }
}

// Cửa sổ thời gian xác định một thông báo là "Gần đây" (24 tiếng)
private const val RECENT_NOTIFICATION_WINDOW_MS = 24L * 60L * 60L * 1000L
