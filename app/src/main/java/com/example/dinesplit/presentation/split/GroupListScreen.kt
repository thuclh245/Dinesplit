package com.example.dinesplit.presentation.split

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GroupListScreen(
    onNavigateToGroupDetail: () -> Unit,
    onNavigateToCreateGroup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Màn hình Group List")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToGroupDetail) {
            Text("Vào xem chi tiết nhóm (Group Detail)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onNavigateToCreateGroup) {
            Text("Tạo nhóm mới (+)")
        }
    }
}