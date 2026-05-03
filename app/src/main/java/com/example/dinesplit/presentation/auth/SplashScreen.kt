package com.example.dinesplit.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppPlaceholderScreen
import com.example.dinesplit.domain.model.AppStartDestination
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun SplashScreen(
    onDestinationResolved: (AppStartDestination) -> Unit,
    viewModel: SplashViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.destination) {
        uiState.destination?.let(onDestinationResolved)
    }

    SplashContent()
}

@Composable
fun SplashContent() {
    AppPlaceholderScreen(title = "DineSplit Loading...")
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    DineSplitTheme {
        SplashContent()
    }
}
