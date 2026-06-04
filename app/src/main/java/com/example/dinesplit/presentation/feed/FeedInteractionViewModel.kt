package com.example.dinesplit.presentation.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Post
import com.google.android.gms.tasks.Task
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

data class FeedPostItem(
    val post: Post,
    val isLiked: Boolean
)

data class FeedInteractionUiState(
    val posts: List<FeedPostItem> = emptyList(),
    val errorMessage: String? = null
)

class FeedInteractionViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseProviders.firestore
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val getCurrentUserProfileUseCase = AppContainer.getCurrentUserProfileUseCase(application)

    private val feedRepository = AppContainer.feedRepository()

    private val _uiState = MutableStateFlow(FeedInteractionUiState())
    val uiState: StateFlow<FeedInteractionUiState> = _uiState.asStateFlow()

    fun submitPosts(posts: List<Post>) {
        _uiState.update { state ->
            val likedLookup = state.posts.associateBy({ it.post.id }, { it.isLiked })
            val items = posts.map { post ->
                FeedPostItem(
                    post = post,
                    isLiked = likedLookup[post.id] ?: false
                )
            }
            state.copy(posts = items)
        }
    }

    fun toggleLike(postId: String) {
        val current = _uiState.value.posts
        val index = current.indexOfFirst { it.post.id == postId }
        if (index < 0) return

        val item = current[index]
        val delta = if (item.isLiked) -1 else 1
        val updatedPost = item.post.copy(
            likesCount = (item.post.likesCount + delta).coerceAtLeast(0)
        )
        val updatedItem = item.copy(post = updatedPost, isLiked = !item.isLiked)
        val updatedPosts = current.toMutableList().also { it[index] = updatedItem }

        _uiState.update { it.copy(posts = updatedPosts, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                firestore.collection("posts")
                    .document(postId)
                    .update("likesCount", FieldValue.increment(delta.toLong()))
                    .awaitFirebase()
            }.onSuccess {
                if (!item.isLiked) { // Only trigger notification when liking (not unliking)
                    val session = observeSessionUseCase().value
                    if (session != null && session.uid != item.post.authorUid) {
                        val profile = getCurrentUserProfileUseCase(session.uid)
                        val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                            ?: session.email.substringBefore('@')
                        triggerSocialNotification(
                            targetUserId = item.post.authorUid,
                            title = "New Like",
                            subtitle = "$displayName liked your post",
                            type = "ACTIVITY_UPDATE",
                            relatedId = postId
                        )
                    }
                }
            }.onFailure { throwable ->
                val revertedPosts = current.toMutableList()
                revertedPosts[index] = item
                _uiState.update {
                    it.copy(
                        posts = revertedPosts,
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable)
                    )
                }
            }
        }
    }

    fun submitComment(postId: String, message: String) {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return

        val current = _uiState.value.posts
        val index = current.indexOfFirst { it.post.id == postId }
        if (index < 0) return

        val item = current[index]
        val updatedPost = item.post.copy(commentsCount = item.post.commentsCount + 1)
        val updatedItem = item.copy(post = updatedPost)
        val updatedPosts = current.toMutableList().also { it[index] = updatedItem }

        _uiState.update { it.copy(posts = updatedPosts, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val session = observeSessionUseCase().value
            if (session == null) {
                _uiState.update { it.copy(errorMessage = "Session expired. Please sign in again.") }
                revertCommentCount(postId, item)
                return@launch
            }

            val profile = getCurrentUserProfileUseCase(session.uid)
            val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                ?: session.email.substringBefore('@')

            val comment = com.example.dinesplit.domain.model.Comment(
                authorUid = session.uid,
                authorName = displayName,
                authorAvatar = profile?.avatarUrl ?: "",
                content = trimmed,
                createdAt = java.util.Date()
            )

            runCatching {
                feedRepository.addComment(postId, comment)
            }.onFailure { throwable ->
                _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) }
                revertCommentCount(postId, item)
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

    private fun revertCommentCount(postId: String, originalItem: FeedPostItem) {
        _uiState.update { state ->
            val index = state.posts.indexOfFirst { it.post.id == postId }
            if (index < 0) return@update state
            val updated = state.posts.toMutableList()
            updated[index] = originalItem
            state.copy(posts = updated)
        }
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
