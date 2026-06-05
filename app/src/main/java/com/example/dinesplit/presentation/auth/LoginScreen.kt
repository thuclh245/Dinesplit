package com.example.dinesplit.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.DineSplitButton
import com.example.dinesplit.core.ui.DineSplitOutlinedButton
import com.example.dinesplit.core.ui.DineSplitTextField
import com.example.dinesplit.ui.theme.DineSplitTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.collectLatest

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
            if (result.resultCode != Activity.RESULT_OK) {
                return@rememberLauncherForActivityResult
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    viewModel.onGoogleSignInError("Google did not return an ID token. Please try again.")
                } else {
                    viewModel.loginWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                viewModel.onGoogleSignInError("Google sign-in failed. Please try again.")
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
        onEmailChange = viewModel::onEmailChange,
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
    onEmailChange: (String) -> Unit,
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
                DineSplitTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    label = "EMAIL ADDRESS",
                    placeholder = "jane.doe@example.com",
                    isError = uiState.emailError != null,
                    supportingText = uiState.emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )

                var passwordVisible by remember { mutableStateOf(false) }
                DineSplitTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = "PASSWORD",
                    placeholder = "••••••••",
                    isError = uiState.passwordError != null || uiState.submitError != null,
                    supportingText = uiState.passwordError ?: uiState.submitError,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                )

                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .padding(end = AppDimens.spaceXs)
                            .clickable { /* Handle forgot password */ },
                )

                Spacer(modifier = Modifier.height(AppDimens.spaceLg))

                DineSplitButton(
                    text = "Sign In",
                    onClick = onSubmit,
                    isLoading = uiState.isSubmitting,
                    icon = { Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp)) },
                )

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                    Text(
                        text = "OR CONTINUE WITH",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg),
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                }

                DineSplitOutlinedButton(
                    text = "Google",
                    onClick = onGoogleSignIn,
                    icon = {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuD3-5nU9KPj_Hs_UC9WFY9-eI6ZoanHilU8-FP0y2Z0yUjs__2H_sCJtrhbFEjh8z935q1mRmNyWkOKmTF31Qnr7UMVgXDUFaaY1i_Ll7DIKYx66AVwk18lQtplYDytARQ4c9gU4lxTrOhSIM5U48S4u_tcqAj821pr1082nimz0kbaFPFdlsPcphSKqv8EXbeYZdO1J8vArnX_cJH7xINCj9b9W0BjV3JXowL_BBf4NQDyzZ483yfi3nG6z8L2cq6BuUGiOEyYR1U",
                            contentDescription = "Google logo",
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier =
                    Modifier
                        .padding(bottom = AppDimens.space2Xl, top = AppDimens.spaceLg)
                        .clickable { onGoToRegister() },
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "New to the table? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Create Account",
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
                    email = "jane.doe@example.com",
                ),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onGoogleSignIn = {},
            onGoToRegister = {},
        )
    }
}
