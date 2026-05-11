package com.example.dinesplit.presentation.personal

import androidx.compose.runtime.Composable

@Composable
fun PersonalRoute(
    onOpenSearch: () -> Unit = {},
    onAddTransaction: () -> Unit = {}
) {
    // Simplified route to match redesigned PersonalScreen
    PersonalScreen(
        onOpenSearch = onOpenSearch,
        onAddTransaction = onAddTransaction
    )
}
