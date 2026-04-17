package com.example.dinesplit.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PrimaryButton

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

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
                    Text(
                        text = "Create account",
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Join DineSplit.",
                        style = androidx.compose.material3.MaterialTheme.typography.displayMedium
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
                        placeholder = "Create a password"
                    )

                    AppTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm Password",
                        placeholder = "Repeat your password"
                    )

                    PrimaryButton(
                        text = "Register",
                        onClick = onRegisterSuccess
                    )
                }
            }
        }
    }
}
