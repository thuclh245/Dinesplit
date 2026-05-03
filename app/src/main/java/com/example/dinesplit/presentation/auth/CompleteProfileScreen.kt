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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.ui.theme.DineSplitTheme
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

    CompleteProfileContent(
        uiState = uiState,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onUsernameChange = viewModel::onUsernameChange,
        onBioChange = viewModel::onBioChange,
        onSubmit = viewModel::submit
    )
}

@Composable
fun CompleteProfileContent(
    uiState: CompleteProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    AppScaffold(title = "Complete Profile") {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            AppTextField(
                value = uiState.displayName,
                onValueChange = onDisplayNameChange,
                label = "Display name",
                isError = uiState.displayNameError != null,
                supportingText = uiState.displayNameError
            )

            AppTextField(
                value = uiState.username,
                onValueChange = onUsernameChange,
                label = "Username",
                placeholder = "your_handle",
                isError = uiState.usernameError != null,
                supportingText = uiState.usernameError
            )

            AppTextField(
                value = uiState.bio,
                onValueChange = onBioChange,
                label = "Bio (optional)",
                placeholder = "A short bio",
                singleLine = false,
                supportingText = uiState.submitError
            )

            Spacer(modifier = Modifier)

            PrimaryButton(
                text = if (uiState.isSubmitting) "Saving..." else "Continue",
                enabled = !uiState.isSubmitting,
                onClick = onSubmit
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompleteProfileScreenPreview() {
    DineSplitTheme {
        CompleteProfileContent(
            uiState = CompleteProfileUiState(
                displayName = "John Doe",
                username = "johndoe",
                bio = "A short bio about me"
            ),
            onDisplayNameChange = {},
            onUsernameChange = {},
            onBioChange = {},
            onSubmit = {}
        )
    }
}
