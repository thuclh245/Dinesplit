package com.example.dinesplit.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onOpenNotifications: () -> Unit = {}
) {
    AppScaffold(title = "Profile") {
        when {
            uiState.isLoading -> LoadingBlock(message = "Loading profile...")
            uiState.profile == null -> ErrorStateBlock(
                title = "Profile unavailable",
                subtitle = uiState.errorMessage ?: "Please complete profile first.",
                onRetryClick = onEditProfile,
                retryText = "Complete profile"
            )
            else -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
            ) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        Text(text = uiState.profile.displayName)
                        Text(text = "@${uiState.profile.username}")
                        Text(text = uiState.profile.email)
                        if (uiState.profile.bio.isNotBlank()) {
                            Text(text = uiState.profile.bio)
                        }
                    }
                }

                PrimaryButton(text = "Edit Profile", onClick = onEditProfile)
                SecondaryButton(text = "Open Notifications", onClick = onOpenNotifications)
                SecondaryButton(
                    text = if (uiState.isLoggingOut) "Logging out..." else "Logout",
                    enabled = !uiState.isLoggingOut,
                    onClick = onLogout
                )
            }
        }
    }
}