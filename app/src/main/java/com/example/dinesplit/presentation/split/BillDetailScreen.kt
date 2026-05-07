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
import androidx.compose.runtime.*

// --- KHO KHAI BÁO MÀU SẮC (Cách ly 100% bằng tiền tố Bd_) ---
private val BdBg = Color(0xFFF9F9F9)
private val BdSurfaceWhite = Color(0xFFFFFFFF)

private val BdOrangeStart = Color(0xFFE2725B)
private val BdOrangeEnd = Color(0xFF9F402D)
private val BdOrangeLightBg = Color(0xFFFFF0ED) // Nền cam nhạt cho Bạn
private val BdOrangeIconBg = Color(0xFFFFDAD3)

private val BdTealText = Color(0xFF006B5B)
private val BdTealBg = Color(0xFFE0F2F1)

private val BdTextMain = Color(0xFF1A1C1C)
private val BdTextSub = Color(0xFF56423E)

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
    val splitMembers = listOf(
        SplitMember("1", "Bạn", "B", true),
        SplitMember("2", "Minh", "M"),
        SplitMember("3", "Thanh Hằng", "T")
    )

    val payerId = "2"

    var paidMemberIds by remember {
        mutableStateOf(setOf(payerId))
    }

    val shares = SmartSplitEngine.calculateEqualSplit(
        totalAmount = 1_200_000L,
        memberIds = splitMembers.map { it.id }
    )

    val members = splitMembers.map { member ->
        val amount = shares[member.id] ?: 0L
        val isPaid = paidMemberIds.contains(member.id)
        val isPayer = member.id == payerId

        BdSplitMember(
            name = member.name,
            role = if (isPayer) "CHỦ CHI" else "",
            initial = member.initial,
            amount = formatCurrency(amount),
            status = if (isPaid) "ĐÃ TRẢ" else "CHƯA TRẢ",
            isPaid = isPaid,
            isMe = member.isMe
        )
    }

    Scaffold(
        containerColor = BdBg,
        topBar = { BdTopBar(onBack = onBack) },
        bottomBar = {
            BdBottomAction(
                isMePaid = paidMemberIds.contains("1"),
                onMarkAsPaid = {
                    paidMemberIds = paidMemberIds + "1"
                }
            )
        }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = BdOrangeEnd)
        }

        Text(
            text = "Bill Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BdTextMain
        )

        IconButton(onClick = { /* Mở menu tùy chọn */ }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = "Thêm", tint = BdOrangeEnd)
        }
    }
}

@Composable
private fun BdReceiptHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BdSurfaceWhite),
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
                    .background(BdOrangeStart),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🍲", fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Lẩu Haidilao", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = BdTextMain)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Hôm nay, 10 Tháng 4", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BdTextSub)

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "1.200.000 đ", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = BdTextMain, letterSpacing = (-1).sp)

            Spacer(modifier = Modifier.height(24.dp))

            // Người thanh toán
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFF3F3F3), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Thanh toán bởi ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BdTextSub)
                Text(text = "Minh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BdTextMain)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Đường kẻ ngang đứt nét (Mô phỏng bằng đường nét liền mờ)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BdSplitBreakdown(members: List<BdSplitMember>) {
    Column {
        Text(
            text = "CHI TIẾT CHIA TIỀN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BdTextSub.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BdSurfaceWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                members.forEachIndexed { index, member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (member.isMe) BdOrangeLightBg else Color.Transparent)
                            .height(IntrinsicSize.Min), // Để cái vạch màu cam bằng đúng chiều cao Row
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vạch màu cam bên trái nếu là "Bạn"
                        if (member.isMe) {
                            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(BdOrangeEnd))
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
                                        .background(if (member.isMe) BdOrangeEnd else Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(member.initial, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(member.name, fontWeight = FontWeight.Bold, color = if (member.isMe) BdOrangeEnd else BdTextMain)
                                    if (member.role.isNotEmpty()) {
                                        Text(member.role, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BdOrangeEnd)
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(member.amount, fontWeight = FontWeight.Bold, color = if (member.isMe) BdOrangeEnd else BdTextMain)
                                Spacer(modifier = Modifier.height(4.dp))

                                if (member.isPaid) {
                                    if (member.role == "CHỦ CHI") {
                                        Text(member.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BdTextSub.copy(alpha = 0.6f))
                                    } else {
                                        Row(
                                            modifier = Modifier.background(BdTealBg, RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = BdTealText, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(member.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BdTealText)
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.background(Color(0xFFE2E2E2), RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(member.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BdTextSub)
                                    }
                                }
                            }
                        }
                    }
                    if (index < members.size - 1) {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BdFooterInfo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3F3)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mã giao dịch", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BdTextSub)
                Text("#HD-82931", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Phương thức", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BdTextSub)
                Text("Ví DineSplit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BdBottomAction(
    isMePaid: Boolean,
    onMarkAsPaid: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BdSurfaceWhite,
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = onMarkAsPaid,
            enabled = !isMePaid,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = BdOrangeEnd,
                disabledContainerColor = Color.LightGray
            )
        ) {
            Text(
                text = if (isMePaid) "Bạn đã thanh toán" else "Đánh dấu đã trả",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatCurrency(amount: Long): String {
    return "%,d đ".format(amount).replace(",", ".")
}