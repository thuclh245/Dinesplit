package com.example.dinesplit.presentation.feed

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.DinePostImage
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.ui.theme.AppColors
import com.example.dinesplit.ui.theme.DineSplitTheme


@Composable
private fun RecentGroupVibes(
    posts: List<Post> = emptyList(),
    viewedStoryIds: Set<String> = emptySet(),
    currentUser: UserProfile? = null,
    myActivePost: Post? = null,
    onVibeClick: (Post) -> Unit = {},
    onCreatePostClick: () -> Unit = {},
) {
    Column(modifier = Modifier.padding(bottom = AppDimens.spaceLg)) {
        Text(
            text = "KHOẢNH KHẮC BẠN BÈ",
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(
                start = AppDimens.screenHorizontal,
                end = AppDimens.screenHorizontal,
                bottom = AppDimens.spaceMd
            ),
        )

        if (posts.isEmpty() && currentUser == null) {
            // Show placeholder story circles when no data
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(vibes) { vibe ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(80.dp)
                                    .background(
                                        if (vibe.hasStory) {
                                            Brush.sweepGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary,
                                                    MaterialTheme.colorScheme.primary,
                                                ),
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                                ),
                                            )
                                        },
                                        CircleShape,
                                    )
                                    .padding(4.dp),
                        ) {
                            DineAvatarImage(
                                imageUrl = vibe.avatar,
                                name = vibe.name,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                                size = 72.dp,
                            )
                        }
                        Text(
                            vibe.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Current user fixed story circle
                if (currentUser != null) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .width(100.dp)
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        if (myActivePost != null) {
                                            onVibeClick(myActivePost)
                                        } else {
                                            onCreatePostClick()
                                        }
                                    },
                        ) {
                            if (myActivePost != null && myActivePost.imageUrls.isNotEmpty()) {
                                DinePostImage(
                                    imageUrl = myActivePost.imageUrls.firstOrNull(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().alpha(0.85f),
                                    shape = AppShapes.large,
                                )
                            } else {
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize().background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary.copy(0.4f),
                                                    MaterialTheme.colorScheme.secondary.copy(0.4f),
                                                ),
                                            ),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = AppColors.surfaceWhite.copy(0.6f),
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                            }
                            // gradient overlay
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))),
                                    ),
                            )

                            // Author avatar at top
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .size(36.dp)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary,
                                                    MaterialTheme.colorScheme.primary,
                                                ),
                                            ),
                                            CircleShape,
                                        )
                                        .padding(2.dp),
                            ) {
                                DineAvatarImage(
                                    imageUrl = currentUser.avatarUrl,
                                    name = currentUser.displayName,
                                    modifier = Modifier.fillMaxSize().border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                    size = 32.dp,
                                )
                            }

                            // Active dot or add icon
                            if (myActivePost != null && !viewedStoryIds.contains(myActivePost.id)) {
                                Box(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(AppDimens.spaceSm)
                                            .size(10.dp)
                                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                            .border(2.dp, AppColors.surfaceWhite, CircleShape),
                                )
                            } else if (myActivePost == null) {
                                Box(
                                    modifier =
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(AppDimens.spaceSm)
                                            .size(16.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .border(1.dp, AppColors.surfaceWhite, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Thêm tin",
                                        tint = AppColors.surfaceWhite,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }

                            // Author name at bottom ("Tin của tôi")
                            Column(
                                modifier = Modifier.align(Alignment.BottomStart).padding(AppDimens.spaceSm),
                            ) {
                                Text(
                                    text = "Tin của tôi",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.surfaceWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (myActivePost != null) "Đang hoạt động" else "Tạo tin mới",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.surfaceWhite.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // 2. Other users' vibes/stories
                items(posts) { post ->
                    // Story card: show post image with author name
                    Box(
                        modifier =
                            Modifier
                                .width(100.dp)
                                .height(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onVibeClick(post) },
                    ) {
                        if (post.imageUrls.isNotEmpty()) {
                            DinePostImage(
                                imageUrl = post.imageUrls.firstOrNull(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().alpha(0.85f),
                                shape = AppShapes.large,
                            )
                        } else {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(0.4f),
                                                MaterialTheme.colorScheme.secondary.copy(0.4f),
                                            ),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = AppColors.surfaceWhite.copy(0.6f),
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        // gradient overlay
                        Box(
                            modifier =
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))),
                                ),
                        )
                        // Author avatar at top
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .size(36.dp)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.primary,
                                            ),
                                        ),
                                        CircleShape,
                                    )
                                    .padding(2.dp),
                        ) {
                            DineAvatarImage(
                                imageUrl = post.authorAvatar,
                                name = post.authorName,
                                modifier = Modifier.fillMaxSize().border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                size = 32.dp,
                            )
                        }
                        // Active dot
                        if (!viewedStoryIds.contains(post.id)) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(AppDimens.spaceSm)
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                        .border(2.dp, AppColors.surfaceWhite, CircleShape),
                            )
                        }
                        // Author name at bottom
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart).padding(AppDimens.spaceSm),
                        ) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.surfaceWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!post.location.isNullOrBlank()) {
                                Text(
                                    text = post.location,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.surfaceWhite.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    bottomPadding: Dp = 80.dp,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onCreatePost: () -> Unit = {},
    onOpenPostDetail: (String) -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onEditPost: (String) -> Unit = {},
    onSettleUp: (String, String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var postToDeleteId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Infinite scroll logic
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 2 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && uiState.canLoadMore) {
            viewModel.loadNextPage()
        }
    }

    if (postToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { postToDeleteId = null },
            title = { Text("Xóa bài viết", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = { Text("Bạn có chắc chắn muốn xóa bài viết này không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = postToDeleteId!!
                        postToDeleteId = null
                        viewModel.onDeletePost(id)
                    },
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDeleteId = null }) {
                    Text("Hủy")
                }
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = bottomPadding),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
                    shape = CircleShape,
                    modifier = Modifier.shadow(8.dp, CircleShape),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "${uiState.posts.size} BÀI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        )
                    }
                }

                FloatingActionButton(
                    onClick = onCreatePost,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = CircleShape,
                    modifier =
                        Modifier
                            .size(52.dp)
                            .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "New Post", modifier = Modifier.size(22.dp))
                }
            }
        },
    ) { padding ->
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp + statusBarHeight), contentAlignment = Alignment.Center) {
                        LoadingBlock(
                            message = "Đang tải bài viết...",
                            modifier = Modifier.padding(AppDimens.spaceLg),
                        )
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp + statusBarHeight), contentAlignment = Alignment.Center) {
                        ErrorStateBlock(
                            title = "Không thể tải bài viết",
                            subtitle = uiState.error.orEmpty(),
                            retryText = "Thử lại",
                            onRetryClick = { viewModel.refresh() },
                            modifier = Modifier.padding(AppDimens.spaceLg),
                        )
                    }
                }
                uiState.posts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp + statusBarHeight), contentAlignment = Alignment.Center) {
                        EmptyStateBlock(
                            title = "Chưa có bài viết nào",
                            subtitle = "Hãy là người đầu tiên chia sẻ khoảnh khắc ẩm thực!",
                            actionText = "Đăng bài ngay",
                            onActionClick = onCreatePost,
                            modifier = Modifier.padding(AppDimens.spaceLg),
                        )
                    }
                }
                else -> {
                    val context = LocalContext.current
                    LazyColumn(
                        state = listState,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(top = 64.dp + statusBarHeight, bottom = 96.dp),
                    ) {
                        item {
                            val currentUser = uiState.currentUser
                            val myActivePost =
                                if (currentUser != null) {
                                    uiState.posts.firstOrNull { post ->
                                        post.authorUid == currentUser.uid &&
                                            post.createdAt?.let { (System.currentTimeMillis() - it.time) < 24 * 60 * 60 * 1000 } == true
                                    }
                                } else {
                                    null
                                }

                            val otherVibesPosts =
                                uiState.posts
                                    .filter { post -> currentUser == null || post.authorUid != currentUser.uid }
                                    .distinctBy { it.authorUid }
                                    .sortedBy { post -> uiState.viewedStoryIds.contains(post.id) }
                                    .take(8)

                            RecentGroupVibes(
                                posts = otherVibesPosts,
                                viewedStoryIds = uiState.viewedStoryIds,
                                currentUser = currentUser,
                                myActivePost = myActivePost,
                                onVibeClick = { post ->
                                    viewModel.markStoryAsViewed(post.id)
                                    onOpenPostDetail(post.id)
                                },
                                onCreatePostClick = onCreatePost,
                            )
                        }

                        items(uiState.posts, key = { it.id }) { post ->
                            val isLikedByMe = uiState.currentUser?.uid?.let { post.likedBy.contains(it) } ?: false
                            val isOwnPost = uiState.currentUser?.uid == post.authorUid
                            val billSummary = uiState.linkedBillSummaries[post.id]
                            SocialSplitCard(
                                post = post,
                                isLikedByMe = isLikedByMe,
                                isOwnPost = isOwnPost,
                                billSummary = billSummary,
                                onLike = { viewModel.onLikePost(post.id) },
                                onUnlike = { viewModel.onUnlikePost(post.id) },
                                onComment = { onOpenPostDetail(post.id) },
                                onShare = {
                                    val shareText =
                                        buildString {
                                            append("${post.authorName} đã chia sẻ tại DineSplit!\n")
                                            if (!post.location.isNullOrBlank()) append("📍 ${post.location}\n")
                                            if (post.caption.isNotBlank()) append(post.caption)
                                        }
                                    val intent =
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                    context.startActivity(Intent.createChooser(intent, "Chia sẻ bài viết"))
                                },
                                onAuthorClick = { onOpenUserProfile(post.authorUid) },
                                onEditClick = { onEditPost(post.id) },
                                onDeleteClick = { postToDeleteId = post.id },
                                onSettleUp = {
                                    val gId = post.linkedGroupId
                                    val bId = post.linkedBillId
                                    if (gId != null && bId != null) {
                                        onSettleUp(gId, bId)
                                    }
                                },
                            )
                        }

                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(AppDimens.spaceLg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedBillSummarySection(
    summary: LinkedBillSummary,
    onViewBill: () -> Unit,
    onSettleUp: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = AppShapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = summary.billName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Surface(
                    color = if (summary.isSettled) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    },
                    shape = CircleShape,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (summary.isSettled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (summary.isSettled) "ĐÃ THANH TOÁN" else "CHƯA THANH TOÁN",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (summary.isSettled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tổng hóa đơn",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMoney(summary.totalAmount),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (summary.isIPayer) "Bạn đã trả trước" else "Phần của bạn",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMoney(summary.myShare),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (summary.isSettled || summary.isMyPaid || summary.isIPayer) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewBill,
                    modifier = Modifier.weight(1f),
                    shape = AppShapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chi tiết",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                if (!summary.isSettled && !summary.isMyPaid && !summary.isIPayer && summary.myShare > 0.0) {
                    Button(
                        onClick = onSettleUp,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Trả nợ",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

private fun formatMoney(amount: Double): String {
    val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())}đ"
}

@Composable
private fun SocialSplitCard(
    post: Post,
    isLikedByMe: Boolean,
    isOwnPost: Boolean = false,
    billSummary: LinkedBillSummary? = null,
    onLike: () -> Unit,
    onUnlike: () -> Unit,
    onComment: () -> Unit = {},
    onShare: () -> Unit = {},
    onBookmark: () -> Unit = {},
    onAuthorClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onSettleUp: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    AppCard(
        modifier =
            Modifier
                .padding(horizontal = AppDimens.screenHorizontal, vertical = AppDimens.spaceSm)
                .fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            // Header (Aligned to Top for absolute consistency)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable { onAuthorClick() },
                ) {
                    DineAvatarImage(
                        imageUrl = post.authorAvatar,
                        name = post.authorName,
                        size = 40.dp,
                    )
                    Column {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = post.location ?: "Chưa rõ địa điểm",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.offset(x = 8.dp, y = (-8).dp),
                ) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        if (isOwnPost) {
                            DropdownMenuItem(
                                text = { Text("Chỉnh sửa bài viết") },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa bài viết", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Báo cáo bài viết") },
                                onClick = {
                                    showMenu = false
                                },
                            )
                        }
                    }
                }
            }

            // Main Image
            DinePostImage(
                imageUrl = post.imageUrls.firstOrNull(),
                contentDescription = post.caption,
                modifier =
                    Modifier
                        .padding(horizontal = 8.dp)
                        .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
            )

            // Stats
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = if (isLikedByMe) onUnlike else onLike,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLikedByMe) "Unlike" else "Like",
                                tint = if (isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        if (post.likesCount > 0) {
                            Text(
                                post.likesCount.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onComment, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", modifier = Modifier.size(22.dp))
                        }
                        if (post.commentsCount > 0) {
                            Text(
                                post.commentsCount.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share", modifier = Modifier.size(22.dp))
                        }
                        if (post.sharesCount > 0) {
                            Text(
                                post.sharesCount.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
                IconButton(onClick = onBookmark, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmark", modifier = Modifier.size(24.dp))
                }
            }

            // Linked bill logic (Primary integration)
            val hasLinkedBill = !post.linkedGroupId.isNullOrBlank() && !post.linkedBillId.isNullOrBlank()
            if (hasLinkedBill) {
                if (billSummary != null) {
                    LinkedBillSummarySection(
                        summary = billSummary,
                        onViewBill = onSettleUp,
                        onSettleUp = onSettleUp
                    )
                } else {
                    Button(
                        onClick = onSettleUp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceXs),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                        shape = AppShapes.medium,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                        Text(
                            text = "Thanh toán ngay (Settle Up)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }

            // Caption
            Text(
                text =
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${post.authorName} ") }
                        append(post.caption)
                    },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
    extraParticipants: Int,
) {
    AppCard(
        modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal, vertical = AppDimens.spaceSm).fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Image Half
            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                DinePostImage(imageUrl = image, contentDescription = null, modifier = Modifier.fillMaxSize(), shape = AppShapes.large)
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))),
                        ),
                )
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(AppDimens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                ) {
                    DineAvatarImage(
                        imageUrl = userAvatar,
                        name = userName,
                        modifier = Modifier.size(24.dp).border(1.dp, AppColors.surfaceWhite, CircleShape),
                        size = 24.dp,
                    )
                    Text(userName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppColors.surfaceWhite)
                }
            }

            // Info Half
            Column(modifier = Modifier.weight(1.3f).padding(AppDimens.spaceLg).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                            Text(
                                "ĐÃ THANH TOÁN",
                                modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Text(
                            timeAgo,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp))
                    Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                    Text(
                        "\"$quote\"",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        participantsAvatars.forEach { url ->
                            DineAvatarImage(
                                imageUrl = url,
                                name = null,
                                modifier = Modifier.size(32.dp).border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                                size = 32.dp,
                            )
                        }
                        if (extraParticipants > 0) {
                            Box(
                                modifier =
                                    Modifier.size(32.dp)
                                        .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "+$extraParticipants",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.surfaceWhite,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Tổng hóa đơn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(totalBill, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialMomentCard(
    image: String,
    title: String,
    status: String,
) {
    Box(
        modifier =
            Modifier
                .padding(AppDimens.spaceLg)
                .fillMaxWidth()
                .aspectRatio(1.77f)
                .clip(AppShapes.xLarge),
    ) {
        DinePostImage(imageUrl = image, contentDescription = null, modifier = Modifier.fillMaxSize(), shape = AppShapes.xLarge)
        Box(
            modifier =
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.9f))),
                ),
        )

        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(AppDimens.spaceXl).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    status.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    color = AppColors.surfaceWhite.copy(alpha = 0.6f),
                )
                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AppColors.surfaceWhite)
            }
            Surface(
                color = AppColors.surfaceWhite.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                border = BorderStroke(1.dp, AppColors.surfaceWhite.copy(alpha = 0.2f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AppColors.surfaceWhite)
                }
            }
        }
    }
}

