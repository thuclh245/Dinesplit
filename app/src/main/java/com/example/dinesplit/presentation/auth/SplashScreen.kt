package com.example.dinesplit.presentation.auth

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.dinesplit.domain.model.AppStartDestination
import androidx.compose.runtime.Composable
import com.example.dinesplit.core.ui.SplashContent

@Composable
fun SplashScreen(
    onDestinationResolved: (AppStartDestination) -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.destination) {
        uiState.destination?.let(onDestinationResolved)
    }

    SplashContent(progress = uiState.progress)
}
