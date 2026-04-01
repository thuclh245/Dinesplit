package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            PrimaryButton(
                text = "Create Post",
                onClick = { }
            )

            AppCard {
                Text("Post card placeholder 1")
            }

            AppCard {
                Text("Post card placeholder 2")
            }

            EmptyStateBlock(
                title = "No new posts",
                subtitle = "Follow more friends or create your first post."
            )
        }
    }
}
