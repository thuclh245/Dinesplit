package com.example.dinesplit.presentation.auth


import androidx.compose.runtime.Composable
import com.example.dinesplit.core.ui.AppPlaceholderScreen

@Composable
fun LoginScreen(
    onGoToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    AppPlaceholderScreen(
        title = "Login Screen",
        primaryActionLabel = "Go To Register",
        onPrimaryAction = onGoToRegister,
        secondaryActionLabel = "Login Success",
        onSecondaryAction = onLoginSuccess
    )
}