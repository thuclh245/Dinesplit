package com.example.dinesplit.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.ui.theme.DineSplitTheme
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.AppIconButton
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PasswordTextField
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen(
    onGoToLogin: () -> Unit,
    onRegisterSuccess: (String) -> Unit,
    viewModel: RegisterViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            if (effect is RegisterUiEffect.NavigateToCompleteProfile) {
                onRegisterSuccess(effect.displayName)
            }
        }
    }

    RegisterContent(
        uiState = uiState,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
        onSubmit = viewModel::submit,
        onGoToLogin = onGoToLogin,
    )
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // --- Decorative Background ---
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-100).dp, y = (-100).dp)
                    .size(400.dp)
                    .blur(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 100.dp, y = 100.dp)
                    .size(250.dp)
                    .blur(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // --- Sticky Top Bar ---
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(AppDimens.textFieldMinHeight)
                        .padding(horizontal = AppDimens.screenHorizontal),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AppIconButton(onClick = onGoToLogin) {
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
                        .padding(horizontal = AppDimens.space2Xl)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppDimens.space2Xl),
            ) {
                Spacer(modifier = Modifier.height(AppDimens.spaceXl))

                // Editorial Hero
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                    Text(
                        text =
                            buildAnnotatedString {
                                append("Join the ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("Table.")
                                }
                            },
                        style =
                            MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2).sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Create your Social Ledger account to start splitting memories, not just bills.",
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Form Fields
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)) {
                    AppTextField(
                        value = uiState.displayName,
                        onValueChange = onDisplayNameChange,
                        label = "DISPLAY NAME",
                        placeholder = "Foodie Traveler",
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        isError = uiState.displayNameError != null,
                        supportingText = uiState.displayNameError,
                    )

                    AppTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = "EMAIL ADDRESS",
                        placeholder = "hello@dinesplit.com",
                        leadingIcon = { Icon(imageVector = Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        isError = uiState.emailError != null,
                        supportingText = uiState.emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )

                    PasswordTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = "PASSWORD",
                        placeholder = "••••••••",
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        isError = uiState.passwordError != null,
                        supportingText = uiState.passwordError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    )

                    PasswordTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = "CONFIRM PASSWORD",
                        placeholder = "••••••••",
                        leadingIcon = { Icon(imageVector = Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        isError = uiState.confirmPasswordError != null,
                        supportingText = uiState.confirmPasswordError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    )
                }

                // Terms Acceptance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    Checkbox(
                        checked = uiState.isTermsAccepted,
                        onCheckedChange = onTermsAcceptedChange,
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        modifier = Modifier.offset(y = -AppDimens.spaceSm),
                    )
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    val annotatedText =
                        buildAnnotatedString {
                            append("By registering, you agree to our ")
                            pushStringAnnotation(tag = "TERMS", annotation = "https://dinesplit.com/terms")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                ),
                            ) {
                                append("Terms of Service")
                            }
                            pop()
                            append(" and ")
                            pushStringAnnotation(tag = "PRIVACY", annotation = "https://dinesplit.com/privacy")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                ),
                            ) {
                                append("Privacy Policy")
                            }
                            pop()
                            append(". We handle your data with care.")
                        }
                    androidx.compose.foundation.text.ClickableText(
                        text = annotatedText,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                     uriHandler.openUri(annotation.item)
                                }
                            annotatedText.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        },
                    )
                }

                // Primary Action
                if (uiState.submitError != null) {
                    Text(
                        text = uiState.submitError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = AppDimens.spaceSm),
                    )
                }

                PrimaryButton(
                    text = "Create Account",
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSubmitting,
                    isLoading = uiState.isSubmitting,
                )

                // Social Options
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                        Text(
                            "OR CONTINUE WITH",
                            modifier = Modifier.padding(horizontal = AppDimens.spaceLg),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                    }

                    Spacer(modifier = Modifier.height(AppDimens.spaceXl))

                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)) {
                        SocialButton(
                            iconUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDpWtz23je5m8QkrgSxY1hqrDjt1DKEZ76ut_Cm8QeQRLBPHsAi9iPqcxSguk7cBDFu4WZRrH3QWg9pIgu3LdiwV-Tx2a0SXTLMK09ccp1RjZIAeylTpK6eW4YJztV_7lSJ5QuCOobycH1z6FYrl6tmau9FeGqrWtMCVmKps7wLwKNj69piYwl40TEYqfXG1YZkNvX-dAtkvcvT1jhTmEDaKaU1XG0DOwoiPWhk8zBdb_eVZ9Vcjr95baVidGx4Qmd3h8jNcIRvYn0",
                            label = "Google",
                            modifier = Modifier.weight(1f),
                        )
                        SocialButton(
                            imageVector = Icons.Default.Smartphone, // Using Smartphone as a placeholder
                            label = "Apple",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Footer
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppDimens.space2Xl)
                            .clickable { onGoToLogin() },
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}


@Composable
private fun SocialButton(
    modifier: Modifier = Modifier,
    iconUrl: String? = null,
    imageVector: ImageVector? = null,
    label: String,
) {
    Surface(
        onClick = { },
        modifier = modifier.height(AppDimens.buttonHeight),
        shape = AppShapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (iconUrl != null) {
                AsyncImage(model = iconUrl, contentDescription = null, modifier = Modifier.size(20.dp))
            } else if (imageVector != null) {
                Icon(imageVector = imageVector, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        RegisterContent(
            uiState =
                RegisterUiState(
                    displayName = "Foodie Traveler",
                ),
            onDisplayNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTermsAcceptedChange = {},
            onSubmit = {},
            onGoToLogin = {},
        )
    }
}
