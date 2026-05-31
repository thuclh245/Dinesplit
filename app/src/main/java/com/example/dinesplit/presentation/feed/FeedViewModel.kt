package com.example.dinesplit.presentation.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.UserProfile
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
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val getFeedUseCase = AppContainer.getFeedUseCase()
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val getCurrentUserProfileUseCase = AppContainer.getCurrentUserProfileUseCase(application)
    private val likePostUseCase = AppContainer.likePostUseCase()
    private val unlikePostUseCase = AppContainer.unlikePostUseCase()

    private val _viewedStoryIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<FeedUiState> =
        combine(
            getFeedUseCase(),
            observeSessionUseCase(),
            _viewedStoryIds,
        ) { posts, session, viewedIds ->
            val userProfile = session?.uid?.let { getCurrentUserProfileUseCase(it) }
            FeedUiState(
                posts = posts,
                currentUser = userProfile,
                viewedStoryIds = viewedIds,
                isLoading = false,
                error = null,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeedUiState(),
        )

    fun refresh() {
        // getFeedUseCase is a Flow; re-subscribing is not needed.
        // This is a no-op placeholder — Firebase snapshot listeners auto-refresh.
    }

    fun onLikePost(postId: String) {
        val uid = uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            likePostUseCase(postId, uid)
        }
    }

    fun onUnlikePost(postId: String) {
        val uid = uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            unlikePostUseCase(postId, uid)
        }
    }

    fun markStoryAsViewed(postId: String) {
        _viewedStoryIds.value = _viewedStoryIds.value + postId
    }

    fun onDeletePost(postId: String) {
        viewModelScope.launch {
            AppContainer.feedRepository().deletePost(postId)
        }
    }
}
