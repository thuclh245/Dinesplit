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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. ĐỔI TÊN BIẾN MÀU ĐỂ TRÁNH ĐỤNG HÀNG VỚI GROUPLISTSCREEN
private val DetailBgLightGray = Color(0xFFF8F8F8)
private val DetailSurfaceWhite = Color(0xFFFFFFFF)

private val DetailGradientOrangeStart = Color(0xFFE2725B)
private val DetailGradientOrangeEnd = Color(0xFF9F402D)
private val DetailTextOrangeDark = Color(0xFF9F402D)
private val DetailTextOrangeLight = Color(0xFFC2410C)

private val DetailErrorRed = Color(0xFFBA1A1A)
private val DetailErrorRedBg = Color(0xFFFFDAD6)
private val DetailMintTeal = Color(0xFF006B5B)

private val DetailIconBgOrange = Color(0xFFFFF7ED)
private val DetailIconBgTeal = Color(0xFFF0FDFA)

private val DetailTextMain = Color(0xFF1A1C1C)
private val DetailTextSub = Color(0xFF56423E)

// 2. DATA CLASS ĐỂ PUBLIC TRÁNH LỖI EXPOSE
data class DetailBillItem(
    val emoji: String,
    val title: String,
    val payerName: String,
    val time: String,
    val totalAmount: String,
    val myAmountLabel: String,
    val myAmount: String,
    val isOwe: Boolean
)

@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onNavigateToCreateBill: () -> Unit,
    onNavigateToBillDetail: () -> Unit,
    onNavigateToSettleSummary: () -> Unit
) {
    val mockBills = listOf(
        DetailBillItem("🍜", "Lẩu Thái", "Minh", "Hôm nay", "600.000 đ", "Nợ:", "150.000 đ", true),
        DetailBillItem("🧋", "Trà sữa Koi", "Bạn", "Hôm qua", "200.000 đ", "Cho mượn:", "150.000 đ", false)
    )

    Scaffold(
        containerColor = DetailBgLightGray,
        topBar = { DetailTopBar(onBack = onBack) },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(listOf(DetailGradientOrangeStart, DetailGradientOrangeEnd))
                    )
                    .clickable { onNavigateToCreateBill() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm hóa đơn", tint = Color.White, modifier = Modifier.size(32.dp))
            }
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
            item { DetailSummaryCard() }

            item { DetailTabNavigation(onNavigateToSettleSummary = onNavigateToSettleSummary) }

            items(mockBills) { bill ->
                DetailBillItemCard(bill = bill, onClick = onNavigateToBillDetail)
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dùng Box làm đường kẻ ngang thay vì Divider để an toàn với mọi phiên bản Compose
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(1.dp).background(Color.LightGray.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE8E8E8).copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Kéo để xem lịch sử cũ hơn", fontSize = 12.sp, color = DetailTextSub.copy(alpha = 0.6f), fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

// 3. ĐÁNH DẤU PRIVATE CHO CÁC HÀM COMPOSABLE PHỤ
@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Cuối tuần ăn vặt",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DetailTextOrangeDark
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                for (color in listOf(Color.LightGray, Color.Gray, Color.DarkGray)) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Settings, contentDescription = "Cài đặt", tint = Color.Gray, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun DetailSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DetailSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text(
                text = "TỔNG CHI TIÊU NHÓM",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DetailTextSub,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(text = "1.500.000", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = DetailTextMain)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "đ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DetailTextOrangeLight)
            }

            Surface(
                color = DetailErrorRedBg.copy(alpha = 0.5f),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = DetailErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Bạn đang nợ: 150.000 đ", color = DetailErrorRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailTabNavigation(onNavigateToSettleSummary: () -> Unit) {
    Surface(
        color = Color(0xFFF3F3F3),
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        brush = Brush.verticalGradient(listOf(DetailGradientOrangeStart, DetailGradientOrangeEnd)),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Hóa đơn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToSettleSummary() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Số dư", color = DetailTextSub, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun DetailBillItemCard(bill: DetailBillItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DetailSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (bill.isOwe) DetailIconBgOrange else DetailIconBgTeal),
                contentAlignment = Alignment.Center
            ) {
                Text(text = bill.emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = bill.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DetailTextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Thanh toán bởi ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = DetailTextOrangeDark)) {
                            append(bill.payerName)
                        }
                        append(" • ${bill.time}")
                    },
                    fontSize = 12.sp,
                    color = DetailTextSub
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Tổng: ${bill.totalAmount}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = bill.myAmountLabel, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if(bill.isOwe) DetailErrorRed else DetailMintTeal)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = bill.myAmount, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if(bill.isOwe) DetailErrorRed else DetailMintTeal)
                }
            }
        }
    }
}