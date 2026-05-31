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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.DinePostImage
import com.example.dinesplit.domain.model.Comment
import com.example.dinesplit.domain.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── ViewModel ───────────────────────────────────────────────────────────────

data class PostDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLikedByMe: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val isLoading: Boolean = true
)

class PostDetailViewModel(
    application: Application,
    private val postId: String
) : AndroidViewModel(application) {

    private val feedRepo = AppContainer.feedRepository()
    private val observeSession = AppContainer.observeSessionUseCase(application)
    private val getCurrentProfile = AppContainer.getCurrentUserProfileUseCase(application)
    private val likeUseCase = AppContainer.likePostUseCase()
    private val unlikeUseCase = AppContainer.unlikePostUseCase()

    private val _isSubmitting = MutableStateFlow(false)

    val uiState: StateFlow<PostDetailUiState> = combine(
        feedRepo.getFeedPosts(),
        feedRepo.getComments(postId),
        observeSession(),
        _isSubmitting
    ) { posts, comments, session, submitting ->
        val post = posts.firstOrNull { it.id == postId }
        val uid = session?.uid
        val isLiked = uid != null && post?.likedBy?.contains(uid) == true
        PostDetailUiState(
            post = post,
            comments = comments,
            isLikedByMe = isLiked,
            isSubmittingComment = submitting,
            isLoading = post == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PostDetailUiState()
    )

    fun toggleLike() {
        val userId = observeSession().value?.uid ?: return
        val post = uiState.value.post ?: return
        viewModelScope.launch {
            if (uiState.value.isLikedByMe) unlikeUseCase(post.id, userId)
            else likeUseCase(post.id, userId)
        }
    }

    fun likePost() {
        val userId = observeSession().value?.uid ?: return
        viewModelScope.launch {
            likeUseCase(postId, userId)
        }
    }

    fun unlikePost() {
        val userId = observeSession().value?.uid ?: return
        viewModelScope.launch {
            unlikeUseCase(postId, userId)
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
                    createdAt = Date()
                )
                feedRepo.addComment(postId, comment)
            } catch (e: Exception) {
                // Xử lý ngoại lệ nếu cần
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    class Factory(private val application: Application, private val postId: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            PostDetailViewModel(application, postId) as T
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit = {}
) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val vm: PostDetailViewModel = viewModel(factory = PostDetailViewModel.Factory(application, postId))
    val uiState by vm.uiState.collectAsState()
    var commentText by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    // Scroll to bottom when comments grow
    LaunchedEffect(uiState.comments.size) {
        if (uiState.comments.isNotEmpty()) {
            listState.animateScrollToItem(uiState.comments.size + 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bài viết", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Comment input bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Viết bình luận...", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f)
                        ),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    IconButton(
                        onClick = {
                            vm.submitComment(commentText.text)
                            commentText = TextFieldValue("")
                        },
                        enabled = commentText.text.isNotBlank() && !uiState.isSubmittingComment,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (commentText.text.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                CircleShape
                            )
                    ) {
                        if (uiState.isSubmittingComment) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi",
                                tint = if (commentText.text.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        val post = uiState.post
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (post != null) {
                item {
                    PostContent(post = post, isLikedByMe = uiState.isLikedByMe, onLike = vm::likePost, onUnlike = vm::unlikePost)
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                    Text(
                        text = "Bình luận (${uiState.comments.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (uiState.comments.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Chưa có bình luận nào. Hãy là người đầu tiên!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                items(uiState.comments) { comment ->
                    CommentItem(comment = comment)
                }
            } else if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

// ─── Post content section ─────────────────────────────────────────────────────

@Composable
private fun PostContent(
    post: Post,
    isLikedByMe: Boolean,
    onLike: () -> Unit,
    onUnlike: () -> Unit
) {
    Column {
        // Author row
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DineAvatarImage(imageUrl = post.authorAvatar, name = post.authorName, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(post.authorName, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                if (!post.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                        Text(post.location, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            post.createdAt?.let { date ->
                Text(
                    text = formatPostDate(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Post image
        if (post.imageUrls.isNotEmpty()) {
            DinePostImage(
                imageUrl = post.imageUrls.first(),
                contentDescription = post.caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(0.dp)
            )
        }

        // Caption
        if (post.caption.isNotBlank()) {
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // Like row
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = if (isLikedByMe) onUnlike else onLike,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLikedByMe) "Bỏ thích" else "Thích",
                    tint = if (isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (post.likesCount > 0) {
                Text(
                    "${post.likesCount} lượt thích",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            } else {
                Text("Hãy thích bài viết này!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// ─── Comment item ─────────────────────────────────────────────────────────────

@Composable
private fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        DineAvatarImage(imageUrl = comment.authorAvatar, name = comment.authorName, size = 36.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(comment.authorName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                comment.createdAt?.let { date ->
                    Text(formatPostDate(date), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(comment.content, style = MaterialTheme.typography.bodySmall)
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
