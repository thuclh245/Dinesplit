package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.StatCard

@Composable
fun PersonalScreen(
    onOpenAssistant: () -> Unit
) {
    AppScaffold(
        title = "Personal",
        actions = {
            TextButton(onClick = onOpenAssistant) {
                Text("AI")
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            StatCard(
                title = "Income this month",
                value = "3.500.000đ"
            )

            StatCard(
                title = "Expense this month",
                value = "1.250.000đ"
            )

            PrimaryButton(
                text = "Add Transaction",
                onClick = { }
            )
        }
    }
}
