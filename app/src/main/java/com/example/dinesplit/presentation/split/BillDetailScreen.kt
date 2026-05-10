package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- MÔ HÌNH DỮ LIỆU TẠM ---
private data class BdSplitMember(
    val name: String,
    val role: String,
    val initial: String,
    val amount: String,
    val status: String,
    val isPaid: Boolean,
    val isMe: Boolean
)

@Composable
fun BillDetailScreen(
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val members = listOf(
        BdSplitMember("Minh", "CHỦ CHI", "M", "400.000 đ", "ĐÃ TRẢ", isPaid = true, isMe = false),
        BdSplitMember("Thanh Hằng", "", "T", "400.000 đ", "ĐÃ TRẢ", isPaid = true, isMe = false),
        BdSplitMember("Bạn", "", "B", "400.000 đ", "CHƯA TRẢ", isPaid = false, isMe = true)
    )

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = { BdTopBar(onBack = onBack) },
        bottomBar = { BdBottomAction() }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { BdReceiptHeaderCard() }
            item { BdSplitBreakdown(members) }
            item { BdFooterInfo() }
        }
    }
}

// --- CÁC COMPONENT GIAO DIỆN ---

@Composable
private fun BdTopBar(onBack: () -> Unit) {
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = colorScheme.primary)
        }

        Text(
            text = "Bill Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )

        IconButton(onClick = { /* Mở menu tùy chọn */ }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = "Thêm", tint = colorScheme.primary)
        }
    }
}

@Composable
private fun BdReceiptHeaderCard() {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Food
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🍲", fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Lẩu Haidilao", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Hôm nay, 10 Tháng 4", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "1.200.000 đ", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = colorScheme.onSurface, letterSpacing = (-1).sp)

            Spacer(modifier = Modifier.height(24.dp))

            // Người thanh toán
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colorScheme.surfaceContainerLow, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(colorScheme.onSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = colorScheme.surfaceContainerLowest, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Thanh toán bởi ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurfaceVariant)
                Text(text = "Minh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Đường kẻ ngang đứt nét (Mô phỏng bằng đường nét liền mờ)
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BdSplitBreakdown(members: List<BdSplitMember>) {
    val colorScheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = "CHI TIẾT CHIA TIỀN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                members.forEachIndexed { index, member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (member.isMe) colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent)
                            .height(IntrinsicSize.Min), // Để cái vạch màu cam bằng đúng chiều cao Row
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vạch màu cam bên trái nếu là "Bạn"
                        if (member.isMe) {
                            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(colorScheme.primary))
                        } else {
                            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(Color.Transparent))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (member.isMe) colorScheme.primary else colorScheme.outlineVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(member.initial, color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(member.name, fontWeight = FontWeight.Bold, color = if (member.isMe) colorScheme.primary else colorScheme.onSurface)
                                    if (member.role.isNotEmpty()) {
                                        Text(member.role, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(member.amount, fontWeight = FontWeight.Bold, color = if (member.isMe) colorScheme.primary else colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(4.dp))

                                if (member.isPaid) {
                                    if (member.role == "CHỦ CHI") {
                                        Text(member.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    } else {
                                        Row(
                                            modifier = Modifier.background(colorScheme.secondaryContainer, RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = colorScheme.secondary, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(member.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colorScheme.secondary)
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.background(colorScheme.surfaceContainerHigh, RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(member.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    if (index < members.size - 1) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BdFooterInfo() {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mã giao dịch", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurfaceVariant)
                Text("#HD-82931", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Phương thức", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurfaceVariant)
                Text("Ví DineSplit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BdBottomAction() {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        Button(
            onClick = { /* Xử lý trả tiền */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primaryContainer),
            shape = RoundedCornerShape(50),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Đánh dấu đã trả cho Minh", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.surfaceContainerLowest)
        }
    }
}
