package com.example.dinesplit.presentation.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val currentUser: UserProfile? = null,
    val viewedStoryIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val linkedBillSummaries: Map<String, LinkedBillSummary> = emptyMap(),
)

data class PaginationState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val getCurrentUserProfileUseCase = AppContainer.getCurrentUserProfileUseCase(application)
    private val likePostUseCase = AppContainer.likePostUseCase()
    private val unlikePostUseCase = AppContainer.unlikePostUseCase()
    private val getLinkedBillSummaryUseCase = AppContainer.getLinkedBillSummaryUseCase()

    private val _paginationState = MutableStateFlow(PaginationState())
    private val _viewedStoryIds = MutableStateFlow<Set<String>>(emptySet())
    private val _linkedBillSummaries = MutableStateFlow<Map<String, LinkedBillSummary>>(emptyMap())

    private val activeBillJobs = mutableMapOf<String, Job>()

    val uiState: StateFlow<FeedUiState> =
        combine(
            _paginationState,
            _linkedBillSummaries,
            observeSessionUseCase(),
            _viewedStoryIds,
        ) { pagination, summaries, session, viewedIds ->
            val userProfile = session?.uid?.let { getCurrentUserProfileUseCase(it) }
            FeedUiState(
                posts = pagination.posts,
                currentUser = userProfile,
                viewedStoryIds = viewedIds,
                isLoading = pagination.isLoading,
                error = pagination.error,
                isRefreshing = pagination.isRefreshing,
                canLoadMore = pagination.canLoadMore,
                isLoadingMore = pagination.isLoadingMore,
                linkedBillSummaries = summaries,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeedUiState(),
        )

    init {
        loadInitialFeed()
        viewModelScope.launch {
            observeSessionUseCase().collect { session ->
                if (session != null) {
                    observeBillSummaries(_paginationState.value.posts)
                }
            }
        }
    }

    private var feedJob: Job? = null

    private fun loadInitialFeed() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            _paginationState.value = _paginationState.value.copy(isLoading = true, error = null)
            try {
                AppContainer.feedRepository().getFeedPosts().collect { posts ->
                    _paginationState.value = _paginationState.value.copy(
                        posts = posts,
                        canLoadMore = false,
                        isLoading = false
                    )
                    observeBillSummaries(posts)
                }
            } catch (e: Exception) {
                _paginationState.value = _paginationState.value.copy(
                    error = "Không thể tải bảng tin: ${e.localizedMessage ?: "Lỗi kết nối"}",
                    isLoading = false
                )
            }
        }
    }

    fun loadNextPage() {
        // Real-time flow handles pagination natively, no manual step needed
    }

    fun refresh() {
        if (_paginationState.value.isRefreshing) return
        viewModelScope.launch {
            _paginationState.value = _paginationState.value.copy(isRefreshing = true, error = null)
            try {
                // Cancel active summaries jobs
                activeBillJobs.values.forEach { it.cancel() }
                activeBillJobs.clear()
                _linkedBillSummaries.value = emptyMap()
                
                loadInitialFeed()
            } catch (e: Exception) {
                _paginationState.value = _paginationState.value.copy(
                    error = "Không thể tải lại: ${e.localizedMessage ?: "Lỗi kết nối"}"
                )
            } finally {
                _paginationState.value = _paginationState.value.copy(isRefreshing = false)
            }
        }
    }

    private fun observeBillSummaries(posts: List<Post>) {
        val currentUserId = FirebaseProviders.auth.currentUser?.uid ?: return
        posts.forEach { post ->
            val postId = post.id
            val groupId = post.linkedGroupId
            val billId = post.linkedBillId
            if (!groupId.isNullOrBlank() && !billId.isNullOrBlank() && !activeBillJobs.containsKey(postId)) {
                val job = viewModelScope.launch {
                    getLinkedBillSummaryUseCase(groupId, billId, currentUserId).collect { summary ->
                        if (summary != null) {
                            _linkedBillSummaries.value = _linkedBillSummaries.value + (postId to summary)
                        } else {
                            _linkedBillSummaries.value = _linkedBillSummaries.value - postId
                        }
                    }
                }
                activeBillJobs[postId] = job
            }
        }
    }

    fun onLikePost(postId: String) {
        val uid = uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            likePostUseCase(postId, uid)
            // Update local likedBy list immediately for instant UI feedback
            val updatedPosts = _paginationState.value.posts.map { post ->
                if (post.id == postId) {
                    val newLikedBy = if (post.likedBy.contains(uid)) post.likedBy else post.likedBy + uid
                    post.copy(likedBy = newLikedBy, likesCount = newLikedBy.size)
                } else post
            }
            _paginationState.value = _paginationState.value.copy(posts = updatedPosts)
        }
    }

    fun onUnlikePost(postId: String) {
        val uid = uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            unlikePostUseCase(postId, uid)
            // Update local likedBy list immediately for instant UI feedback
            val updatedPosts = _paginationState.value.posts.map { post ->
                if (post.id == postId) {
                    val newLikedBy = post.likedBy - uid
                    post.copy(likedBy = newLikedBy, likesCount = newLikedBy.size)
                } else post
            }
            _paginationState.value = _paginationState.value.copy(posts = updatedPosts)
        }
    }

    fun markStoryAsViewed(postId: String) {
        _viewedStoryIds.value = _viewedStoryIds.value + postId
    }

    fun onDeletePost(postId: String) {
        viewModelScope.launch {
            AppContainer.feedRepository().deletePost(postId)
            val updatedPosts = _paginationState.value.posts.filter { it.id != postId }
            _paginationState.value = _paginationState.value.copy(posts = updatedPosts)
        }
    }
}
