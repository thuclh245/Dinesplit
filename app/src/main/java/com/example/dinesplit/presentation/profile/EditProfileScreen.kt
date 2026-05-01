package com.example.dinesplit.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton

@Composable
fun EditProfileScreen(
    uiState: EditProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    AppScaffold(title = "Edit Profile") {
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
                text = if (uiState.isSubmitting) "Saving..." else "Save",
                enabled = !uiState.isSubmitting,
                onClick = onSave
            )

            SecondaryButton(
                text = "Back",
                enabled = !uiState.isSubmitting,
                onClick = onBack
            )
        }
    }
}


