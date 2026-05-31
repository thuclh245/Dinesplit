package com.example.dinesplit.presentation.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ProfileRepository
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateGroupUiState(
    val groupName: String = "",
    val selectedCategory: String = "Du lịch",
    val searchQuery: String = "",
    val currentProfile: UserProfile? = null,
    val searchResults: List<UserProfile> = emptyList(),
    val selectedProfiles: List<UserProfile> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val isCreated: Boolean = false,
    val error: String? = null,
) {
    val selectedMemberIds: Set<String>
        get() = selectedProfiles.map { it.uid }.toSet()

    val totalMemberCount: Int
        get() = selectedProfiles.size + if (currentProfile != null) 1 else 0
}

class CreateGroupViewModel(
    private val repository: SplitRepository,
    private val profileRepository: ProfileRepository,
    private val currentUserId: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCurrentProfile()
        searchProfiles("")
    }

    fun onGroupNameChange(value: String) {
        _uiState.update { it.copy(groupName = value, error = null) }
    }

    fun onCategorySelected(value: String) {
        _uiState.update { it.copy(selectedCategory = value) }
    }

    fun onSearchQueryChange(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                delay(250)
                searchProfiles(value)
            }
    }

    fun onProfileToggled(profile: UserProfile) {
        _uiState.update { state ->
            val selected = state.selectedProfiles.toMutableList()
            val index = selected.indexOfFirst { it.uid == profile.uid }
            if (index >= 0) {
                selected.removeAt(index)
            } else {
                selected.add(profile)
            }
            state.copy(selectedProfiles = selected, error = null)
        }
    }

    fun createGroup() {
        val currentState = _uiState.value
        val trimmedName = currentState.groupName.trim()

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(error = "Tên nhóm không được để trống") }
            return
        }

        if (currentState.currentProfile == null) {
            _uiState.update { it.copy(error = "Không tìm thấy hồ sơ người dùng hiện tại") }
            return
        }

        if (currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                val now = System.currentTimeMillis()
                val members =
                    buildSelectedMembers(
                        currentProfile = currentState.currentProfile,
                        selectedProfiles = currentState.selectedProfiles,
                    )
                val group =
                    Group(
                        id = UUID.randomUUID().toString(),
                        name = trimmedName,
                        imageUrl = null,
                        memberCount = members.size,
                        totalExpense = 0.0,
                        yourBalance = 0.0,
                        createdAt = now,
                        ownerId = currentState.currentProfile.uid,
                    )

                repository.createGroup(group, members)
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, isCreated = true) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Không thể tạo nhóm",
                    )
                }
            }
        }
    }

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            val uid = currentUserId
            if (uid.isNullOrBlank()) {
                _uiState.update { it.copy(error = "Bạn cần đăng nhập để tạo nhóm") }
                return@launch
            }

            val profile = profileRepository.getProfile(uid) ?: fallbackCurrentProfile(uid)
            _uiState.update { it.copy(currentProfile = profile) }
        }
    }

    private fun searchProfiles(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            val result = profileRepository.searchProfiles(query)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { profiles ->
                        state.copy(
                            searchResults =
                                profiles
                                    .filter { it.uid != currentUserId }
                                    .sortedBy { it.displayName.lowercase() },
                            isSearching = false,
                        )
                    },
                    onFailure = { throwable ->
                        state.copy(
                            isSearching = false,
                            error = throwable.message ?: "Không thể tìm người dùng",
                        )
                    },
                )
            }
        }
    }

    private fun buildSelectedMembers(
        currentProfile: UserProfile,
        selectedProfiles: List<UserProfile>,
    ): List<Member> {
        val currentMember = currentProfile.toMember(isMe = true)
        val selectedMembers = selectedProfiles.map { it.toMember(isMe = false) }
        return (listOf(currentMember) + selectedMembers).distinctBy { it.id }
    }

    private fun UserProfile.toMember(isMe: Boolean): Member {
        val name = displayName.ifBlank { username.ifBlank { email } }
        return Member(
            id = uid,
            name = name,
            initial = name.firstOrNull()?.uppercase().orEmpty(),
            isMe = isMe,
        )
    }

    private fun fallbackCurrentProfile(uid: String): UserProfile {
        val now = java.util.Date()
        return UserProfile(
            uid = uid,
            displayName = "Bạn",
            username = "me",
            email = "",
            avatarUrl = "",
            bio = "",
            createdAt = now,
            updatedAt = now,
        )
    }
}
