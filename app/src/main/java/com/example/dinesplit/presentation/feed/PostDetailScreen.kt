package com.example.dinesplit.presentation.feed

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.DinePostImage
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.Comment
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.ui.theme.AppColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class PostDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLikedByMe: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class PostDetailViewModel(
    application: Application,
    private val postId: String,
) : ViewModel() {
    private val feedRepo = AppContainer.feedRepository()
    private val observeSession = AppContainer.observeSessionUseCase(application)
    private val getCurrentProfile = AppContainer.getCurrentUserProfileUseCase(application)
    private val likeUseCase = AppContainer.likePostUseCase()
    private val unlikeUseCase = AppContainer.unlikePostUseCase()

    private val _isSubmitting = MutableStateFlow(false)

    val uiState: StateFlow<PostDetailUiState> =
        combine(
            feedRepo.getFeedPosts(),
            feedRepo.getComments(postId),
            observeSession(),
            _isSubmitting,
        ) { posts, comments, session, submitting ->
            val post = posts.firstOrNull { it.id == postId }
            val uid = session?.uid
            val isLiked = uid != null && post?.likedBy?.contains(uid) == true
            PostDetailUiState(
                post = post,
                comments = comments,
                isLikedByMe = isLiked,
                isSubmittingComment = submitting,
                isLoading = false,
                error = if (post == null) "Bài viết không tồn tại hoặc đã bị xóa" else null,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PostDetailUiState(),
        )

    fun toggleLike() {
        val userId = observeSession().value?.uid ?: return
        val post = uiState.value.post ?: return
        viewModelScope.launch {
            if (uiState.value.isLikedByMe) {
                unlikeUseCase(post.id, userId)
            } else {
                likeUseCase(post.id, userId)
            }
        }
    }

    fun submitComment(text: String) {
        if (text.isBlank()) return
        val userId = observeSession().value?.uid ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val profile = getCurrentProfile(userId)
                val comment = Comment(
                    authorUid = userId,
                    authorName = profile?.displayName ?: "Người dùng",
                    authorAvatar = profile?.avatarUrl ?: "",
                    content = text.trim(),
                    createdAt = Date(),
                )
                feedRepo.addComment(postId, comment)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    class Factory(private val application: Application, private val postId: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = 
            PostDetailViewModel(application, postId) as T
    }
}


@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit = {},
) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val vm: PostDetailViewModel = viewModel(factory = PostDetailViewModel.Factory(application, postId))
    val uiState by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    // TỰ ĐỘNG CUỘN ĐÁY: Khi có bình luận mới được thêm vào, danh sách tự cuộn xuống dưới cùng
    LaunchedEffect(uiState.comments.size) {
        if (uiState.comments.isNotEmpty()) {
            listState.animateScrollToItem(uiState.comments.size + 1)
        }
    }

    AppScaffold(
        title = "Chi tiết bài viết",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val state = uiState
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingBlock(message = "Đang tải chi tiết bài viết...")
                    }
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorStateBlock(
                            title = "Không tìm thấy bài viết",
                            subtitle = state.error,
                            retryText = "Quay lại",
                            onRetryClick = onBack
                        )
                    }
                }
                state.post != null -> {
                    // Hiển thị nội dung chính khi dữ liệu nạp thành công
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp), // Tránh bị che bởi thanh nhập liệu dưới đáy
                        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                    ) {
                        item {
                            PostDetailCard(
                                post = state.post,
                                isLikedByMe = state.isLikedByMe,
                                onToggleLike = vm::toggleLike
                            )
                        }

                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f),
                            )
                            Text(
                                text = "Bình luận (${state.comments.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }

                        if (state.comments.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Chưa có bình luận nào. Hãy là người đầu tiên!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            // KEYED ITEMS: Ép danh sách sử dụng ID thật để tối ưu hóa Slot Table khi bình luận tăng lên
                            items(state.comments, key = { it.id }) { comment ->
                                CommentRow(comment = comment)
                            }
                        }
                    }

                    CommentInputBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .imePadding(),
                        isSubmitting = state.isSubmittingComment,
                        onSubmit = vm::submitComment
                    )
                }
            }
        }
    }
}

@Composable
private fun PostDetailCard(
    post: Post,
    isLikedByMe: Boolean,
    onToggleLike: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DineAvatarImage(imageUrl = post.authorAvatar, name = post.authorName, size = 40.dp)
                    Column {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        post.location?.takeIf { it.isNotBlank() }?.let { location ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                Text(text = location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                post.createdAt?.let { date ->
                    Text(
                        text = formatPostDate(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            if (post.imageUrls.isNotEmpty()) {
                DinePostImage(
                    imageUrl = post.imageUrls.first(),
                    contentDescription = post.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(AppDimens.radiusLg),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onToggleLike, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = if (post.likesCount > 0) "${post.likesCount} lượt thích" else "Hãy thích bài viết này!",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (post.likesCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (post.caption.isNotBlank()) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.Top
        ) {
            DineAvatarImage(imageUrl = comment.authorAvatar, name = comment.authorName, size = 36.dp)

            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs), modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = comment.authorName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    comment.createdAt?.let { date ->
                        Text(
                            text = formatPostDate(date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CommentInputBar(
    modifier: Modifier = Modifier,
    isSubmitting: Boolean,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                AppTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "",
                    placeholder = "Viết bình luận công khai...",
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val current = text.trim()
                        if (current.isNotBlank()) {
                            onSubmit(current)
                            text = "" // Xóa trống ô gõ sau khi submit thành công
                        }
                    },
                    enabled = text.isNotBlank() && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Gửi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatPostDate(date: Date): String {
    val now = Date()
    val diffMs = now.time - date.time
    val diffMins = diffMs / 60000
    return when {
        diffMins < 1 -> "Vừa xong"
        diffMins < 60 -> "${diffMins}ph"
        diffMins < 1440 -> "${diffMins / 60}g"
        diffMins < 10080 -> "${diffMins / 1440}ng"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }
}