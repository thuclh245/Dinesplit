package com.example.dinesplit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.buttonHeight)
            .shadow(
                elevation = AppDimens.elevHigh,
                shape = RoundedCornerShape(AppDimens.buttonHeight / 2),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(AppDimens.buttonHeight / 2),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        enabled = enabled && !isLoading
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    if (icon != null) {
                        Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                        icon()
                    }
                }
            }
        }
    }
}

// Alias for PrimaryButton
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    PrimaryButton(text, onClick, modifier, isLoading, enabled, icon)
}

@Composable
fun DineSplitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    PrimaryButton(text, onClick, modifier, isLoading, enabled, icon)
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.buttonHeight),
        shape = RoundedCornerShape(AppDimens.buttonHeight / 2),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
        ),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(AppDimens.spaceMd))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DineSplitOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    SecondaryButton(text, onClick, modifier, isLoading, enabled, icon)
}
