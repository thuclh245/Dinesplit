package com.example.dinesplit.presentation.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FollowListUiState(
    val isLoading: Boolean = true,
    val inspectedUser: UserProfile? = null,
    val following: List<UserProfile> = emptyList(),
    val followers: List<UserProfile> = emptyList(),
    val friends: List<UserProfile> = emptyList(),
    val myFollowingIds: Set<String> = emptySet(),
    val myFollowerIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val followActionBusyUserIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class FollowListViewModel(
    application: Application,
    private val targetUid: String
) : AndroidViewModel(application) {

    private val profileRepo = AppContainer.profileRepository(application)
    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    private val currentUserId = FirebaseProviders.auth.currentUser?.uid

    init {
        loadData()
    }

    fun loadData() {
        if (targetUid.isBlank()) {
            _uiState.value = FollowListUiState(isLoading = false, errorMessage = "User not found")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                // 1. Fetch inspected user profile
                val user = profileRepo.getProfile(targetUid)
                if (user == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "User not found")
                    return@launch
                }

                // 2. Fetch followers & following
                val followersResult = profileRepo.getFollowers(targetUid).getOrDefault(emptyList())
                val followingResult = profileRepo.getFollowing(targetUid).getOrDefault(emptyList())

                // 3. Intersect friends (mutual followings)
                val friendsResult = followersResult.filter { follower ->
                    followingResult.any { followed -> followed.uid == follower.uid }
                }

                // 4. Fetch current user's follow data (to compute relationships of list items to viewer)
                var myFollowings: Set<String> = emptySet()
                var myFollowers: Set<String> = emptySet()
                if (!currentUserId.isNullOrBlank()) {
                    val myProfile = profileRepo.getProfile(currentUserId)
                    if (myProfile != null) {
                        // Use list from document or query
                        val myFollowingsResult = profileRepo.getFollowing(currentUserId).getOrDefault(emptyList())
                        val myFollowersResult = profileRepo.getFollowers(currentUserId).getOrDefault(emptyList())
                        myFollowings = myFollowingsResult.map { it.uid }.toSet()
                        myFollowers = myFollowersResult.map { it.uid }.toSet()
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    inspectedUser = user,
                    followers = followersResult,
                    following = followingResult,
                    friends = friendsResult,
                    myFollowingIds = myFollowings,
                    myFollowerIds = myFollowers
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Error loading lists"
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleFollowUserInList(targetUserUid: String) {
        val currentUid = currentUserId ?: return
        val state = _uiState.value
        if (state.followActionBusyUserIds.contains(targetUserUid)) return

        _uiState.value = state.copy(
            followActionBusyUserIds = state.followActionBusyUserIds + targetUserUid
        )

        val isCurrentlyFollowing = state.myFollowingIds.contains(targetUserUid)

        viewModelScope.launch {
            if (isCurrentlyFollowing) {
                profileRepo.unfollowUser(currentUid, targetUserUid)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            myFollowingIds = _uiState.value.myFollowingIds - targetUserUid,
                            followActionBusyUserIds = _uiState.value.followActionBusyUserIds - targetUserUid
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Unfollow failed: ${error.localizedMessage}",
                            followActionBusyUserIds = _uiState.value.followActionBusyUserIds - targetUserUid
                        )
                    }
            } else {
                profileRepo.followUser(currentUid, targetUserUid)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            myFollowingIds = _uiState.value.myFollowingIds + targetUserUid,
                            followActionBusyUserIds = _uiState.value.followActionBusyUserIds - targetUserUid
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Follow failed: ${error.localizedMessage}",
                            followActionBusyUserIds = _uiState.value.followActionBusyUserIds - targetUserUid
                        )
                    }
            }
        }
    }

    class Factory(private val application: Application, private val targetUid: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FollowListViewModel(application, targetUid) as T
    }
}
