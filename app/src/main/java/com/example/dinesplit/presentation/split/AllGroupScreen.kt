package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- MÔ HÌNH DỮ LIỆU TẠM ---
private data class Ag_GroupInfo(
    val title: String,
    val date: String,
    val icon: ImageVector,
    val iconColor: @Composable () -> Color,
    val iconBg: @Composable () -> Color,
    val statusText: String,
    val statusColor: @Composable () -> Color,
    val isSettled: Boolean,
    val avatarCount: Int,
    val extraCount: Int = 0,
    val isDimmed: Boolean = false
)

@Composable
fun AllGroupsScreen(
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val filters = listOf("Tất cả", "Đang nợ", "Được trả", "Đã xong")
    var selectedFilter by remember { mutableStateOf("Tất cả") }

    // Đã thay thế bằng các Core Icons an toàn 100%
    val mockGroups = listOf(
        Ag_GroupInfo("Chuyến đi Đà Lạt", "Hôm qua", Icons.Default.Place, { colorScheme.primary }, { colorScheme.primaryContainer.copy(alpha = 0.2f) }, "Bạn nợ 50.000 đ", { colorScheme.error }, false, 3),
        Ag_GroupInfo("Ăn trưa công ty", "2 ngày trước", Icons.Default.Person, { colorScheme.secondary }, { colorScheme.secondaryContainer }, "Nhận 120.000 đ", { colorScheme.secondary }, false, 3, 1),
        Ag_GroupInfo("Nhà chung", "Tuần trước", Icons.Default.Home, { colorScheme.onSurfaceVariant }, { colorScheme.surfaceContainer }, "Đã thanh toán", { colorScheme.onSurfaceVariant }, true, 2, 0, true),
        Ag_GroupInfo("Tiệc sinh nhật", "Tháng trước", Icons.Default.Favorite, { colorScheme.primary }, { colorScheme.primaryContainer.copy(alpha = 0.2f) }, "Bạn nợ 250.000 đ", { colorScheme.error }, false, 3, 2),
        Ag_GroupInfo("Cà phê sáng", "Hôm nay", Icons.Default.Star, { colorScheme.primary }, { colorScheme.primaryContainer.copy(alpha = 0.2f) }, "Đã thanh toán", { colorScheme.onSurfaceVariant }, true, 3)
    )

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = { Ag_TopBar(onBack = onBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Ag_FilterChips(
                filters = filters,
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(mockGroups) { group ->
                    Ag_GroupCard(group = group)
                }
            }
        }
    }
}

// --- CÁC COMPONENT GIAO DIỆN ---

@Composable
private fun Ag_TopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Tất cả nhóm",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        }

        IconButton(onClick = { /* Mở tìm kiếm */ }) {
            Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = colorScheme.primary)
        }
    }
}

@Composable
private fun Ag_FilterChips(
    filters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLow)
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = filter,
                    color = if (isSelected) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun Ag_GroupCard(group: Ag_GroupInfo) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (group.isDimmed) 0.7f else 1f)
            .clickable { /* Điều hướng tới chi tiết nhóm */ },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(group.iconBg()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(group.icon, contentDescription = null, tint = group.iconColor(), modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = group.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = group.date,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                    val avatarColors = listOf(colorScheme.onSurfaceVariant, colorScheme.outline, colorScheme.outlineVariant)
                    for (i in 0 until group.avatarCount) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(avatarColors[i % avatarColors.size])
                                .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape)
                        )
                    }
                    if (group.extraCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colorScheme.surfaceContainer)
                                .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+${group.extraCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (group.isSettled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colorScheme.surfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = group.statusText.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Text(
                        text = group.statusText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = group.statusColor(),
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
    }
}
