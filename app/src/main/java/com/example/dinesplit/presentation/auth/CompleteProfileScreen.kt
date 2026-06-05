package com.example.dinesplit.presentation.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.ui.theme.DineSplitTheme
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.AppTextField

@Composable
fun CompleteProfileScreen(
    onBack: () -> Unit,
    onCompleteProfileSuccess: () -> Unit,
    initialDisplayName: String = "",
    viewModel: CompleteProfileViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            if (effect is CompleteProfileUiEffect.NavigateToMain) {
                onCompleteProfileSuccess()
            }
        }
    }

    LaunchedEffect(initialDisplayName) {
        if (initialDisplayName.isNotBlank() && uiState.displayName.isBlank()) {
            viewModel.onDisplayNameChange(initialDisplayName)
        }
    }

    CompleteProfileContent(
        uiState = uiState,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onUsernameChange = viewModel::onUsernameChange,
        onBioChange = viewModel::onBioChange,
        onAvatarSelected = viewModel::onAvatarSelected,
        onToggleDiningStyle = viewModel::toggleDiningStyle,
        onSubmit = viewModel::submit,
        onBack = onBack,
    )
}

@Composable
private fun CompleteProfileContent(
    uiState: CompleteProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onAvatarSelected: (Uri) -> Unit,
    onToggleDiningStyle: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let { onAvatarSelected(it) }
        }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Top App Bar ---
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = "DineSplit",
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1).sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TextButton(onClick = { /* Help */ }) {
                    Text(
                        "Help",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // --- Scrollable Form ---
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Header Section
                Text(
                    text = "Create Profile",
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Set the table for your next social meal.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Profile Picture Upload
                Box(
                    modifier =
                        Modifier
                            .size(128.dp)
                            .clickable { launcher.launch("image/*") },
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 12.dp,
                    ) {
                        AsyncImage(
                            model =
                                uiState.avatarLocalUri ?: uiState.avatarUrl.takeIf {
                                    it.isNotBlank()
                                } ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuDkuNXrcHfy46qqNVzH5WCEMMztgKNUW2-ZOvk2egw6QipHOKcqkdsMrYhwVldG3RyNp2J_ynP-LohRwu6ULWERiYrbC2g7RmLkGuWLso2o_e-Ih5dkbAdQkYMU1NCL0f3GyqwqtWC4UKRnw1-eIBLnXiHjNNKv69qf9izVTIhsz1aIAcNZavl4boZLKJ8IYMeulPgr8YSnqrrnXm0sss_MaGHOfbqT1WwmwPOWvwDP_byDQUeg20yimOvxiMksj47rY7JckyClSnY",
                            contentDescription = "Avatar",
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            alpha = if (uiState.avatarLocalUri == null && uiState.avatarUrl.isBlank()) 0.6f else 1.0f,
                        )
                        if (uiState.avatarLocalUri == null && uiState.avatarUrl.isBlank()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .shadow(8.dp, CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (uiState.avatarError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Không thể tải ảnh đại diện: ${uiState.avatarError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Form Section
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    AppTextField(
                        value = uiState.displayName,
                        onValueChange = onDisplayNameChange,
                        label = "DISPLAY NAME",
                        placeholder = "e.g. Linh Trần",
                        isError = uiState.displayNameError != null,
                        supportingText = uiState.displayNameError,
                    )

                    AppTextField(
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        label = "USERNAME",
                        placeholder = "ten_dang_nhap",
                        leadingIcon = {
                            Text(
                                "@",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        isError = uiState.usernameError != null,
                        supportingText = uiState.usernameError ?: "Unique handle for splitting bills",
                    )

                    AppTextField(
                        value = uiState.bio,
                        onValueChange = onBioChange,
                        label = "SHORT BIO",
                        placeholder = "Foodie, coffee lover, and weekend brunch enthusiast...",
                        singleLine = false,
                        maxLines = 3,
                    )

                    // Dining Style Section
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "DINING STYLE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            StyleChip(
                                label = "Fine Dining",
                                icon = Icons.Default.Restaurant,
                                selected =
                                    uiState.selectedStyles.contains(
                                        "Fine Dining",
                                    ),
                                onClick = {
                                    onToggleDiningStyle("Fine Dining")
                                },
                            )
                            StyleChip(
                                label = "Cafe Hopper",
                                icon = Icons.Default.Coffee,
                                selected =
                                    uiState.selectedStyles.contains(
                                        "Cafe Hopper",
                                    ),
                                onClick = {
                                    onToggleDiningStyle("Cafe Hopper")
                                },
                            )
                            StyleChip(
                                label = "Nightlife",
                                icon = Icons.Default.LocalBar,
                                selected =
                                    uiState.selectedStyles.contains(
                                        "Nightlife",
                                    ),
                                onClick = {
                                    onToggleDiningStyle("Nightlife")
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Actions
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    if (uiState.submitError != null) {
                        Text(
                            text = uiState.submitError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    PrimaryButton(
                        text = "Complete Profile",
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSubmitting,
                        isLoading = uiState.isSubmitting,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        },
                    )

                    val primaryColor = MaterialTheme.colorScheme.primary
                    Text(
                        text =
                            buildAnnotatedString {
                                append("By continuing, you agree to our ")
                                withStyle(
                                    SpanStyle(
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    ),
                                ) {
                                    append("Terms of Service")
                                }
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = CircleShape,
        modifier = Modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompleteProfileScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        CompleteProfileContent(
            uiState =
                CompleteProfileUiState(
                    displayName = "Linh Trần",
                    username = "linhtran",
                ),
            onDisplayNameChange = {},
            onUsernameChange = {},
            onBioChange = {},
            onAvatarSelected = {},
            onToggleDiningStyle = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
