package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- MÔ HÌNH DỮ LIỆU TẠM (MOCK DATA) ---
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
    onNavigateToGroupDetail: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToAllGroups: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val mockGroups = listOf(
        GroupItem("Chuyến đi Đà Lạt", "Hôm qua", "Bạn nợ 50.000 đ", false, GroupStatusType.OWE, 4),
        GroupItem("Team ăn uống", "2 giờ trước", "Nhận 120.000 đ", false, GroupStatusType.RECEIVE, 2),
        GroupItem("Cà phê cuối tuần", "Thứ 7", "Đã thanh toán", true, GroupStatusType.SETTLED, 3)
    )

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
        },
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

            items(mockGroups) { group ->
                GroupCardItem(group = group, onClick = onNavigateToGroupDetail)
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
        // --- THẺ 1: BẠN ĐANG NỢ ---
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
                    text = "450.000 đ",
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

        // --- THẺ 2: BẠN ĐƯỢC TRẢ ---
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
                    text = "1.280.000 đ",
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
