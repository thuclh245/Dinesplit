package com.example.dinesplit.presentation.feed


import androidx.compose.runtime.Composable
import com.example.dinesplit.core.ui.AppPlaceholderScreen

@Composable
fun FeedScreen(
    onOpenNotifications: () -> Unit = {},
    onOpenAssistant: () -> Unit = {}
) {
    AppPlaceholderScreen(
        title = "Feed Screen",
        primaryActionLabel = "Open Notifications",
        onPrimaryAction = onOpenNotifications,
        secondaryActionLabel = "Open Assistant",
        onSecondaryAction = onOpenAssistant
    )
}