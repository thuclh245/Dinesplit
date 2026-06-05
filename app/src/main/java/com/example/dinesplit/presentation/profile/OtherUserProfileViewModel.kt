package com.example.dinesplit.presentation.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class OtherUserProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val posts: List<Post> = emptyList(),
    val taggedBills: List<LinkedBillSummary> = emptyList(),
    val isFollowing: Boolean = false,
    val isFollowedByOther: Boolean = false,
    val isFollowActionBusy: Boolean = false,
    val errorMessage: String? = null
)

class OtherUserProfileViewModel(
    application: Application,
    private val targetUserRef: String
) : AndroidViewModel(application) {

    private val profileRepo = AppContainer.profileRepository(application)
    private val feedRepo = AppContainer.feedRepository()
    private val splitRepo = AppContainer.splitRepository(application)

    private val _uiState = MutableStateFlow(OtherUserProfileUiState())
    val uiState: StateFlow<OtherUserProfileUiState> = _uiState.asStateFlow()

    private val currentUserId = FirebaseProviders.auth.currentUser?.uid
    private var resolvedTargetUid: String = targetUserRef

    init {
        loadData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadData() {
        if (targetUserRef.isBlank()) {
            _uiState.value = OtherUserProfileUiState(isLoading = false, errorMessage = "User not found")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val profile = resolveTargetProfile(targetUserRef)
                if (profile == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Profile not found")
                    return@launch
                }
                val targetUid = profile.uid
                resolvedTargetUid = targetUid

                val following = if (!currentUserId.isNullOrBlank()) {
                    profileRepo.isFollowing(currentUserId, targetUid).getOrDefault(false)
                } else {
                    false
                }

                val followedByOther = if (!currentUserId.isNullOrBlank()) {
                    profileRepo.isFollowing(targetUid, currentUserId).getOrDefault(false)
                } else {
                    false
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    isFollowing = following,
                    isFollowedByOther = followedByOther
                )

                launch {
                    feedRepo.getUserPosts(targetUid).collect { userPosts ->
                        _uiState.value = _uiState.value.copy(posts = userPosts)
                    }
                }

                if (!currentUserId.isNullOrBlank()) {
                    launch {
                        splitRepo.getGroups().flatMapLatest { groups ->
                            if (groups.isEmpty()) {
                                flowOf(emptyList<LinkedBillSummary>())
                            } else {
                                val billFlows = groups.map { group ->
                                    splitRepo.getBills(group.id).map { bills ->
                                        bills.filter { bill ->
                                            bill.payerId == targetUid || bill.shares.containsKey(targetUid)
                                        }.map { bill ->
                                            val isIPayer = bill.payerId == targetUid
                                            val myShare = bill.shares[targetUid] ?: 0.0
                                            val isMyPaid = targetUid in bill.paidMemberIds
                                            val isSettled = bill.status == com.example.dinesplit.domain.model.BillStatus.SETTLED

                                            LinkedBillSummary(
                                                billId = bill.id,
                                                groupId = group.id,
                                                billName = bill.name,
                                                totalAmount = bill.totalAmount,
                                                isSettled = isSettled,
                                                myShare = myShare,
                                                isMyPaid = isMyPaid,
                                                isIPayer = isIPayer,
                                                isParticipant = true
                                            )
                                        }
                                    }
                                }
                                combine(billFlows) { arrays ->
                                    arrays.flatMap { it }.sortedByDescending { it.billId }
                                }
                            }
                        }.collect { summaries ->
                            _uiState.value = _uiState.value.copy(taggedBills = summaries)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Error loading profile")
            }
        }
    }

    private suspend fun resolveTargetProfile(target: String): UserProfile? {
        val cleanTarget = target.trim().removePrefix("@")
        profileRepo.getProfile(cleanTarget)?.let { return it }

        return profileRepo.searchProfiles(cleanTarget, limit = 5)
            .getOrDefault(emptyList())
            .firstOrNull { profile ->
                profile.uid == cleanTarget || profile.username.equals(cleanTarget, ignoreCase = true)
            }
    }

    fun toggleFollow() {
        val currentUid = currentUserId ?: return
        val targetUid = resolvedTargetUid
        val state = _uiState.value
        if (state.profile == null || state.isFollowActionBusy) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFollowActionBusy = true)
            if (state.isFollowing) {
                profileRepo.unfollowUser(currentUid, targetUid)
                    .onSuccess {
                        val updatedProfile = state.profile.copy(
                            followersCount = maxOf(0, state.profile.followersCount - 1)
                        )
                        _uiState.value = _uiState.value.copy(
                            isFollowing = false,
                            profile = updatedProfile,
                            isFollowActionBusy = false
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isFollowActionBusy = false,
                            errorMessage = "Unfollow failed: ${error.localizedMessage}"
                        )
                    }
            } else {
                profileRepo.followUser(currentUid, targetUid)
                    .onSuccess {
                        val updatedProfile = state.profile.copy(
                            followersCount = state.profile.followersCount + 1
                        )
                        _uiState.value = _uiState.value.copy(
                            isFollowing = true,
                            profile = updatedProfile,
                            isFollowActionBusy = false
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isFollowActionBusy = false,
                            errorMessage = "Follow failed: ${error.localizedMessage}"
                        )
                    }
            }
        }
    }

    class Factory(private val application: Application, private val targetUid: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OtherUserProfileViewModel(application, targetUid) as T
    }
}
