package com.example.dinesplit.presentation.feed

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
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
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    刻 Success(val content: PostDetailContent) : PostDetailUiState
    data class Error(val message: String) : PostDetailUiState
}

data class PostDetailContent(
    val post: Post,
    val comments: List<PostComment>,
    val isLiked: Boolean,
    val isSubmittingComment: Boolean = false,
    val errorMessage: String? = null
)

data class PostComment(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val message: String,
    val createdAt: Long,
    val isPending: Boolean = false
)

class PostDetailViewModel(
    application: Application,
    private val postId: String
) : ViewModel() {

    private val firestore = AppContainer.feedRepository() // Kết nối thông qua Repository sạch thay vì gọi Firestore trực tiếp
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val getCurrentUserProfileUseCase = AppContainer.getCurrentUserProfileUseCase(application)
    private val likeUseCase = AppContainer.likePostUseCase()
    private val unlikeUseCase = AppContainer.unlikePostUseCase()

    private val _isSubmitting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PostDetailUiState> =
        combine(
            AppContainer.feedRepository().getFeedPosts(),
            AppContainer.feedRepository().getComments(postId),
            observeSessionUseCase(),
            _isSubmitting,
            _errorMessage
        ) { posts, comments, session, submitting, errorMsg ->
            val post = posts.firstOrNull { it.id == postId }
            val uid = session?.uid
            val isLiked = uid != null && post?.likedBy?.contains(uid) == true
            
            if (post == null) {
                PostDetailUiState.Error("Bài viết không tồn tại hoặc đã bị xóa")
            } else {
                // Chuyển đổi dữ liệu domain Comment sang cấu trúc dữ liệu hiển thị PostComment có tính năng pending
                val mappedComments = comments.map { domainComment ->
                    PostComment(
                        id = UUID.randomUUID().toString(), // Khởi tạo ID an toàn cho LazyColumn
                        userId = domainComment.authorUid,
                        userName = domainComment.authorName,
                        userAvatarUrl = domainComment.authorAvatar,
                        message = domainComment.content,
                        createdAt = domainComment.createdAt?.time ?: System.currentTimeMillis(),
                        isPending = false
                    )
                }
                
                PostDetailUiState.Success(
                    PostDetailContent(
                        post = post,
                        comments = mappedComments,
                        isLiked = isLiked,
                        isSubmittingComment = submitting,
                        errorMessage = errorMsg
                    )
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PostDetailUiState.Loading,
        )

    fun toggleLike() {
        val userId = observeSessionUseCase().value?.uid ?: return
        val state = uiState.value as? PostDetailUiState.Success ?: return
        viewModelScope.launch {
            if (state.content.isLiked) {
                unlikeUseCase(postId, userId)
            } else {
                likeUseCase(postId, userId)
            }
        }
    }

    fun submitComment(message: String) {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return

        val state = uiState.value as? PostDetailUiState.Success ?: return
        if (state.content.isSubmittingComment) return

        val session = observeSessionUseCase().value
        if (session == null) {
            _errorMessage.value = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            runCatching {
                val profile = getCurrentUserProfileUseCase(session.uid)
                val comment = com.example.dinesplit.domain.model.Comment(
                    authorUid = session.uid,
                    authorName = profile?.displayName ?: "Người dùng",
                    authorAvatar = profile?.avatarUrl ?: "",
                    content = trimmed,
                    createdAt = Date(),
                )
                AppContainer.feedRepository().addComment(postId, comment)
            }.onFailure { throwable ->
                _errorMessage.value = "Không thể gửi bình luận: ${throwable.localizedMessage}"
            }.onFinalized {
                _isSubmitting.value = false
            }
        }
    }

    private fun <T> Result<T>.onFinalized(action: () -> Unit): Result<T> {
        action()
        return this
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
    // KHỞI TẠO CHUẨN KIẾN TRÚC: Đưa Factory vào ngăn chặn lỗi Crash Runtime
    val vm: PostDetailViewModel = viewModel(factory = PostDetailViewModel.Factory(application, postId))
    val uiState by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState) {
        if (uiState is PostDetailUiState.Success) {
            val comments = (uiState as PostDetailUiState.Success).content.comments
            if (comments.isNotEmpty()) {
                listState.animateScrollToItem(comments.size + 1)
            }
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
        when (val state = uiState) {
            is PostDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingBlock(message = "Đang tải chi tiết bài viết...")
                }
            }
            is PostDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateBlock(
                        title = "Không tìm thấy bài viết",
                        subtitle = state.message,
                        retryText = "Quay lại",
                        onRetryClick = onBack
                    )
                }
            }
            is PostDetailUiState.Success -> {
                PostDetailBody(
                    content = state.content,
                    listState = listState,
                    onToggleLike = vm::toggleLike,
                    onSubmitComment = vm::submitComment
                )
            }
        }
    }
}

@Composable
private fun PostDetailBody(
    content: PostDetailContent,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onToggleLike: () -> Unit,
    onSubmitComment: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            item {
                PostDetailCard(
                    post = content.post,
                    isLikedByMe = content.isLiked,
                    onToggleLike = onToggleLike
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f),
                )
                Text(
                    text = "Bình luận (${content.comments.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (content.comments.isEmpty()) {
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
                items(content.comments, key = { it.id }) { comment ->
                    CommentRow(comment = comment)
                }
            }

            content.errorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                item {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        CommentInputBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            isSubmitting = content.isSubmittingComment,
            onSubmit = onSubmitComment
        )
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
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
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
private fun CommentRow(comment: PostComment) {
    val alpha = if (comment.isPending) 0.5f else 1f
    AppCard(modifier = Modifier.alpha(alpha)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.Top
        ) {
            DineAvatarImage(imageUrl = comment.userAvatarUrl, name = comment.userName, size = 36.dp)

            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs), modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = comment.userName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    
                    val commentDate = remember(comment.createdAt) { Date(comment.createdAt) }
                    Text(
                        text = formatPostDate(commentDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = comment.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (comment.isPending) {
                    Text(
                        text = "Đang gửi...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
                            text = "" 
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