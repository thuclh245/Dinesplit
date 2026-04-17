package com.example.dinesplit.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton

@Composable
fun LoginScreen(
    onGoToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AppScaffold(title = "DINESPLIT") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                    androidx.compose.material3.Text(
                        text = "Welcome back",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.Text(
                        text = "Sign in.",
                        style = androidx.compose.material3.MaterialTheme.typography.displayMedium
                    )
                    androidx.compose.material3.Text(
                        text = "Track personal and group finance in one place.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    AppTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        placeholder = "example@email.com"
                    )

                    AppTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Enter your password"
                    )

                    PrimaryButton(
                        text = "Login",
                        onClick = onLoginSuccess
                    )

                    SecondaryButton(
                        text = "Create account",
                        onClick = onGoToRegister
                    )
                }
            }
        }
    }
}
