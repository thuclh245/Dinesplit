@file:Suppress("DEPRECATION")

package com.example.dinesplit.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.PrimaryButton;
import com.example.dinesplit.core.ui.SecondaryButton;
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PasswordTextField
import com.example.dinesplit.ui.theme.DineSplitTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.collectLatest

@Suppress("DEPRECATION")
@Composable
fun LoginScreen(
    onGoToRegister: () -> Unit,
    onLoginSuccess: (com.example.dinesplit.domain.model.AppStartDestination) -> Unit,
    viewModel: LoginViewModel = viewModel(),
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
            if (effect is LoginUiEffect.NavigateToResolved) {
                onLoginSuccess(effect.destination)
            }
        }
    }

    LoginContent(
        uiState = uiState,
        onEmailOrUsernameChange = viewModel::onEmailOrUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onGoogleSignIn = {
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        },
        onGoToRegister = onGoToRegister,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailOrUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onGoToRegister: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // --- Background Layer ---
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))

        AsyncImage(
            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBpYLrxFBohbap3Bx8DQPARiGC474ysLoH3IgnXXOtMDH-JOWwSqZqigIOWvJGBy7p-lX-qfCPXXGTmbi4LJ7E5-bvNMUkHF-6WF6zJoDhRzigidqy4oSYBsyDPr2UkI29QUyT2J7sbgmZpxVkn3hbRGF_ibc-mwBEva8KH6FIiEamruFGPx0H1BQ4YDqg5l0Oc3hKvTSp2WEBJM6EevXhWkt0-4_KwWqQXl5AhW5H5m8lloE9pl2mLIXk86ZvXbKvR296mZuDF5hI",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.1f),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    MaterialTheme.colorScheme.surface,
                                ),
                        ),
                    ),
        )

        // --- Content Layer ---
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = AppDimens.spaceXl)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(AppDimens.space4Xl))

            // Brand Identity Section
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .shadow(
                            elevation = AppDimens.elevEditorial,
                            shape = RoundedCornerShape(AppDimens.cornerMedium),
                            spotColor = Color.Black.copy(alpha = 0.06f),
                        )
                        .clip(RoundedCornerShape(AppDimens.cornerMedium))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(AppDimens.iconLg),
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceXl))

            Text(
                text = "DineSplit",
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1.2).sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "THE SOCIAL LEDGER",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(AppDimens.space3Xl))

            // Form Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
            ) {
                AppTextField(
                    value = uiState.emailOrUsername,
                    onValueChange = onEmailOrUsernameChange,
                    label = "Email hoặc Tên người dùng",
                    placeholder = "email@example.com hoặc username",
                    isError = uiState.emailOrUsernameError != null || (uiState.submitError != null && !uiState.submitError.contains("Google")),
                    supportingText = uiState.emailOrUsernameError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                )

                PasswordTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = "Mật khẩu",
                    placeholder = "••••••••",
                    isError = uiState.passwordError != null || (uiState.submitError != null && !uiState.submitError.contains("Google")),
                    supportingText = uiState.passwordError ?: (if (uiState.submitError != null && !uiState.submitError.contains("Google")) uiState.submitError else null),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = AppDimens.spaceXs)
                        .clip(RoundedCornerShape(AppDimens.radiusSm))
                        .clickable(role = Role.Button) { /* Handle forgot password */ }
                        .padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceSm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Quên mật khẩu?",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (uiState.submitError != null && uiState.submitError.contains("Google")) {
                    Text(
                        text = uiState.submitError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = AppDimens.spaceSm),
                    )
                }

                Spacer(modifier = Modifier.height(AppDimens.spaceLg))

                PrimaryButton(
                    text = "Đăng nhập",
                    onClick = onSubmit,
                    isLoading = uiState.isSubmitting,
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp)) },
                )

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                    Text(
                        text = "Hoặc tiếp tục với",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg),
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                }

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

            Spacer(modifier = Modifier.height(AppDimens.space2Xl))

            Row(
                modifier =
                    Modifier
                        .padding(bottom = AppDimens.space2Xl, top = AppDimens.spaceLg)
                        .clickable(role = Role.Button) { onGoToRegister() },
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Chưa có tài khoản? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Tạo tài khoản",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        LoginContent(
            uiState =
                LoginUiState(
                    emailOrUsername = "jane.doe@example.com",
                ),
            onEmailOrUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onGoogleSignIn = {},
            onGoToRegister = {},
        )
    }
}
