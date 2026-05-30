package com.example.dinesplit.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onToggleDiningStyle: (String) -> Unit,
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

            // Dining Style Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DINING STYLE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StyleChip(label = "Fine Dining", icon = Icons.Default.Restaurant, selected = uiState.selectedStyles.contains("Fine Dining"), onClick = { onToggleDiningStyle("Fine Dining") })
                    StyleChip(label = "Cafe Hopper", icon = Icons.Default.Coffee, selected = uiState.selectedStyles.contains("Cafe Hopper"), onClick = { onToggleDiningStyle("Cafe Hopper") })
                    StyleChip(label = "Nightlife", icon = Icons.Default.LocalBar, selected = uiState.selectedStyles.contains("Nightlife"), onClick = { onToggleDiningStyle("Nightlife") })
                }
            }

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

@Composable
private fun StyleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = CircleShape,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


