package com.example.dinesplit.presentation.profile



import androidx.compose.runtime.Composable
import com.example.dinesplit.core.ui.AppPlaceholderScreen

@Composable
fun ProfileScreen(
    onOpenNotifications: () -> Unit = {}
) {
    AppPlaceholderScreen(
        title = "Profile Screen",
        primaryActionLabel = "Open Notifications",
        onPrimaryAction = onOpenNotifications
    )
}