package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppButton
import com.example.dinesplit.ui.theme.DineSplitTheme

private data class FeedPost(
    val id: String,
    val userName: String,
    val caption: String
)

@Composable
fun FeedScreen(
    onOpenNotifications: () -> Unit,
    onOpenAssistant: () -> Unit,
    onCreatePost: () -> Unit,
    onOpenPostDetail: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenOtherUserProfile: (String) -> Unit
) {
    val feedPosts = listOf(
        FeedPost("post_01", "Linh", "Seafood hotpot split with 4 friends. Great value and taste."),
        FeedPost("post_02", "An", "Late-night ramen with team. Worth every calorie."),
        FeedPost("post_03", "Khanh", "Family brunch combo. Split bill done in 10 seconds.")
    )

    AppScaffold(
        title = "Feed",
        actions = {
            TextButton(onClick = onOpenSearch) {
                Text("Search")
            }
            TextButton(onClick = onOpenNotifications) {
                Text("Bell")
            }
            TextButton(onClick = onOpenAssistant) {
                Text("AI")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppButton(
                text = "Create Post",
                onClick = onCreatePost,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppDimens.spaceLg)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
            ) {
                items(feedPosts) { post ->
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(AppDimens.spaceMd)
                                ) {
                                    Text(
                                        text = post.userName.take(1),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                TextButton(onClick = { onOpenOtherUserProfile(post.userName) }) {
                                    Text(post.userName)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(AppDimens.space2Xl * 5)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Food photo placeholder",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Text(
                                text = post.caption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Like")
                                    Text("Like", style = MaterialTheme.typography.labelMedium)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment")
                                    Text("Comment", style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = { onOpenPostDetail(post.id) }) {
                                    Icon(Icons.Outlined.Groups, contentDescription = "Split Status")
                                    Text("Split")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    DineSplitTheme {
        FeedScreen(
            onOpenNotifications = {},
            onOpenAssistant = {},
            onCreatePost = {},
            onOpenPostDetail = {},
            onOpenSearch = {},
            onOpenOtherUserProfile = {}
        )
    }
}
