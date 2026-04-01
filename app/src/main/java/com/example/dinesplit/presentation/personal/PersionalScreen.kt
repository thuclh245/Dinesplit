package com.example.dinesplit.presentation.personal


import androidx.compose.runtime.Composable
import com.example.dinesplit.core.ui.AppPlaceholderScreen

@Composable
fun PersonalScreen(
    onOpenAssistant: () -> Unit = {}
) {
    AppPlaceholderScreen(
        title = "Personal Screen",
        primaryActionLabel = "Open Assistant",
        onPrimaryAction = onOpenAssistant
    )
}