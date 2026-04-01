package com.example.dinesplit.presentation.auth


import androidx.compose.runtime.Composable
import com.example.dinesplit.core.ui.AppPlaceholderScreen

@Composable
fun CompleteProfileScreen(
    onCompleteProfileSuccess: () -> Unit
) {
    AppPlaceholderScreen(
        title = "Complete Profile Screen",
        primaryActionLabel = "Finish Profile",
        onPrimaryAction = onCompleteProfileSuccess
    )
}