data class Vibe(val name: String, val avatar: String, val hasStory: Boolean)

// Fallback placeholder vibes shown before real data loads
private val vibes =
    listOf(
        Vibe(
            "Minh Tú",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuB-lcBKRoAYwQw73nIuBdULF7SZEeFfg2TOaffudPtrcOnyx_8_a249_LJcMx5TBluLjiWE8fbEcy4eV7gK7RbihQblIjmgOP5B7c55rKXy9JsKjJjQmetVj5yL0q9GvyYQPiR0_ZnmJv_VjBFl-eNXvTyxNV_wGEHIOerHgr7-Bhr0JQ52rl2IHVEA925v4ju8vhX_A3TbdL37vEXq2FZ6hzChgpASZ9lmR1dkpJprauIMSfg-jAAb0dHPuAtCHgI4cY8VOV-Agd4",
            true,
        ),
        Vibe(
            "Khánh Linh",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuBz8Jc-E35SQpfts5F-5Eczw6dWoYmC-HCNwwt8GI_w0EWLE-2FnqQ8mgZvohpGRKnOAVGodaj82NSuuH_X44mCJJF7svdrXs69vjYwM96R4FUn5f4TKPG3hUkyjfKZH4SiY7gfmVzYcX-w6uDBdpBiMt_ZPYvDEIlUp5JJt-Wworohv65EZUi3d15JqXw6myxzpL87IYhIB4EmDZooMn6Y3D8DcEbET8nOa6KpvmgNiVWOgGg3Cd0oMcbuAHlEdpFlt0R-injRfPo",
            false,
        ),
        Vibe(
            "Thế Huy",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDgF0M5FazB2IT4juh4tcOt4K1Ebn3YjSLLXXnEO_orZuRvR7754qsoNDOrLZZRk9MBdEyyJm92iSBTTnUo254hKU062XQiAI0pDu2ZzQ6qUeeRLIRs31LkLGwZlpQVMko9-vOn8jdvYQxhY1IXcHNASxdE5qHGU8nV6uM1v89Ykoyi-NsBff_wlPgG-H-Xsclt1CrCt3PDOJlWGuMnbGFCAkt3p8c5XbDj3XqELFVIf12Tnm9BMHwVXvqOCJPx3F_X1-e8Nyuo7RU",
            false,
        ),
    )

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        // Mock UI or just empty for preview
    }
}
