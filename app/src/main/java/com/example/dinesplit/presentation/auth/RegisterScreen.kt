package com.example.dinesplit.presentation.auth

import androidx.compose.runtime.Composable
import com.example.dinesplit.core.ui.AppPlaceholderScreen

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit
) {
    AppPlaceholderScreen(
        title = "Register Screen",
        primaryActionLabel = "Register Success",
        onPrimaryAction = onRegisterSuccess
    )
}