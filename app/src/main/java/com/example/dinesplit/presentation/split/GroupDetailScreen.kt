package com.example.dinesplit.presentation.split

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onNavigateToCreateBill: () -> Unit,
    onNavigateToBillDetail: () -> Unit,
    onNavigateToSettleSummary: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Màn hình Group Detail")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToCreateBill) {
            Text("Tạo hóa đơn mới (+)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onNavigateToBillDetail) {
            Text("Xem chi tiết một hóa đơn (Bill Detail)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onNavigateToSettleSummary) {
            Text("Chốt sổ nhóm (Settle Summary)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Quay lại danh sách nhóm")
        }
    }
}