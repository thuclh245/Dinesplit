package com.example.dinesplit.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppTextField
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompleteProfileContent(
    uiState: CompleteProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // ─── Header Section (Editorial) ──────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create Profile",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set the table for your next social meal.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant
            )

            // ─── Profile Picture Upload ──────────────────────────────────────
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    // Avatar circle with gradient ring
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(colorScheme.primary, colorScheme.primaryContainer)
                                )
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surface)
                            .border(4.dp, colorScheme.surface, CircleShape)
                            .clickable { /* TODO: open image picker */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Add photo",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Edit badge
                    Box(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(36.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer)
                            .border(3.dp, colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (uiState.avatarError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.avatarError,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // ─── Form Fields ─────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(32.dp))

            // Display Name
            FieldLabel(text = "DISPLAY NAME")
            Spacer(modifier = Modifier.height(8.dp))
            AppTextField(
                value = uiState.displayName,
                onValueChange = onDisplayNameChange,
                label = "",
                placeholder = "e.g. Alex Thompson",
                isError = uiState.displayNameError != null,
                supportingText = uiState.displayNameError
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Username
            FieldLabel(text = "USERNAME")
            Spacer(modifier = Modifier.height(8.dp))
            AppTextField(
                value = uiState.username,
                onValueChange = onUsernameChange,
                label = "",
                placeholder = "@your_handle",
                isError = uiState.usernameError != null,
                supportingText = uiState.usernameError ?: "Unique handle for splitting bills"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bio
            FieldLabel(text = "SHORT BIO")
            Spacer(modifier = Modifier.height(8.dp))
            AppTextField(
                value = uiState.bio,
                onValueChange = onBioChange,
                label = "",
                placeholder = "Foodie, coffee lover, and weekend brunch enthusiast...",
                singleLine = false,
                supportingText = "Briefly describe yourself (optional)"
            )

            // ─── Dining Style Chips ──────────────────────────────────────────
            Spacer(modifier = Modifier.height(28.dp))

            FieldLabel(text = "DINING STYLE")
            Spacer(modifier = Modifier.height(12.dp))

            var selectedChips by remember { mutableStateOf(setOf("Fine Dining")) }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiningStyleChip(
                    label = "Fine Dining",
                    icon = Icons.Default.Restaurant,
                    isSelected = "Fine Dining" in selectedChips,
                    onClick = { selectedChips = selectedChips.toggle("Fine Dining") }
                )
                DiningStyleChip(
                    label = "Cafe Hopper",
                    icon = Icons.Default.LocalCafe,
                    isSelected = "Cafe Hopper" in selectedChips,
                    onClick = { selectedChips = selectedChips.toggle("Cafe Hopper") }
                )
                DiningStyleChip(
                    label = "Nightlife",
                    icon = Icons.Default.LocalBar,
                    isSelected = "Nightlife" in selectedChips,
                    onClick = { selectedChips = selectedChips.toggle("Nightlife") }
                )
            }

            // ─── Primary Action Button ───────────────────────────────────────
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = colorScheme.primary.copy(alpha = 0.2f),
                        spotColor = colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(colorScheme.primary, colorScheme.primaryContainer)
                        )
                    )
                    .clickable(enabled = !uiState.isSubmitting) { onSubmit() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (uiState.isSubmitting) "Saving..." else "Complete Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimary
                    )
                    if (!uiState.isSubmitting) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ─── Terms Footer ────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.submitError != null) {
                Text(
                    text = uiState.submitError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            Text(
                text = "By continuing, you agree to our Terms of Service",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Helper Composables ──────────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun DiningStyleChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor = if (isSelected) {
        colorScheme.secondaryContainer
    } else {
        colorScheme.surfaceContainerHighest
    }

    val contentColor = if (isSelected) {
        colorScheme.onSecondaryContainer
    } else {
        colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Bold
            ),
            color = contentColor
        )
    }
}

private fun Set<String>.toggle(item: String): Set<String> {
    return if (contains(item)) minus(item) else plus(item)
}

@Preview(showBackground = true)
@Composable
fun CompleteProfileScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        CompleteProfileContent(
            uiState = CompleteProfileUiState(
                displayName = "John Doe",
                username = "johndoe",
                bio = "I love splitting bills!"
            ),
            onDisplayNameChange = {},
            onUsernameChange = {},
            onBioChange = {},
            onSubmit = {}
        )
    }
}
