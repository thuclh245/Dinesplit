package com.example.dinesplit.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CompleteProfileScreen(
    onCompleteProfileSuccess: () -> Unit,
    viewModel: CompleteProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            if (effect is CompleteProfileUiEffect.NavigateToMain) {
                onCompleteProfileSuccess()
            }
        }
    }

    AppScaffold(title = "Complete Profile") {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            AppTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = "Display name",
                isError = uiState.displayNameError != null,
                supportingText = uiState.displayNameError
            )

            AppTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label = "Username",
                placeholder = "your_handle",
                isError = uiState.usernameError != null,
                supportingText = uiState.usernameError
            )

            AppTextField(
                value = uiState.bio,
                onValueChange = viewModel::onBioChange,
                label = "Bio (optional)",
                placeholder = "A short bio",
                singleLine = false,
                supportingText = uiState.submitError
            )


            PrimaryButton(
                text = if (uiState.isSubmitting) "Saving..." else "Continue",
                enabled = !uiState.isSubmitting,
                onClick = viewModel::submit
            )
        }
    }
}