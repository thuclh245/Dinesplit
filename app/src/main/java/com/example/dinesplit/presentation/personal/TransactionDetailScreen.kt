package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onBack: () -> Unit
) {
    AppScaffold(
        title = "Transaction Detail",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            AppCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    Text(text = "Detail placeholder")
                    Text(text = "Transaction id: $transactionId")
                    Text(text = "Week 2 only needs navigation wiring here for now.")
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionDetailScreenPreview() {
    DineSplitTheme {
        TransactionDetailScreen(
            transactionId = "tx_1",
            onBack = {}
        )
    }
}

