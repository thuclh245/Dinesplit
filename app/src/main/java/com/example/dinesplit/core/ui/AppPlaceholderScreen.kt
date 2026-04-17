package com.example.dinesplit.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dinesplit.ui.theme.DineSplitTheme

@Composable
fun AppPlaceholderScreen(
    title: String,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
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
                        text = "Screen",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            if (primaryActionLabel != null && onPrimaryAction != null) {
                PrimaryButton(
                    text = primaryActionLabel,
                    onClick = onPrimaryAction
                )
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
                SecondaryButton(
                    text = secondaryActionLabel,
                    onClick = onSecondaryAction
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
            onSecondaryAction = {}
        )
    }
}
