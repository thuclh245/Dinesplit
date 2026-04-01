package com.example.dinesplit.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    AppScaffold(title = "Create Account") {
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
                placeholder = "Create a password"
            )

            AppTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                placeholder = "Repeat your password"
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = "Register",
                onClick = onRegisterSuccess
            )
        }
    }
}
