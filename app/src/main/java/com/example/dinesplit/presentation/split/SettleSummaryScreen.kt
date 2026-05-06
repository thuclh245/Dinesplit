package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- KHO KHAI BÁO MÀU SẮC (Cách ly 100% bằng tiền tố Ss_) ---
private val Ss_Bg = Color(0xFFF9F9F9)
private val Ss_SurfaceWhite = Color(0xFFFFFFFF)

private val Ss_OrangeStart = Color(0xFFE2725B)
private val Ss_OrangeEnd = Color(0xFF9F402D)
private val Ss_OrangeLightBtn = Color(0xFFFFDAD3) // Nền nút chia sẻ

private val Ss_TealText = Color(0xFF006B5B)
private val Ss_TealBg = Color(0xFFE0F2F1)

private val Ss_TextMain = Color(0xFF1A1C1C)
private val Ss_TextSub = Color(0xFF56423E)
private val Ss_BorderLight = Color(0xFFE2E2E2)

// --- MÔ HÌNH DỮ LIỆU TẠM ---
private data class Ss_DebtMapping(
    val fromName: String,
    val fromInitial: String,
    val fromColor: Color,
    val toName: String,
    val toInitial: String,
    val toColor: Color,
    val amount: String,
    val isPayAction: Boolean // true: Thanh toán (Cam), false: Nhắc nợ (Xám)
)

@Composable
fun SettleSummaryScreen(
    onBack: () -> Unit
) {
    val debts = listOf(
        Ss_DebtMapping("Bạn", "B", Ss_OrangeEnd, "Thanh Hằng", "T", Color.DarkGray, "200.000 đ", true),
        Ss_DebtMapping("Minh", "M", Color.Gray, "Bạn", "B", Ss_OrangeEnd, "50.000 đ", false)
    )

    Scaffold(
        containerColor = Ss_Bg,
        topBar = { Ss_TopBar(onBack = onBack) },
        bottomBar = { Ss_BottomAction() }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Ss_HeroSection() }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            item { Ss_DebtMappingList(debts) }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item { Ss_StatsCard() }
        }
    }
}

// --- CÁC COMPONENT GIAO DIỆN ---

@Composable
private fun Ss_TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ss_OrangeEnd)
        }

        Text(
            text = "Chốt sổ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Ss_TextMain
        )

        IconButton(onClick = { /* Mở menu share */ }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Share, contentDescription = "Chia sẻ", tint = Ss_OrangeEnd)
        }
    }
}

@Composable
private fun Ss_HeroSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Icon thần kỳ
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Ss_OrangeStart.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Ss_OrangeEnd, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Đã tối ưu hóa nợ nần!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ss_TextMain,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Thuật toán đã giúp giảm bớt 3 giao dịch thừa.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Ss_TextSub,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Ss_DebtMappingList(debts: List<Ss_DebtMapping>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CHI TIẾT ĐỐI SOÁT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Ss_TextSub.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Ss_SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column {
                debts.forEachIndexed { index, debt ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Hàng Avatar và Mũi tên
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Người gửi
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(debt.fromColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(debt.fromInitial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(debt.fromName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ss_TextMain)
                            }

                            // Mũi tên và Số tiền ở giữa
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = debt.amount,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (debt.isPayAction) Ss_OrangeEnd else Ss_TealText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(modifier = Modifier.width(60.dp), color = Ss_BorderLight, thickness = 2.dp)
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = if (debt.isPayAction) Ss_OrangeEnd else Ss_TealText,
                                    modifier = Modifier.offset(y = (-12).dp).size(20.dp).background(Ss_SurfaceWhite)
                                )
                            }

                            // Người nhận
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(debt.toColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(debt.toInitial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(debt.toName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ss_TextMain)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Nút Hành động
                        if (debt.isPayAction) {
                            Button(
                                onClick = { /* Mở thanh toán */ },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(listOf(Ss_OrangeStart, Ss_OrangeEnd))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Thanh toán ngay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { /* Mở nhắc nợ */ },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ss_TextSub),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Ss_BorderLight),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("Nhắc nợ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    if (index < debts.size - 1) {
                        HorizontalDivider(color = Ss_BorderLight, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Ss_StatsCard() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tổng trả
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Ss_OrangeLightBtn.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("TỔNG BẠN TRẢ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ss_OrangeEnd)
                Spacer(modifier = Modifier.height(8.dp))
                Text("200k", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ss_OrangeEnd)
            }
        }

        // Tổng nhận
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Ss_TealBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("TỔNG NHẬN VỀ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Ss_TealText)
                Spacer(modifier = Modifier.height(8.dp))
                Text("50k", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ss_TealText)
            }
        }
    }
}

@Composable
private fun Ss_BottomAction() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Ss_Bg, Ss_Bg),
                    startY = 0f, endY = 100f
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .navigationBarsPadding()
    ) {
        Button(
            onClick = { /* Xử lý chia sẻ */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ss_OrangeLightBtn),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Ss_OrangeEnd, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Chia sẻ bảng kê nợ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ss_OrangeEnd)
        }
    }
}