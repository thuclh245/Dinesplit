package com.example.dinesplit.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    onGoToRegister: () -> Unit,
    onLoginResolved: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            if (effect is LoginUiEffect.NavigateToResolver) {
                onLoginResolved()
            }
        }
    }

    AppScaffold(title = "Login") {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.space2Xl))

            AppTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                placeholder = "example@email.com",
                isError = uiState.emailError != null,
                supportingText = uiState.emailError
            )

            AppTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                placeholder = "Enter your password",
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError ?: uiState.submitError
            )

            PrimaryButton(
                text = if (uiState.isSubmitting) "Logging in..." else "Login",
                enabled = !uiState.isSubmitting,
                onClick = viewModel::submit
            )

            SecondaryButton(
                enabled = !uiState.isSubmitting,
                text = "Don't have an account? Register",
                onClick = onGoToRegister
            )
        }
    }
}
