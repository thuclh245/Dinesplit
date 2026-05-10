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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- MÔ HÌNH DỮ LIỆU TẠM ---
private data class Cg_Member(
    val name: String,
    val initial: String,
    val phone: String,
    val isSelected: Boolean,
    val avatarColor: @Composable () -> Color
)

@Composable
fun CreateGroupScreen(
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("Ăn uống", "Du lịch", "Nhà ở", "Khác")
    val selectedCategory = "Du lịch"

    val friends = listOf(
        Cg_Member("Minh", "M", "090 123 4567", true, { colorScheme.onSurfaceVariant }),
        Cg_Member("Thanh Hằng", "T", "091 987 6543", true, { colorScheme.outline }),
        Cg_Member("Tuấn Anh", "A", "098 555 1234", false, { colorScheme.outlineVariant })
    )

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = { Cg_TopBar(onBack = onBack) },
        bottomBar = { Cg_BottomAction(selectedCount = 3) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Cg_GroupInfoCard(
                    groupName = groupName,
                    onNameChange = { groupName = it },
                    categories = categories,
                    selectedCategory = selectedCategory
                )
            }

            item {
                Cg_AddMembersSection(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    friends = friends
                )
            }
        }
    }
}

// --- CÁC COMPONENT GIAO DIỆN ---

@Composable
private fun Cg_TopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = colorScheme.onSurfaceVariant)
        }

        Text(
            text = "Tạo nhóm mới",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )

        Text(
            text = "Lưu",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
            modifier = Modifier.clickable { /* Xử lý lưu nháp */ }
        )
    }
}

@Composable
private fun Cg_GroupInfoCard(
    groupName: String,
    onNameChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = colorScheme.outline, modifier = Modifier.size(40.dp))
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer)
                        .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape)
                        .clickable { /* Chọn ảnh */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = colorScheme.surfaceContainerLowest, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (groupName.isEmpty()) {
                    Text("Tên nhóm (VD: Chuyến đi Vũng Tàu)", color = colorScheme.outline, fontSize = 14.sp)
                }
                BasicTextField(
                    value = groupName,
                    onValueChange = onNameChange,
                    textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer)
                            .clickable { /* Chọn category */ }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Cg_AddMembersSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    friends: List<Cg_Member>
) {
    val colorScheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = "THÊM THÀNH VIÊN",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainerLowest, RoundedCornerShape(50))
                .border(1.dp, colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colorScheme.outline, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text("Tìm kiếm bạn bè...", color = colorScheme.outline, fontSize = 14.sp)
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Gợi ý", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                friends.forEachIndexed { index, friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Toggle chọn */ }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(friend.avatarColor()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(friend.initial, color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colorScheme.onSurface)
                                Text(friend.phone, fontSize = 12.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }

                        if (friend.isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Đã chọn", tint = colorScheme.primaryContainer)
                        } else {
                            // Đã thay RadioButtonUnchecked bằng một nút Add hình tròn tự vẽ
                            Box(
                                modifier = Modifier.size(24.dp).border(2.dp, colorScheme.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Thêm", tint = colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (index < friends.size - 1) {
                        HorizontalDivider(color = colorScheme.surfaceContainerHigh)
                    }
                }
            }
        }
    }
}

@Composable
private fun Cg_BottomAction(selectedCount: Int) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.95f))
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                    for (color in listOf(colorScheme.onSurfaceVariant, colorScheme.outline, colorScheme.outlineVariant)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainer)
                            .border(2.dp, colorScheme.surfaceContainerLowest, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+0", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Đã chọn $selectedCount thành viên", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = { /* Xử lý tạo nhóm */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(50)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(listOf(colorScheme.primaryContainer, colorScheme.primary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("TẠO NHÓM", color = colorScheme.surfaceContainerLowest, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}
