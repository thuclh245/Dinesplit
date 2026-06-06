package com.example.dinesplit.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.ui.theme.DineSplitTheme
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.core.ui.AppIconButton
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PasswordTextField
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen(
    onGoToLogin: () -> Unit,
    onRegisterSuccess: (String) -> Unit,
    onGoogleLoginSuccess: (com.example.dinesplit.domain.model.AppStartDestination) -> Unit = {},
    viewModel: RegisterViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val googleSignInClient =
        remember(context) {
            val options =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(com.example.dinesplit.R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            GoogleSignIn.getClient(context, options)
        }
    val googleSignInLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    viewModel.onGoogleSignInError("Google không trả về mã đăng nhập. Vui lòng thử lại.")
                } else {
                    viewModel.loginWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                if (e.statusCode == 12501) {
                    viewModel.onGoogleSignInError("Đã hủy đăng nhập Google.")
                } else {
                    val errorMsg = when (e.statusCode) {
                        10 -> "Lỗi cấu hình Google Sign-In (10): Vui lòng đăng ký vân tay SHA-1 trong Firebase Console."
                        7 -> "Lỗi kết nối mạng (7). Vui lòng kiểm tra internet."
                        else -> "Đăng nhập Google thất bại (Mã lỗi: ${e.statusCode}). Vui lòng thử lại."
                    }
                    viewModel.onGoogleSignInError(errorMsg)
                }
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is RegisterUiEffect.NavigateToCompleteProfile -> {
                    onRegisterSuccess(effect.displayName)
                }
                is RegisterUiEffect.NavigateToResolved -> {
                    onGoogleLoginSuccess(effect.destination)
                }
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
        onGoogleSignIn = {
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        },
        onGoToLogin = onGoToLogin,
    )
}

@Suppress("DEPRECATION")
@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit,
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
                AppIconButton(
                    onClick = onGoToLogin,
                    contentDescription = "Quay lại"
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
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding()
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
                                append("Tham gia ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("bàn ăn.")
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
                        text = "Tạo tài khoản DineSplit để chia sẻ kỷ niệm, không chỉ chia hóa đơn.",
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
                        label = "Tên hiển thị",
                        placeholder = "Tên hiển thị của bạn",
                        leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        isError = uiState.displayNameError != null,
                        supportingText = uiState.displayNameError,
                    )

                    AppTextField(
                        value = uiState.email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        placeholder = "email@example.com",
                        leadingIcon = { Icon(imageVector = Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        isError = uiState.emailError != null,
                        supportingText = uiState.emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )

                    PasswordTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = "Mật khẩu",
                        placeholder = "••••••••",
                        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        isError = uiState.passwordError != null,
                        supportingText = uiState.passwordError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    )

                    PasswordTextField(
                        value = uiState.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = "Xác nhận mật khẩu",
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
                        modifier = Modifier.offset(y = (-6).dp), // Offset optimized to align with first line of translated terms text
                    )
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    val annotatedText =
                        buildAnnotatedString {
                            append("Bằng cách đăng ký, bạn đồng ý với ")
                            pushStringAnnotation(tag = "TERMS", annotation = "https://dinesplit.com/terms")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                ),
                            ) {
                                append("Điều khoản dịch vụ")
                            }
                            pop()
                            append(" và ")
                            pushStringAnnotation(tag = "PRIVACY", annotation = "https://dinesplit.com/privacy")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                ),
                            ) {
                                append("Chính sách quyền riêng tư")
                            }
                            pop()
                            append(". Chúng tôi bảo vệ dữ liệu của bạn.")
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
                    text = "Tạo tài khoản",
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
                            "Hoặc tiếp tục với",
                            modifier = Modifier.padding(horizontal = AppDimens.spaceLg),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                    }

                    Spacer(modifier = Modifier.height(AppDimens.spaceXl))

                    SecondaryButton(
                        text = "Google",
                        onClick = onGoogleSignIn,
                        icon = {
                            AsyncImage(
                                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuD3-5nU9KPj_Hs_UC9WFY9-eI6ZoanHilU8-FP0y2Z0yUjs__2H_sCJtrhbFEjh8z935q1mRmNyWkOKmTF31Qnr7UMVgXDUFaaY1i_Ll7DIKYx66AVwk18lQtplYDytARQ4c9gU4lxTrOhSIM5U48S4u_tcqAj821pr1082nimz0kbaFPFdlsPcphSKqv8EXbeYZdO1J8vArnX_cJH7xINCj9b9W0BjV3JXowL_BBf4NQDyzZ483yfi3nG6z8L2cq6BuUGiOEyYR1U",
                                contentDescription = "Logo Google",
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )
                }

                // Footer
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppDimens.space2Xl)
                            .clickable(role = Role.Button) { onGoToLogin() },
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Đã có tài khoản? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Đăng nhập",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
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
            onGoogleSignIn = {},
            onGoToLogin = {},
        )
    }
}
