package com.example.dinesplit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    textStyle: TextStyle? = null,
    minHeight: Dp = AppDimens.textFieldMinHeight,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        prefix = prefix,
        textStyle = textStyle ?: MaterialTheme.typography.bodyLarge,
        shape = AppShapes.medium,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,
                focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorSupportingTextColor = MaterialTheme.colorScheme.error,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                errorPlaceholderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            ),
        label = if (label.isNotEmpty()) {
            { Text(text = label, style = MaterialTheme.typography.bodyMedium) }
        } else {
            null
        },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(text = placeholder)
            }
        },
        supportingText = {
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

// Backward compat alias
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DineSplitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    textStyle: TextStyle? = null,
    minHeight: Dp = AppDimens.textFieldMinHeight,
) = AppTextField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    placeholder = placeholder,
    enabled = enabled,
    readOnly = readOnly,
    singleLine = singleLine,
    maxLines = maxLines,
    isError = isError,
    supportingText = supportingText,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    visualTransformation = visualTransformation,
    trailingIcon = trailingIcon,
    leadingIcon = leadingIcon,
    prefix = prefix,
    textStyle = textStyle,
    minHeight = minHeight,
)

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        singleLine = true,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        leadingIcon = leadingIcon,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            AppIconButton(
                onClick = { passwordVisible = !passwordVisible }
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                )
            }
        }
    )
}

@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onClearClick: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    label: String = "",
    minHeight: Dp = 44.dp,
) {
    val isFocused = remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(minHeight)
            .clip(AppShapes.medium)
            .background(
                if (isFocused.value) MaterialTheme.colorScheme.surfaceContainerLowest
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .padding(horizontal = AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused.value = focusState.isFocused
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions
            )
        }

        if (value.isNotEmpty()) {
            AppIconButton(
                onClick = onClearClick,
                contentDescription = "Xóa nội dung tìm kiếm",
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
