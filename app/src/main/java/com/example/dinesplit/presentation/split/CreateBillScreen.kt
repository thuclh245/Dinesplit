package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
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

// --- KHO KHAI BÁO MÀU SẮC (Cách ly 100% bằng tiền tố Cb_) ---
private val Cb_Bg = Color(0xFFF9F9F9)
private val Cb_SurfaceWhite = Color(0xFFFFFFFF)

private val Cb_OrangeStart = Color(0xFFE2725B)
private val Cb_OrangeEnd = Color(0xFF9F402D)
private val Cb_OrangeLightBg = Color(0xFFFFF0ED)
private val Cb_OrangeIconBg = Color(0xFFFFDAD3)

private val Cb_TextMain = Color(0xFF1A1C1C)
private val Cb_TextSub = Color(0xFF56423E)

// --- MÔ HÌNH DỮ LIỆU TẠM ---
private data class Cb_SplitMember(
    val name: String,
    val initial: String,
    val amount: String,
    val isMe: Boolean
)

@Composable
fun CreateBillScreen(
    onBack: () -> Unit
) {
    var billName by remember { mutableStateOf("") }

    val members = listOf(
        Cb_SplitMember("Bạn", "B", "400.000 đ", true),
        Cb_SplitMember("Minh", "M", "400.000 đ", false),
        Cb_SplitMember("Sarah Chen", "S", "400.000 đ", false)
    )

    Scaffold(
        containerColor = Cb_Bg,
        topBar = { Cb_TopBar(onBack = onBack) }
        // KHÔNG DÙNG bottomBar NỮA ĐỂ KHÔNG BỊ LỖI CUỘN
    ) { paddingValues ->

        // Dùng Box để xếp chồng nút Xác nhận lên trên LazyColumn
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // LỚP DƯỚI: Danh sách có thể cuộn
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { Cb_MainInfoCard(billName = billName, onNameChange = { billName = it }) }
                item { Cb_PayerSection() }
                item { Cb_SplitMethodTabs() }
                item { Cb_SplitDetailsList(members = members) }
            }

            // LỚP TRÊN: Nút bấm nổi lơ lửng ở dưới cùng
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                Cb_BottomAction()
            }
        }
    }
}

// --- CÁC COMPONENT GIAO DIỆN ---

@Composable
private fun Cb_TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Cb_OrangeEnd)
        }

        Text(
            text = "Tạo hóa đơn",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Cb_OrangeEnd
        )

        Text(
            text = "Lưu",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Cb_OrangeEnd,
            modifier = Modifier.clickable { /* Xử lý lưu nháp */ }
        )
    }
}

@Composable
private fun Cb_MainInfoCard(billName: String, onNameChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Cb_SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Cb_OrangeIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Cb_OrangeEnd, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (billName.isEmpty()) {
                        Text("Tên hóa đơn (VD: Lẩu Haidilao)", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    BasicTextField(
                        value = billName,
                        onValueChange = onNameChange,
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Cb_TextMain),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "TỔNG CỘNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Cb_TextSub.copy(alpha = 0.6f), letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "1.200.000 đ", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Cb_OrangeEnd)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFF3F3F3), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = Cb_TextSub, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Hôm nay, 10 Tháng 4", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Cb_TextSub)
            }
        }
    }
}

@Composable
private fun Cb_PayerSection() {
    Column {
        Text(
            text = "NGƯỜI THANH TOÁN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Cb_TextSub.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Cb_SurfaceWhite),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(2.dp, Cb_OrangeStart, CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("B", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Bạn", fontWeight = FontWeight.Bold, color = Cb_TextMain)
                        Text("Trả toàn bộ hóa đơn", fontSize = 12.sp, color = Cb_TextSub)
                    }
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Đổi người", tint = Cb_TextSub)
            }
        }
    }
}

@Composable
private fun Cb_SplitMethodTabs() {
    Surface(
        color = Color(0xFFF3F3F3),
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        brush = Brush.verticalGradient(listOf(Cb_OrangeStart, Cb_OrangeEnd)),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Chia đều", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("Tự nhập", color = Cb_TextSub.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("Theo món", color = Cb_TextSub.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Cb_SplitDetailsList(members: List<Cb_SplitMember>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Cb_SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            members.forEach { member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (member.isMe) Cb_OrangeLightBg else Color.Transparent)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (member.isMe) Cb_OrangeEnd else Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(member.initial, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(member.name, fontWeight = FontWeight.Bold, color = Cb_TextMain)
                            Text(
                                text = member.amount,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (member.isMe) Cb_OrangeEnd else Cb_TextSub
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Cb_OrangeEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9))
                    .clickable { /* Mở modal thêm người */ }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Cb_OrangeEnd, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thêm người tham gia", color = Cb_OrangeEnd, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun Cb_BottomAction() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Cb_Bg, Cb_Bg),
                    startY = 0f,
                    endY = 100f
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Button(
            onClick = { /* Xử lý xác nhận */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(50)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = Brush.horizontalGradient(listOf(Cb_OrangeStart, Cb_OrangeEnd))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Xác nhận hóa đơn",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}