package com.example.dinesplit.presentation.personal

import androidx.compose.runtime.Composable

@Composable
fun PersonalRoute(
    userAvatarUrl: String?,
    onOpenSearch: () -> Unit = {},
    onAddTransaction: () -> Unit = {}
) {
    // Simplified route to match redesigned PersonalScreen
    PersonalScreen(
        userAvatarUrl = userAvatarUrl,
        onOpenSearch = onOpenSearch,
        onAddTransaction = onAddTransaction
    )
}
