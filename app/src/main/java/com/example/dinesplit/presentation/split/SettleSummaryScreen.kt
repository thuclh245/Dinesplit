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

// --- MÔ HÌNH DỮ LIỆU TẠM ---
private data class Ss_DebtMapping(
    val fromName: String,
    val fromInitial: String,
    val fromColor: @Composable () -> Color,
    val toName: String,
    val toInitial: String,
    val toColor: @Composable () -> Color,
    val amount: String,
    val isPayAction: Boolean // true: Thanh toán (Cam), false: Nhắc nợ (Xám)
)

@Composable
fun SettleSummaryScreen(
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val debts = listOf(
        Ss_DebtMapping("Bạn", "B", { colorScheme.primary }, "Thanh Hằng", "T", { colorScheme.onSurfaceVariant }, "200.000 đ", true),
        Ss_DebtMapping("Minh", "M", { colorScheme.outline }, "Bạn", "B", { colorScheme.primary }, "50.000 đ", false)
    )

    Scaffold(
        containerColor = colorScheme.surface,
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
            text = "Chốt sổ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )

        IconButton(onClick = { /* Mở menu share */ }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Share, contentDescription = "Chia sẻ", tint = colorScheme.primary)
        }
    }
}

@Composable
private fun Ss_HeroSection() {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Icon thần kỳ
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colorScheme.primaryContainer.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Đã tối ưu hóa nợ nần!",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Thuật toán đã giúp giảm bớt 3 giao dịch thừa.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Ss_DebtMappingList(debts: List<Ss_DebtMapping>) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CHI TIẾT ĐỐI SOÁT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
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
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(debt.fromColor()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(debt.fromInitial, color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(debt.fromName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            }

                            // Mũi tên và Số tiền ở giữa
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = debt.amount,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (debt.isPayAction) colorScheme.primary else colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(modifier = Modifier.width(60.dp), color = colorScheme.surfaceContainerHigh, thickness = 2.dp)
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = if (debt.isPayAction) colorScheme.primary else colorScheme.secondary,
                                    modifier = Modifier.offset(y = (-12).dp).size(20.dp).background(colorScheme.surfaceContainerLowest)
                                )
                            }

                            // Người nhận
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(debt.toColor()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(debt.toInitial, color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(debt.toName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
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
                                    modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(listOf(colorScheme.primaryContainer, colorScheme.primary))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Thanh toán ngay", color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { /* Mở nhắc nợ */ },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.surfaceContainerHigh),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("Nhắc nợ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    if (index < debts.size - 1) {
                        HorizontalDivider(color = colorScheme.surfaceContainerHigh, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Ss_StatsCard() {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tổng trả
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("TỔNG BẠN TRẢ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("200k", fontSize = 24.sp, fontWeight = FontWeight.Black, color = colorScheme.primary)
            }
        }

        // Tổng nhận
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("TỔNG NHẬN VỀ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("50k", fontSize = 24.sp, fontWeight = FontWeight.Black, color = colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun Ss_BottomAction() {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, colorScheme.surface, colorScheme.surface),
                    startY = 0f, endY = 100f
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .navigationBarsPadding()
    ) {
        Button(
            onClick = { /* Xử lý chia sẻ */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.errorContainer),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Chia sẻ bảng kê nợ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
        }
    }
}
