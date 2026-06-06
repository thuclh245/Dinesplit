package com.example.dinesplit.presentation.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.ui.theme.DineSplitTheme
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // --- Top App Bar ---
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Quay lại" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
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
                TextButton(
                    onClick = { /* Help */ },
                    modifier = Modifier.semantics { contentDescription = "Trợ giúp" }
                ) {
                    Text(
                        "Trợ giúp",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // --- Scrollable Form ---
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Header Section
                Text(
                    text = "Hoàn thiện hồ sơ",
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Thiết lập hồ sơ để bắt đầu chia sẻ bữa ăn cùng bạn bè.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Profile Picture Upload
                Box(
                    modifier =
                        Modifier
                            .size(128.dp)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Chọn ảnh đại diện"
                            ) { launcher.launch("image/*") },
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 12.dp,
                    ) {
                        if (uiState.avatarLocalUri != null || uiState.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = uiState.avatarLocalUri ?: uiState.avatarUrl,
                                contentDescription = "Ảnh đại diện",
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Thêm ảnh đại diện",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp),
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
                                .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .semantics { contentDescription = "Đổi ảnh đại diện" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Đổi ảnh đại diện",
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
                        label = "Tên hiển thị",
                        placeholder = "Ví dụ: Linh Trần",
                        isError = uiState.displayNameError != null,
                        supportingText = uiState.displayNameError,
                    )

                    AppTextField(
                        value = uiState.username,
                        onValueChange = onUsernameChange,
                        label = "Tên người dùng",
                        placeholder = "ten_dang_nhap",
                        prefix = {
                            Text(
                                "@",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        isError = uiState.usernameError != null,
                        supportingText = uiState.usernameError ?: "Tên định danh dùng khi chia hóa đơn",
                    )

                    AppTextField(
                        value = uiState.bio,
                        onValueChange = onBioChange,
                        label = "Giới thiệu ngắn",
                        placeholder = "Người thích ăn uống, yêu cà phê...",
                        singleLine = false,
                        maxLines = 3,
                    )

                    // Dining Style Section
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Phong cách ăn uống",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StyleChip(
                                label = "Nhà hàng",
                                icon = Icons.Default.Restaurant,
                                selected =
                                    uiState.selectedStyles.contains(
                                        "Fine Dining",
                                    ),
                                onClick = {
                                    onToggleDiningStyle("Fine Dining")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StyleChip(
                                label = "Cà phê",
                                icon = Icons.Default.Coffee,
                                selected =
                                    uiState.selectedStyles.contains(
                                        "Cafe Hopper",
                                    ),
                                onClick = {
                                    onToggleDiningStyle("Cafe Hopper")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StyleChip(
                                label = "Ăn đêm",
                                icon = Icons.Default.LocalBar,
                                selected =
                                    uiState.selectedStyles.contains(
                                        "Nightlife",
                                    ),
                                onClick = {
                                    onToggleDiningStyle("Nightlife")
                                },
                                modifier = Modifier.weight(1f)
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
                        text = "Hoàn tất hồ sơ",
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
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    val annotatedTermsText = buildAnnotatedString {
                        append("Bằng cách tiếp tục, bạn đồng ý với ")
                        pushStringAnnotation(tag = "TERMS", annotation = "https://dinesplit.com/terms")
                        withStyle(
                            SpanStyle(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            ),
                        ) {
                            append("Điều khoản dịch vụ")
                        }
                        pop()
                    }
                    androidx.compose.foundation.text.ClickableText(
                        text = annotatedTermsText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = { offset ->
                            annotatedTermsText.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        },
                        modifier = Modifier.padding(bottom = 48.dp),
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
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = CircleShape,
        modifier = modifier
            .clickable(role = Role.Checkbox) { onClick() }
            .semantics {
                this.stateDescription = if (selected) "Đã chọn" else "Chưa chọn"
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Clip,
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
