package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.PrimaryButton

@Composable
fun FeedScreen(
    onOpenNotifications: () -> Unit,
    onOpenAssistant: () -> Unit
) {
    AppScaffold(
        title = "Feed",
        actions = {
            TextButton(onClick = onOpenNotifications) {
                Text("Bell")
            }
            TextButton(onClick = onOpenAssistant) {
                Text("AI")
            }
        }
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Text(
                text = "Feed.",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            PrimaryButton(
                text = "Create Post",
                onClick = { }
            )

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
                    Text("Shared dinner", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "An shared expense update from your group.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
                    Text("Trip settlement", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Travel costs were updated and split equally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            EmptyStateBlock(
                title = "No more updates",
                subtitle = "Follow more friends or create your first post."
            )
        }
    }
}
