package com.example.dinesplit.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.dinesplit.core.ui.AppPlaceholderScreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    AppPlaceholderScreen(title = "DineSplit Loading...")
}
