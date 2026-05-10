package com.example.dinesplit.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.net.Uri
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.ProfileAvatarSection
import com.example.dinesplit.core.ui.SecondaryButton

@Composable
fun EditProfileScreen(
    uiState: EditProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onAvatarChange: (Uri) -> Unit,
    onAvatarClear: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onAvatarChange)
    }

    AppScaffold(title = "Edit Profile") {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            ProfileAvatarSection(
                avatarModel = uiState.avatarUrl,
                title = "Profile avatar",
                subtitle = if (uiState.avatarError.isNullOrBlank()) {
                    "Choose a new avatar or clear the current one"
                } else {
                    uiState.avatarError
                },
                actionText = "Change avatar",
                clearText = if (uiState.avatarUrl.isNotBlank()) "Clear avatar" else null,
                onActionClick = {
                    avatarPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClearClick = onAvatarClear,
                isBusy = uiState.isSubmitting || uiState.isAvatarUploading
            )

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

            PrimaryButton(
                text = when {
                    uiState.isAvatarUploading -> "Uploading avatar..."
                    uiState.isSubmitting -> "Saving..."
                    else -> "Save"
                },
                enabled = !uiState.isSubmitting && !uiState.isAvatarUploading,
                onClick = onSave
            )

            SecondaryButton(
                text = "Back",
                enabled = !uiState.isSubmitting && !uiState.isAvatarUploading,
                onClick = onBack
            )
        }
    }
}


