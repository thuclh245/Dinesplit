package com.example.dinesplit.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
fun RegisterScreen(
    onGoToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            if (effect is RegisterUiEffect.NavigateToCompleteProfile) {
                onRegisterSuccess()
            }
        }
    }

    AppScaffold(title = "Create Account") {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Spacer(modifier = Modifier)

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
                placeholder = "Create a password",
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError
            )

            AppTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "Confirm Password",
                placeholder = "Repeat your password",
                isError = uiState.confirmPasswordError != null,
                supportingText = uiState.confirmPasswordError ?: uiState.submitError
            )

            Spacer(modifier = Modifier)

            PrimaryButton(
                text = if (uiState.isSubmitting) "Creating account..." else "Register",
                enabled = !uiState.isSubmitting,
                onClick = viewModel::submit
            )

            SecondaryButton(
                text = "Already have an account? Login",
                enabled = !uiState.isSubmitting,
                onClick = onGoToLogin
            )
        }
    }
}
