package com.example.dinesplit.presentation.feed

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.SmallButton
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.DinePostImage
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.core.ui.HomeTopBar
import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.Story
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.ui.theme.AppColors
import com.example.dinesplit.ui.theme.DineSplitTheme
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private data class StoryViewerTarget(
    val authorUid: String,
    val storyId: String,
)

private data class StoryGroupUiModel(
    val authorUid: String,
    val authorName: String,
    val authorAvatar: String,
    val stories: List<Story>,
) {
    val previewStory: Story
        get() = stories.maxByOrNull { it.createdAt?.time ?: 0L } ?: stories.first()

    fun firstUnseenOrPreview(viewedStoryIds: Set<String>): Story {
        return stories.firstOrNull { it.id !in viewedStoryIds } ?: previewStory
    }
}

private data class StoryGroupCollection(
    val myGroup: StoryGroupUiModel?,
    val otherGroups: List<StoryGroupUiModel>,
)

@Composable
fun FeedRoute(
    userAvatarUrl: String?,
    bottomPadding: Dp = 80.dp,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onSettleUp: (String, String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToCreateStory: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToEditPost: (String) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    // KIẾN TRÚC SẠCH: Khởi tạo ViewModelFactory độc lập bảo vệ vòng đời hệ thống
    val viewModel: FeedViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
                    return FeedViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    FeedScreen(
        uiState = uiState,
        userAvatarUrl = userAvatarUrl,
        bottomPadding = bottomPadding,
        onOpenNotifications = onOpenNotifications,
        onOpenSearch = onOpenSearch,
        onCreatePost = onNavigateToCreatePost,
        onCreateStory = onNavigateToCreateStory,
        onOpenPostDetail = onNavigateToPostDetail,
        onOpenUserProfile = onNavigateToUserProfile,
        onEditPost = onNavigateToEditPost,
        onSettleUp = onSettleUp,
        onRefresh = { viewModel.refresh() },
        onLoadNextPage = { viewModel.loadNextPage() },
        onDeletePost = { viewModel.onDeletePost(it) },
        onLikePost = { viewModel.onLikePost(it) },
        onUnlikePost = { viewModel.onUnlikePost(it) },
        onMarkStoryAsViewed = { viewModel.markStoryAsViewed(it) },
        onLikeStory = { viewModel.onLikeStory(it) },
        onUnlikeStory = { viewModel.onUnlikeStory(it) },
        onSavePost = { viewModel.onSavePost(it) },
        onUnsavePost = { viewModel.onUnsavePost(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    uiState: FeedUiState,
    userAvatarUrl: String?,
    bottomPadding: Dp = 80.dp,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onCreatePost: () -> Unit = {},
    onCreateStory: () -> Unit = {},
    onOpenPostDetail: (String) -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onEditPost: (String) -> Unit = {},
    onSettleUp: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onDeletePost: (String) -> Unit,
    onLikePost: (String) -> Unit,
    onUnlikePost: (String) -> Unit,
    onMarkStoryAsViewed: (String) -> Unit,
    onLikeStory: (String) -> Unit,
    onUnlikeStory: (String) -> Unit,
    onSavePost: (String) -> Unit,
    onUnsavePost: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val listState = rememberLazyListState()
    var postToDeleteId by remember { mutableStateOf<String?>(null) }
    var selectedStoryTarget by remember { mutableStateOf<StoryViewerTarget?>(null) }
    var commentsPost by remember { mutableStateOf<Post?>(null) }

    // BẪY VÒNG ĐỜI: Tự động quét lại dữ liệu đám mây khi người dùng quay về từ màn tạo bài viết
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        currentOnRefresh()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ĐOÁN NHẬN CUỘN VÔ HẠN: Tối ưu hóa tính toán chỉ số để tải trang tiếp theo mượt mà
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 2 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && uiState.canLoadMore) {
            onLoadNextPage()
        }
    }

    // HỘP THOẠI XÓA BÀI VIẾT CHUẨN UX
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
                        onDeletePost(id)
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

    selectedStoryTarget?.let { target ->
        val storiesInGroup = uiState.stories
            .filter { story -> story.authorUid == target.authorUid }
            .sortedForStoryViewer()
        val story = storiesInGroup.firstOrNull { it.id == target.storyId } ?: storiesInGroup.firstOrNull()

        if (story != null) {
            val currentUserId = uiState.currentUser?.uid.orEmpty()
            val isLikedByMe = currentUserId.isNotBlank() && story.likedBy.contains(currentUserId)

            LaunchedEffect(story.id) {
                onMarkStoryAsViewed(story.id)
            }

            StoryViewerDialog(
                stories = storiesInGroup,
                currentStoryId = story.id,
                isLikedByMe = isLikedByMe,
                canLike = currentUserId.isNotBlank(),
                onStoryChange = { storyId ->
                    selectedStoryTarget = StoryViewerTarget(target.authorUid, storyId)
                },
                onDismiss = { selectedStoryTarget = null },
                onOpenAuthor = {
                    selectedStoryTarget = null
                    onOpenUserProfile(story.authorUid)
                },
                onToggleLike = {
                    if (isLikedByMe) {
                        onUnlikeStory(story.id)
                    } else {
                        onLikeStory(story.id)
                    }
                },
            )
        } else {
            LaunchedEffect(target) {
                selectedStoryTarget = null
            }
        }
    }

    commentsPost?.let { post ->
        CommentsBottomSheet(
            post = post,
            onDismiss = { commentsPost = null },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // CỐ ĐỊNH NÚT NỔI TOÀN CỤC: Giúp nút bấm hiển thị mượt mà trên mọi trạng thái nội dung
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                modifier = Modifier.padding(bottom = bottomPadding),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
                    shape = CircleShape,
                    modifier = Modifier.shadow(AppDimens.level3, CircleShape),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("${uiState.posts.size} BÀI", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                    }
                }

                ExtendedFloatingActionButton(
                    onClick = onCreatePost,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = CircleShape,
                    icon = {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    text = {
                        Text("Đăng bài", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    },
                    modifier = Modifier
                        .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                )
            }
        },
    ) { padding ->
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        
        // CƠ CHẾ KÉO ĐỂ LÀM MỚI (PULL TO REFRESH) CAO CẤP CỦA MATERIAL 3
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // CHỐT CHẶN SNAPSHOT STATE: Tránh xung đột render dữ liệu trên Slot Table
            val state = uiState
            
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp + statusBarHeight), contentAlignment = Alignment.Center) {
                        LoadingBlock(modifier = Modifier.padding(AppDimens.spaceLg), message = "Đang tải bài viết...")
                    }
                }
                state.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp + statusBarHeight), contentAlignment = Alignment.Center) {
                        ErrorStateBlock(
                            title = "Không thể tải bài viết",
                            subtitle = state.errorMessage,
                            retryText = "Thử lại",
                            onRetryClick = onRefresh,
                            modifier = Modifier.padding(AppDimens.spaceLg),
                        )
                    }
                }
                state.posts.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 64.dp + statusBarHeight),
                    ) {
                        state.currentUser?.let { currentUser ->
                            val storyGroups = state.stories.toStoryGroupCollection(
                                currentUserId = currentUser.uid,
                                viewedStoryIds = state.viewedStoryIds,
                            )
                            RecentGroupVibes(
                                storyGroups = storyGroups.otherGroups,
                                viewedStoryIds = state.viewedStoryIds,
                                currentUser = currentUser,
                                myStoryGroup = storyGroups.myGroup,
                                onVibeClick = { group, story ->
                                    selectedStoryTarget = StoryViewerTarget(group.authorUid, story.id)
                                },
                                onCreatePostClick = onCreateStory,
                            )
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyStateBlock(
                                title = "Chưa có bài viết nào",
                                subtitle = "Hãy là người đầu tiên chia sẻ khoảnh khắc ẩm thực!",
                                actionText = "Đăng bài ngay",
                                onActionClick = onCreatePost,
                                modifier = Modifier.padding(AppDimens.spaceLg),
                            )
                        }
                    }
                }
                else -> {
                    val context = LocalContext.current
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(top = 64.dp + statusBarHeight, bottom = bottomPadding + 96.dp),
                    ) {
                        // KHOẢNH KHẮC BẠN BÈ (STORY COMPONENT) INTEGRATION
                        if (state.stories.isNotEmpty() || state.currentUser != null) {
                            item {
                                val currentUser = state.currentUser
                                val storyGroups = state.stories.toStoryGroupCollection(
                                    currentUserId = currentUser?.uid,
                                    viewedStoryIds = state.viewedStoryIds,
                                )

                                RecentGroupVibes(
                                    storyGroups = storyGroups.otherGroups,
                                    viewedStoryIds = state.viewedStoryIds,
                                    currentUser = currentUser,
                                    myStoryGroup = storyGroups.myGroup,
                                    onVibeClick = { group, story ->
                                        selectedStoryTarget = StoryViewerTarget(group.authorUid, story.id)
                                    },
                                    onCreatePostClick = onCreateStory,
                                )
                            }
                        }

                        // DANH SÁCH BÀI ĐĂNG CHUẨN KEYED ITEMS ĐẠT HIỆU NĂNG TỐI ĐA
                        items(state.posts, key = { it.id }) { post ->
                            val isLikedByMe = state.currentUser?.uid?.let { post.likedBy.contains(it) } ?: false
                            val isSavedByMe = state.currentUser?.savedPostIds?.contains(post.id) == true
                            val isOwnPost = state.currentUser?.uid == post.authorUid
                            val billSummary = state.linkedBillSummaries[post.id]
                            
                            SocialSplitCard(
                                post = post,
                                isLikedByMe = isLikedByMe,
                                isSavedByMe = isSavedByMe,
                                isOwnPost = isOwnPost,
                                billSummary = billSummary,
                                onLike = { onLikePost(post.id) },
                                onUnlike = { onUnlikePost(post.id) },
                                onComment = { commentsPost = post },
                                onShare = {
                                    val shareText = buildString {
                                        append("${post.authorName} đã chia sẻ tại DineSplit!\n")
                                        if (!post.location.isNullOrBlank()) append("📍 ${post.location}\n")
                                        if (post.caption.isNotBlank()) append(post.caption)
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Chia sẻ bài viết"))
                                },
                                onBookmark = {
                                    if (isSavedByMe) {
                                        onUnsavePost(post.id)
                                    } else {
                                        onSavePost(post.id)
                                    }
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

                        // LOADING INDICATOR KHI KÉO PHÂN TRANG VÔ HẠN
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(AppDimens.spaceLg),
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
private fun RecentGroupVibes(
    storyGroups: List<StoryGroupUiModel> = emptyList(),
    viewedStoryIds: Set<String> = emptySet(),
    currentUser: UserProfile? = null,
    myStoryGroup: StoryGroupUiModel? = null,
    onVibeClick: (StoryGroupUiModel, Story) -> Unit = { _, _ -> },
    onCreatePostClick: () -> Unit = {},
) {
    Column(modifier = Modifier.padding(bottom = AppDimens.spaceLg)) {
        Text(
            text = "KHOẢNH KHẮC BẠN BÈ",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(
                start = AppDimens.screenHorizontal,
                end = AppDimens.screenHorizontal,
                bottom = AppDimens.spaceMd
            ),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = AppDimens.spaceXl),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            if (currentUser != null) {
                item {
                    val previewStory = myStoryGroup?.previewStory
                    StoryPreviewCard(
                        imageUrl = previewStory?.imageUrl,
                        avatarUrl = currentUser.avatarUrl,
                        authorName = "Tin của tôi",
                        subtitle = myStoryGroup?.let { "${it.stories.size} tin đang hoạt động" } ?: "Tạo tin mới",
                        isUnseen = myStoryGroup?.stories?.any { it.id !in viewedStoryIds } == true,
                        showAddBadge = myStoryGroup == null,
                        onClick = {
                            val group = myStoryGroup
                            if (group != null) {
                                onVibeClick(group, group.firstUnseenOrPreview(viewedStoryIds))
                            } else {
                                onCreatePostClick()
                            }
                        },
                    )
                }

                if (myStoryGroup != null) {
                    item {
                        StoryPreviewCard(
                            imageUrl = null,
                            avatarUrl = currentUser.avatarUrl,
                            authorName = "Tin mới",
                            subtitle = "Đăng tin 24h",
                            isUnseen = false,
                            showAddBadge = true,
                            onClick = onCreatePostClick,
                        )
                    }
                }
            }

            items(storyGroups, key = { it.authorUid }) { group ->
                val story = group.firstUnseenOrPreview(viewedStoryIds)
                StoryPreviewCard(
                    imageUrl = group.previewStory.imageUrl,
                    avatarUrl = group.authorAvatar,
                    authorName = group.authorName,
                    subtitle = group.previewStory.location.orEmpty().ifBlank { "${group.stories.size} tin" },
                    isUnseen = group.stories.any { it.id !in viewedStoryIds },
                    onClick = { onVibeClick(group, story) },
                )
            }
        }
    }
}

private fun List<Story>.toStoryGroupCollection(
    currentUserId: String?,
    viewedStoryIds: Set<String>,
): StoryGroupCollection {
    val groups = filter { it.authorUid.isNotBlank() }
        .groupBy { it.authorUid }
        .values
        .mapNotNull { it.toStoryGroupUiModel() }

    val myGroup = currentUserId?.let { uid -> groups.firstOrNull { it.authorUid == uid } }
    val otherGroups = groups
        .filterNot { it.authorUid == currentUserId }
        .sortedWith(
            compareBy<StoryGroupUiModel> { group ->
                group.stories.all { it.id in viewedStoryIds }
            }.thenByDescending { group ->
                group.previewStory.createdAt?.time ?: 0L
            },
        )

    return StoryGroupCollection(myGroup = myGroup, otherGroups = otherGroups)
}

private fun List<Story>.toStoryGroupUiModel(): StoryGroupUiModel? {
    val sortedStories = sortedForStoryViewer()
    val previewStory = sortedStories.maxByOrNull { it.createdAt?.time ?: 0L } ?: return null
    return StoryGroupUiModel(
        authorUid = previewStory.authorUid,
        authorName = previewStory.authorName,
        authorAvatar = previewStory.authorAvatar,
        stories = sortedStories,
    )
}

private fun List<Story>.sortedForStoryViewer(): List<Story> {
    return sortedBy { it.createdAt?.time ?: 0L }
}

@Composable
private fun StoryPreviewCard(
    imageUrl: String?,
    avatarUrl: String?,
    authorName: String,
    subtitle: String,
    isUnseen: Boolean,
    showAddBadge: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(150.dp)
            .clip(AppShapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            DinePostImage(
                imageUrl = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.85f),
                shape = AppShapes.large,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(
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
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = AppColors.surfaceWhite.copy(0.6f),
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))),
            ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(AppDimens.spaceSm)
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
                imageUrl = avatarUrl,
                name = authorName,
                modifier = Modifier.fillMaxSize().border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                size = 32.dp,
            )
        }
        if (isUnseen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppDimens.spaceSm)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .border(2.dp, AppColors.surfaceWhite, CircleShape),
            )
        } else if (showAddBadge) {
            Box(
                modifier = Modifier
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
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(AppDimens.spaceSm),
        ) {
            Text(
                text = authorName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AppColors.surfaceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.surfaceWhite.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StorySegmentTabs(
    storyCount: Int,
    currentIndex: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(storyCount.coerceAtLeast(1)) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(
                        AppColors.surfaceWhite.copy(
                            alpha = if (index <= currentIndex) 0.92f else 0.34f,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun StoryViewerDialog(
    stories: List<Story>,
    currentStoryId: String,
    isLikedByMe: Boolean,
    canLike: Boolean,
    onStoryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenAuthor: () -> Unit,
    onToggleLike: () -> Unit,
) {
    val currentIndex = stories.indexOfFirst { it.id == currentStoryId }.coerceAtLeast(0)
    val story = stories.getOrNull(currentIndex) ?: return
    val canMovePrevious = currentIndex > 0
    val canMoveNext = currentIndex < stories.lastIndex

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (story.imageUrl.isNotBlank()) {
                DinePostImage(
                    imageUrl = story.imageUrl,
                    contentDescription = story.caption,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                    Color(0xFF111111),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = AppColors.surfaceWhite.copy(alpha = 0.34f),
                        modifier = Modifier.size(88.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.62f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f),
                            ),
                        ),
                    ),
            )

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = canMovePrevious,
                        ) {
                            onStoryChange(stories[currentIndex - 1].id)
                        },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = canMoveNext,
                        ) {
                            onStoryChange(stories[currentIndex + 1].id)
                        },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppDimens.screenHorizontal, vertical = AppDimens.spaceMd),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                StorySegmentTabs(storyCount = stories.size, currentIndex = currentIndex)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppShapes.medium)
                            .clickable(onClick = onOpenAuthor)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                    ) {
                        DineAvatarImage(
                            imageUrl = story.authorAvatar,
                            name = story.authorName,
                            size = 42.dp,
                            modifier = Modifier.border(2.dp, AppColors.surfaceWhite.copy(alpha = 0.9f), CircleShape),
                        )
                        Column {
                            Text(
                                text = story.authorName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.surfaceWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = formatStoryTime(story.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.surfaceWhite.copy(alpha = 0.78f),
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.26f), CircleShape),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = AppColors.surfaceWhite,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = AppDimens.screenHorizontal)
                    .padding(top = AppDimens.spaceLg, bottom = AppDimens.space3Xl)
                    .background(Color.Black.copy(alpha = 0.52f), RoundedCornerShape(24.dp))
                    .padding(AppDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                if (!story.location.isNullOrBlank()) {
                    Surface(
                        color = AppColors.surfaceWhite.copy(alpha = 0.16f),
                        contentColor = AppColors.surfaceWhite,
                        shape = CircleShape,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = story.location,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (story.caption.isNotBlank()) {
                    Text(
                        text = story.caption,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.surfaceWhite,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                    ) {
                        IconButton(
                            onClick = onToggleLike,
                            enabled = canLike,
                            modifier = Modifier
                                .size(46.dp)
                                .background(AppColors.surfaceWhite.copy(alpha = 0.16f), CircleShape),
                        ) {
                            Icon(
                                imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLikedByMe) "Bỏ thả tim tin" else "Thả tim tin",
                                tint = if (isLikedByMe) MaterialTheme.colorScheme.error else AppColors.surfaceWhite,
                            )
                        }
                        Text(
                            text = story.likesCount.toString(),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.surfaceWhite,
                        )
                    }

                    Text(
                        text = "Khoảnh khắc 24h",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.surfaceWhite.copy(alpha = 0.72f),
                    )
                }

                /*
                Text(
                    text = "Khoảnh khắc 24h",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.surfaceWhite.copy(alpha = 0.72f),
                )
                */
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsBottomSheet(
    post: Post,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val repository = remember { AppContainer.feedRepository() }
    val observeSessionUseCase = remember { AppContainer.observeSessionUseCase(application) }
    val getCurrentUserProfileUseCase = remember { AppContainer.getCurrentUserProfileUseCase(application) }
    var input by remember(post.id) { mutableStateOf("") }
    var isSubmitting by remember(post.id) { mutableStateOf(false) }
    var errorMessage by remember(post.id) { mutableStateOf<String?>(null) }
    val comments by remember(post.id) {
        repository.getComments(post.id)
            .catch { throwable ->
                errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                emit(emptyList())
            }
    }.collectAsState(initial = emptyList())
    val session by observeSessionUseCase().collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(
                    text = "Bình luận",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp)
                        .padding(AppDimens.spaceLg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Chưa có bình luận nào",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(horizontal = AppDimens.screenHorizontal, vertical = AppDimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                            verticalAlignment = Alignment.Top,
                        ) {
                            DineAvatarImage(
                                imageUrl = comment.authorAvatar,
                                name = comment.authorName,
                                size = 34.dp,
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = AppShapes.large,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceSm),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = comment.authorName,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        )
                                        Text(
                                            text = comment.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                Text(
                                    text = formatStoryTime(comment.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal, vertical = AppDimens.spaceXs),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.screenHorizontal, vertical = AppDimens.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Viết bình luận...") },
                    minLines = 1,
                    maxLines = 4,
                    shape = AppShapes.large,
                )
                IconButton(
                    onClick = {
                        val trimmed = input.trim()
                        if (trimmed.isBlank() || isSubmitting) return@IconButton
                        val currentSession = session
                        if (currentSession == null) {
                            errorMessage = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."
                            return@IconButton
                        }
                        scope.launch {
                            isSubmitting = true
                            errorMessage = null
                            runCatching {
                                val profile = getCurrentUserProfileUseCase(currentSession.uid)
                                repository.addComment(
                                    post.id,
                                    com.example.dinesplit.domain.model.Comment(
                                        authorUid = currentSession.uid,
                                        authorName = profile?.displayName?.takeIf { it.isNotBlank() }
                                            ?: currentSession.email.substringBefore('@'),
                                        authorAvatar = profile?.avatarUrl.orEmpty(),
                                        content = trimmed,
                                        createdAt = Date(),
                                    ),
                                )
                            }.onSuccess {
                                input = ""
                            }.onFailure { throwable ->
                                errorMessage = "Không thể gửi bình luận: ${throwable.localizedMessage ?: "Vui lòng thử lại"}"
                            }
                            isSubmitting = false
                        }
                    },
                    enabled = input.isNotBlank() && !isSubmitting,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (input.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            CircleShape,
                        ),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gửi",
                            tint = if (input.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}

private fun formatStoryTime(createdAt: Date?): String {
    if (createdAt == null) return "Vừa xong"
    val elapsedMinutes = ((System.currentTimeMillis() - createdAt.time) / 60_000).coerceAtLeast(0)
    return when {
        elapsedMinutes < 1 -> "Vừa xong"
        elapsedMinutes < 60 -> "${elapsedMinutes} phút"
        elapsedMinutes < 24 * 60 -> "${elapsedMinutes / 60} giờ"
        else -> "24 giờ"
    }
}

@Composable
private fun LinkedBillSummarySection(
    summary: LinkedBillSummary,
    onViewBill: () -> Unit,
    onSettleUp: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        shape = AppShapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), AppShapes.large)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AppDimens.spaceMd)
                            )
                            Text(
                                text = "ĐÃ GẮN HÓA ĐƠN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = summary.billName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
                
                if (summary.isParticipant && summary.isAuthorized) {
                    Text(
                        text = if (summary.isSettled) "✓ ĐÃ XONG" else "⚠ CHỜ TRẢ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (summary.isSettled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
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
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                if (summary.isParticipant && summary.isAuthorized) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (summary.isIPayer) "Bạn đã trả trước" else "Bạn còn cần trả",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMoney(summary.myShare),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.isSettled || summary.isMyPaid || summary.isIPayer) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        )
                    }
                }
            }

            if (summary.isAuthorized) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    TextButton(
                        onClick = onViewBill,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.medium,
                    ) {
                        Text(
                            text = "Xem chi tiết",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (summary.isParticipant && !summary.isSettled && !summary.isMyPaid && !summary.isIPayer && summary.myShare > 0.0) {
                        SmallButton(
                            text = "Thanh toán",
                            onClick = onSettleUp,
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
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
    isSavedByMe: Boolean = false,
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
    var isCaptionExpanded by remember(post.id) { mutableStateOf(false) }
    val canExpandCaption = post.caption.length > 120 || post.caption.count { it == '\n' } > 1

    AppCard(
        modifier = Modifier
            .padding(horizontal = AppDimens.screenHorizontal, vertical = AppDimens.spaceSm)
            .fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(AppDimens.spaceLg),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                    modifier = Modifier.weight(1f).clickable { onAuthorClick() },
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
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(AppDimens.spaceMd),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = post.location ?: "Chưa rõ địa điểm",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(text = "•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Icon(
                                imageVector = if (post.visibility == "public") Icons.Default.Public else Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(AppDimens.spaceMd),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = if (post.visibility == "public") "Công khai" else "Bạn bè",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
                Box(modifier = Modifier.offset(x = AppDimens.spaceSm, y = -AppDimens.spaceSm)) {
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
                                onClick = { showMenu = false },
                            )
                        }
                    }
                }
            }

            if (post.imageUrls.isNotEmpty()) {
                DinePostImage(
                    imageUrl = post.imageUrls.firstOrNull(),
                    contentDescription = post.caption,
                    modifier = Modifier.padding(horizontal = AppDimens.spaceSm).aspectRatio(1f),
                    shape = AppShapes.large,
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = if (isLikedByMe) onUnlike else onLike,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(AppDimens.spaceXl),
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
                    Icon(
                        imageVector = if (isSavedByMe) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSavedByMe) "Unsave" else "Save",
                        tint = if (isSavedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(AppDimens.spaceXl)
                    )
                }
            }

            val hasLinkedBill = !post.linkedGroupId.isNullOrBlank() && !post.linkedBillId.isNullOrBlank() && post.id != post.linkedBillId
            if (hasLinkedBill) {
                if (billSummary != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    LinkedBillSummarySection(
                        summary = billSummary,
                        onViewBill = {
                            if (billSummary.isAuthorized) {
                                onSettleUp()
                            } else {
                                android.widget.Toast.makeText(context, "Bạn không có quyền xem hóa đơn này.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSettleUp = {
                            if (billSummary.isAuthorized) {
                                onSettleUp()
                            } else {
                                android.widget.Toast.makeText(context, "Bạn không có quyền xem hóa đơn này.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm)
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), AppShapes.large),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (post.caption.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = AppDimens.spaceLg)
                        .padding(bottom = AppDimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${post.authorName} ") }
                            append(post.caption)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (isCaptionExpanded) Int.MAX_VALUE else 2,
                        overflow = if (isCaptionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    )
                    if (canExpandCaption) {
                        Text(
                            text = if (isCaptionExpanded) "Ẩn bớt" else "Xem thêm",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { isCaptionExpanded = !isCaptionExpanded },
                        )
                    }
                }
            }
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
    AppCard(
        modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal, vertical = AppDimens.spaceSm).fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                DinePostImage(imageUrl = image, contentDescription = null, modifier = Modifier.fillMaxSize(), shape = AppShapes.large)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(AppDimens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                ) {
                    DineAvatarImage(imageUrl = userAvatar, name = userName, modifier = Modifier.size(AppDimens.spaceXl).border(1.dp, AppColors.surfaceWhite, CircleShape), size = AppDimens.spaceXl)
                    Text(userName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppColors.surfaceWhite)
                }
            }

            Column(modifier = Modifier.weight(1.3f).padding(AppDimens.spaceLg).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                            Text("ĐÃ THANH TOÁN", modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text(timeAgo, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp))
                    Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                    Text("\"$quote\"", style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(-AppDimens.spaceSm)) {
                        participantsAvatars.forEach { url ->
                            DineAvatarImage(imageUrl = url, name = null, modifier = Modifier.size(AppDimens.space2Xl).border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape), size = AppDimens.space2Xl)
                        }
                        if (extraParticipants > 0) {
                            Box(modifier = Modifier.size(AppDimens.space2Xl).border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                Text("+$extraParticipants", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppColors.surfaceWhite)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng hóa đơn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
        modifier = Modifier.padding(AppDimens.spaceLg).fillMaxWidth().aspectRatio(1.77f).clip(AppShapes.xLarge),
    ) {
        DinePostImage(imageUrl = image, contentDescription = null, modifier = Modifier.fillMaxSize(), shape = AppShapes.xLarge)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.9f)))))
        
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(AppDimens.spaceXl).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(status.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = AppColors.surfaceWhite.copy(alpha = 0.6f))
                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AppColors.surfaceWhite)
            }
            Surface(color = AppColors.surfaceWhite.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(AppDimens.space4Xl), border = BorderStroke(1.dp, AppColors.surfaceWhite.copy(alpha = 0.2f))) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AppColors.surfaceWhite)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    DineSplitTheme(darkTheme = false) {
        // Preview logic
    }
}
