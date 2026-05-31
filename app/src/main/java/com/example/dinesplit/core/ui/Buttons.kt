package com.example.dinesplit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = AppDimens.buttonHeight)
                .background(
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer,
                                ),
                        ),
                    shape = AppShapes.full,
                ),
        shape = AppShapes.full,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = AppDimens.cardElevation,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = AppDimens.spaceLg),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    icon()
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = AppDimens.buttonHeight),
        shape = AppShapes.full,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
            ),
    ) {
        if (icon != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                icon()
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Suppress("unused")
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = AppDimens.buttonHeight),
        shape = AppShapes.full,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

// Backward compatibility aliases
@Composable
fun DineSplitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
) = PrimaryButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled, isLoading = isLoading, icon = icon)

@Composable
fun DineSplitOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
) = SecondaryButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled, icon = icon)
