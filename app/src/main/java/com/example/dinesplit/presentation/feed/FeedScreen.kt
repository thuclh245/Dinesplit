package com.example.dinesplit.presentation.feed

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.domain.model.Post
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.dinesplit.ui.theme.DineSplitTheme
import com.example.dinesplit.core.ui.HomeTopBar
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.DinePostImage

@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    bottomPadding: Dp = 80.dp,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onCreatePost: () -> Unit = {},
    onSettleUp: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = bottomPadding)
            ) {
                // DINERS Badge (Floating at the bottom right, above navbar)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
                    shape = CircleShape,
                    modifier = Modifier.shadow(8.dp, CircleShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("${uiState.posts.size} BÀI", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black))
                    }
                }

                // New Post Button
                FloatingActionButton(
                    onClick = onCreatePost,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "New Post", modifier = Modifier.size(22.dp))
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    LoadingBlock(
                        message = "Đang tải bài viết...",
                        modifier = Modifier.padding(AppDimens.spaceLg)
                    )
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    ErrorStateBlock(
                        title = "Không thể tải bài viết",
                        subtitle = uiState.error.orEmpty(),
                        retryText = "Thử lại",
                        onRetryClick = { viewModel.refresh() },
                        modifier = Modifier.padding(AppDimens.spaceLg)
                    )
                }
            }
            uiState.posts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(top = 64.dp), contentAlignment = Alignment.Center) {
                    EmptyStateBlock(
                        title = "Chưa có bài viết nào",
                        subtitle = "Hãy là người đầu tiên chia sẻ khoảnh khắc ẩm thực!",
                        actionText = "Đăng bài ngày",
                        onActionClick = onCreatePost,
                        modifier = Modifier.padding(AppDimens.spaceLg)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(padding),
                    contentPadding = PaddingValues(top = 64.dp, bottom = 96.dp)
                ) {
                    item {
                        RecentGroupVibes()
                    }

                    items(uiState.posts) { post ->
                        val isLikedByMe = uiState.currentUser?.uid?.let { post.likedBy.contains(it) } ?: false
                        SocialSplitCard(
                            post = post,
                            isLikedByMe = isLikedByMe,
                            onLike = { viewModel.onLikePost(post.id) },
                            onUnlike = { viewModel.onUnlikePost(post.id) },
                            onSettleUp = {
                                val gId = post.linkedGroupId
                                val bId = post.linkedBillId
                                if (gId != null && bId != null) {
                                    onSettleUp(gId, bId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentGroupVibes() {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = "RECENT GROUP VIBES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Current Meal Status
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(176.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDK-tvPMo867EAwEwleBgzX8R9QReUBHS9zRKDNRQeVBoRqUOW5HiZEk7lP__cGXdsQk54h1RXzZsyYX6PNbnXnIUEq8qInVBjCxseD_Xni_-dzFLT3Lnb2LuZQNOYPNU8PPqmlmeN2wvNi3UF4CfefCWdXID6R0lRwkQtzJai3znOl7lStckvTq7Z1ZELG1yjE0zCZ7AKAIS0v1uedtPu-pkZtUho7J-6NTfgh_WIkjME8KvLwICokqGXbq48s2prybOjR-ASSHRM",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().alpha(0.6f),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                    
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    ) {
                        Text("YOU'RE EATING", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color.White.copy(alpha = 0.8f))
                        Text("Phở Gia Truyền", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
            
            items(vibes) { vibe ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                if (vibe.hasStory) Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))
                                else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh)),
                                CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        DineAvatarImage(
                            imageUrl = vibe.avatar,
                            name = vibe.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                            size = 72.dp
                        )
                    }
                    Text(vibe.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun SocialSplitCard(
    post: Post,
    isLikedByMe: Boolean,
    onLike: () -> Unit,
    onUnlike: () -> Unit,
    onComment: () -> Unit = {},
    onShare: () -> Unit = {},
    onBookmark: () -> Unit = {},
    onSettleUp: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DineAvatarImage(
                        imageUrl = post.authorAvatar,
                        name = post.authorName,
                        size = 40.dp
                    )
                    Column {
                        Text(post.authorName, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(
                                text = post.location ?: "Chưa rõ địa điểm",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1
                            )
                        }
                    }
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = MaterialTheme.colorScheme.outline)
                }
            }

            // Main Image
            DinePostImage(
                imageUrl = post.imageUrls.firstOrNull(),
                contentDescription = post.caption,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp)
            )

            // Stats
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = if (isLikedByMe) onUnlike else onLike,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLikedByMe) "Unlike" else "Like",
                                tint = if (isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (post.likesCount > 0) {
                            Text(post.likesCount.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onComment, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", modifier = Modifier.size(22.dp))
                        }
                        if (post.commentsCount > 0) {
                            Text(post.commentsCount.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share", modifier = Modifier.size(22.dp))
                        }
                        if (post.sharesCount > 0) {
                            Text(post.sharesCount.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
                IconButton(onClick = onBookmark, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmark", modifier = Modifier.size(24.dp))
                }
            }

            // Caption
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${post.authorName} ") }
                    append(post.caption)
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AsymmetricSplitCard(
    image: String,
    userName: String,
    userAvatar: String,
    title: String,
    quote: String,
    totalBill: String,
    timeAgo: String,
    participantsAvatars: List<String>,
    extraParticipants: Int
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Image Half
            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                AsyncImage(model = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(model = userAvatar, contentDescription = null, modifier = Modifier.size(24.dp).clip(CircleShape).border(1.dp, Color.White, CircleShape))
                    Text(userName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }

            // Info Half
            Column(modifier = Modifier.weight(1.3f).padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                            Text("SETTLED", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text(timeAgo, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("\"$quote\"", style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        participantsAvatars.forEach { url ->
                            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(32.dp).border(2.dp, Color.White, CircleShape).clip(CircleShape), contentScale = ContentScale.Crop)
                        }
                        if (extraParticipants > 0) {
                            Box(modifier = Modifier.size(32.dp).border(2.dp, Color.White, CircleShape).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                Text("+$extraParticipants", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Bill", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(totalBill, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialMomentCard(image: String, title: String, status: String) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .aspectRatio(1.77f)
            .clip(RoundedCornerShape(24.dp))
    ) {
        AsyncImage(model = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.9f)))))
        
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(status, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = Color.White.copy(alpha = 0.6f))
                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

data class Vibe(val name: String, val avatar: String, val hasStory: Boolean)
private val vibes = listOf(
    Vibe("Minh Tú", "https://lh3.googleusercontent.com/aida-public/AB6AXuB-lcBKRoAYwQw73nIuBdULF7SZEeFfg2TOaffudPtrcOnyx_8_a249_LJcMx5TBluLjiWE8fbEcy4eV7gK7RbihQblIjmgOP5B7c55rKXy9JsKjJjQmetVj5yL0q9GvyYQPiR0_ZnmJv_VjBFl-eNXvTyxNV_wGEHIOerHgr7-Bhr0JQ52rl2IHVEA925v4ju8vhX_A3TbdL37vEXq2FZ6hzChgpASZ9lmR1dkpJprauIMSfg-jAAb0dHPuAtCHgI4cY8VOV-Agd4", true),
    Vibe("Khánh Linh", "https://lh3.googleusercontent.com/aida-public/AB6AXuBz8Jc-E35SQpfts5F-5Eczw6dWoYmC-HCNwwt8GI_w0EWLE-2FnqQ8mgZvohpGRKnOAVGodaj82NSuuH_X44mCJJF7svdrXs69vjYwM96R4FUn5f4TKPG3hUkyjfKZH4SiY7gfmVzYcX-w6uDBdpBiMt_ZPYvDEIlUp5JJt-Wworohv65EZUi3d15JqXw6myxzpL87IYhIB4EmDZooMn6Y3D8DcEbET8nOa6KpvmgNiVWOgGg3Cd0oMcbuAHlEdpFlt0R-injRfPo", false),
    Vibe("Thế Huy", "https://lh3.googleusercontent.com/aida-public/AB6AXuDgF0M5FazB2IT4juh4tcOt4K1Ebn3YjSLLXXnEO_orZuRvR7754qsoNDOrLZZRk9MBdEyyJm92iSBTTnUo254hKU062XQiAI0pDu2ZzQ6qUeeRLIRs31LkLGwZlpQVMko9-vOn8jdvYQxhY1IXcHNASxdE5qHGU8nV6uM1v89Ykoyi-NsBff_wlPgG-H-Xsclt1CrCt3PDOJlWGuMnbGFCAkt3p8c5XbDj3XqELFVIf12Tnm9BMHwVXvqOCJPx3F_X1-e8Nyuo7RU", false)
)

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        // Mock UI or just empty for preview
    }
}
