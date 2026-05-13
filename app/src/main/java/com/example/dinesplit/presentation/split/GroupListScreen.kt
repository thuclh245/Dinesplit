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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.Group
import kotlin.math.abs

data class GroupItem(
    val title: String,
    val time: String,
    val statusText: String,
    val isSettled: Boolean,
    val statusType: GroupStatusType,
    val avatarCount: Int
)

enum class GroupStatusType { OWE, RECEIVE, SETTLED }

@Composable
fun GroupListScreen(
    onNavigateToGroupDetail: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToAllGroups: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val viewModel = remember { GroupListViewModel(AppContainer.splitRepository(context)) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = { GroupTopBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateGroup,
                containerColor = colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo nhóm", tint = colorScheme.onPrimaryContainer)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FinancialSummaryCard()
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Hoạt động gần đây",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Xem tất cả",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToAllGroups() }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                uiState.error != null -> {
                    item {
                        GroupMessageCard(
                            title = "Không thể tải nhóm",
                            message = uiState.error.orEmpty()
                        )
                    }
                }

                uiState.groups.isEmpty() -> {
                    item {
                        GroupMessageCard(
                            title = "Chưa có nhóm nào",
                            message = "Bấm nút + để tạo nhóm đầu tiên."
                        )
                    }
                }

                else -> {
                    items(uiState.groups, key = { group -> group.id }) { group ->
                        GroupCardItem(
                            group = group.toGroupItem(),
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
fun GroupTopBar() {
    val colorScheme = MaterialTheme.colorScheme
    TopAppBar(
        title = { Text("Nhóm của bạn", fontWeight = FontWeight.Bold) },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Notifications, contentDescription = "Thông báo")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.surface.copy(alpha = 0.9f)
        )
    )
}

@Composable
fun FinancialSummaryCard() {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BẠN ĐANG NỢ",
                    fontSize = 11.sp,
                    color = colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "0 đ",
                    fontSize = 22.sp,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Settle Up", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.onPrimary)
                }
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BẠN ĐƯỢC TRẢ",
                    fontSize = 11.sp,
                    color = colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "0 đ",
                    fontSize = 22.sp,
                    color = colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondary),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Remind", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSecondary)
                }
            }
        }
    }
}

@Composable
private fun GroupMessageCard(
    title: String,
    message: String
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
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

@Composable
fun GroupCardItem(group: GroupItem, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    val statusColor = when (group.statusType) {
        GroupStatusType.OWE -> colorScheme.error
        GroupStatusType.RECEIVE -> colorScheme.secondary
        GroupStatusType.SETTLED -> colorScheme.onSurfaceVariant
    }

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
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = colorScheme.outline)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        for (i in 0 until minOf(3, group.avatarCount)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceContainerHigh)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = group.time, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                }
            }

            if (group.isSettled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = group.statusText.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = group.statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

private fun Group.toGroupItem(): GroupItem {
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
