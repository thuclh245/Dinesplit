package com.example.dinesplit.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    AppScaffold(title = "Login") {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.space2Xl))

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

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = "Login",
                onClick = onLoginSuccess
            )

            SecondaryButton(
                text = "Don't have an account? Register",
                onClick = onGoToRegister
            )
        }
    }
}
