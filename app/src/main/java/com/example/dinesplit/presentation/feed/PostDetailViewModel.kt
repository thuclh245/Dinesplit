package com.example.dinesplit.presentation.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Post
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data class Success(val content: PostDetailContent) : PostDetailUiState
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

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseProviders.firestore
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val getCurrentUserProfileUseCase = AppContainer.getCurrentUserProfileUseCase(application)

    private val likedPostIds = mutableSetOf<String>()
    private var currentPostId: String? = null

    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    fun loadPost(postId: String) {
        if (postId.isBlank()) {
            _uiState.value = PostDetailUiState.Error("Invalid post")
            return
        }

        currentPostId = postId
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = PostDetailUiState.Loading
            runCatching {
                val post = fetchPost(postId) ?: error("Post not found")
                val comments = fetchComments(postId)
                PostDetailContent(
                    post = post,
                    comments = comments,
                    isLiked = likedPostIds.contains(postId)
                )
            }.onSuccess { content ->
                _uiState.value = PostDetailUiState.Success(content)
            }.onFailure { throwable ->
                _uiState.value = PostDetailUiState.Error(FirebaseErrorMapper.toUserMessage(throwable))
            }
        }
    }

    fun toggleLike() {
        val postId = currentPostId ?: return
        val state = _uiState.value as? PostDetailUiState.Success ?: return
        val isLiked = state.content.isLiked
        val delta = if (isLiked) -1 else 1
        val updatedPost = state.content.post.copy(
            likesCount = (state.content.post.likesCount + delta).coerceAtLeast(0)
        )

        val updatedContent = state.content.copy(post = updatedPost, isLiked = !isLiked, errorMessage = null)
        _uiState.value = PostDetailUiState.Success(updatedContent)

        if (isLiked) {
            likedPostIds.remove(postId)
        } else {
            likedPostIds.add(postId)
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                firestore.collection("posts")
                    .document(postId)
                    .update("likesCount", FieldValue.increment(delta.toLong()))
                    .awaitFirebase()
            }.onSuccess {
                if (!isLiked) { // Only trigger notification when liking
                    val session = observeSessionUseCase().value
                    if (session != null && session.uid != updatedPost.userId) {
                        val profile = getCurrentUserProfileUseCase(session.uid)
                        val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                            ?: session.email.substringBefore('@')
                        triggerSocialNotification(
                            targetUserId = updatedPost.userId,
                            title = "New Like",
                            subtitle = "$displayName liked your post",
                            type = "ACTIVITY_UPDATE",
                            relatedId = postId
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update { current ->
                    val contentState = current as? PostDetailUiState.Success ?: return@update current
                    PostDetailUiState.Success(
                        contentState.content.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable))
                    )
                }
            }
        }
    }

    fun submitComment(message: String) {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return

        val postId = currentPostId ?: return
        val state = _uiState.value as? PostDetailUiState.Success ?: return
        if (state.content.isSubmittingComment) return

        val session = observeSessionUseCase().value
        if (session == null) {
            _uiState.value = PostDetailUiState.Success(
                state.content.copy(errorMessage = "Session expired. Please sign in again.")
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val commentId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val profile = getCurrentUserProfileUseCase(session.uid)
            val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                ?: session.email.substringBefore('@')

            val pendingComment = PostComment(
                id = commentId,
                userId = session.uid,
                userName = displayName,
                userAvatarUrl = profile?.avatarUrl,
                message = trimmed,
                createdAt = now,
                isPending = true
            )

            val optimisticPost = state.content.post.copy(commentsCount = state.content.post.commentsCount + 1)
            val optimisticComments = state.content.comments + pendingComment
            _uiState.value = PostDetailUiState.Success(
                state.content.copy(
                    post = optimisticPost,
                    comments = optimisticComments,
                    isSubmittingComment = true,
                    errorMessage = null
                )
            )

            val commentPayload = mapOf(
                "id" to commentId,
                "userId" to session.uid,
                "userName" to displayName,
                "userAvatarUrl" to profile?.avatarUrl,
                "message" to trimmed,
                "createdAt" to now
            )

            runCatching {
                firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(commentId)
                    .set(commentPayload)
                    .awaitFirebase()

                firestore.collection("posts")
                    .document(postId)
                    .update("commentsCount", FieldValue.increment(1))
                    .awaitFirebase()
            }.onSuccess {
                if (session.uid != optimisticPost.userId) {
                    triggerSocialNotification(
                        targetUserId = optimisticPost.userId,
                        title = "New Comment",
                        subtitle = "$displayName commented on your post",
                        type = "ACTIVITY_UPDATE",
                        relatedId = postId
                    )
                }
                val refreshed = fetchComments(postId)
                _uiState.value = PostDetailUiState.Success(
                    state.content.copy(
                        post = optimisticPost,
                        comments = refreshed,
                        isSubmittingComment = false
                    )
                )
            }.onFailure { throwable ->
                val fallbackComments = optimisticComments.filterNot { it.id == commentId }
                val fallbackPost = optimisticPost.copy(
                    commentsCount = (optimisticPost.commentsCount - 1).coerceAtLeast(0)
                )
                _uiState.value = PostDetailUiState.Success(
                    state.content.copy(
                        post = fallbackPost,
                        comments = fallbackComments,
                        isSubmittingComment = false,
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                    )
                )
            }
        }
    }

    private fun triggerSocialNotification(
        targetUserId: String,
        title: String,
        subtitle: String,
        type: String,
        relatedId: String
    ) {
        val notificationId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val notificationData = mapOf(
            "id" to notificationId,
            "userId" to targetUserId,
            "title" to title,
            "subtitle" to subtitle,
            "type" to type,
            "relatedId" to relatedId,
            "isRead" to false,
            "createdAt" to now,
            "updatedAt" to now,
            "deepLinkDestination" to "POST_DETAIL",
            "deepLinkTargetId" to relatedId
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                firestore.collection("user_notifications")
                    .document(targetUserId)
                    .collection("notifications")
                    .document(notificationId)
                    .set(notificationData)
                    .awaitFirebase()
            }
        }
    }

    private suspend fun fetchPost(postId: String): Post? {
        val snapshot = firestore.collection("posts")
            .document(postId)
            .get()
            .awaitFirebase()
        return snapshot.toPost()
    }

    private suspend fun fetchComments(postId: String): List<PostComment> {
        val snapshot = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt")
            .get()
            .awaitFirebase()

        return snapshot.documents.mapNotNull { document ->
            document.toPostComment()
        }
    }

    private fun DocumentSnapshot.toPost(): Post? {
        if (!exists()) return null
        val postId = getString("id")?.takeIf { it.isNotBlank() } ?: id
        val userId = getString("userId") ?: return null
        val userName = getString("userName") ?: return null
        val mainImageUrl = getString("mainImageUrl") ?: return null
        val caption = getString("caption") ?: ""
        val dinersCount = getLong("dinersCount")?.toInt() ?: 0
        val likesCount = getLong("likesCount")?.toInt() ?: 0
        val commentsCount = getLong("commentsCount")?.toInt() ?: 0
        val shareAmount = getDouble("shareAmount") ?: getLong("shareAmount")?.toDouble() ?: 0.0
        val createdAt = getLong("createdAt") ?: 0L
        val userAvatarUrl = getString("userAvatarUrl")
        val location = getString("location")

        return Post(
            id = postId,
            userId = userId,
            userName = userName,
            userAvatarUrl = userAvatarUrl,
            location = location,
            mainImageUrl = mainImageUrl,
            dinersCount = dinersCount,
            likesCount = likesCount,
            commentsCount = commentsCount,
            caption = caption,
            shareAmount = shareAmount,
            createdAt = createdAt
        )
    }

    private fun DocumentSnapshot.toPostComment(): PostComment? {
        if (!exists()) return null
        val commentId = getString("id")?.takeIf { it.isNotBlank() } ?: id
        val userId = getString("userId") ?: return null
        val userName = getString("userName") ?: return null
        val message = getString("message") ?: return null
        val createdAt = getLong("createdAt") ?: 0L
        val userAvatarUrl = getString("userAvatarUrl")

        return PostComment(
            id = commentId,
            userId = userId,
            userName = userName,
            userAvatarUrl = userAvatarUrl,
            message = message,
            createdAt = createdAt
        )
    }

    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase task failed")
                    )
                }
            }
        }
    }
}
