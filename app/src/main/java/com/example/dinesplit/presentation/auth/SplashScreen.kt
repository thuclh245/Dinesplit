package com.example.dinesplit.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.SplashContent
import com.example.dinesplit.domain.model.AppStartDestination

@Composable
fun SplashScreen(
    onDestinationResolved: (AppStartDestination) -> Unit,
    viewModel: SplashViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.destination) {
        uiState.destination?.let(onDestinationResolved)
    }

    SplashContent(progress = uiState.progress)
}
