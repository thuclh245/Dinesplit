package com.example.dinesplit.presentation.split

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettleSummaryScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Màn hình Settle Summary (Smart Split)")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onBack) {
            Text("Quay lại")
        }
    }
}