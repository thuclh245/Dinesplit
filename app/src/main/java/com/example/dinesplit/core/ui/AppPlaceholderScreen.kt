package com.example.dinesplit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun AppPlaceholderScreen(
    title: String,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    AppScaffold(title = "DineSplit") {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                    Box(
                        modifier =
                            Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(AppDimens.radiusSm),
                                )
                                .padding(horizontal = AppDimens.spaceSm, vertical = 6.dp),
                    ) {
                        Text(
                            text = "DineSplit module",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Temporary fallback shell for a module that still needs product copy or real data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (primaryActionLabel != null && onPrimaryAction != null) {
                PrimaryButton(
                    text = primaryActionLabel,
                    onClick = onPrimaryAction,
                )
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
                SecondaryButton(
                    text = secondaryActionLabel,
                    onClick = onSecondaryAction,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppPlaceholderScreenPreview() {
    DineSplitTheme {
        AppPlaceholderScreen(
            title = "DineSplit Placeholder Screen",
            primaryActionLabel = "Primary Action",
            onPrimaryAction = {},
            secondaryActionLabel = "Secondary Action",
            onSecondaryAction = {},
        )
    }
}
