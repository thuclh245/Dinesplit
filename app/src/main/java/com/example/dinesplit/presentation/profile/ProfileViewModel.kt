package com.example.dinesplit.presentation.profile

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.domain.exception.UsernameAlreadyExistsException
import com.example.dinesplit.domain.model.LinkedBillSummary
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.usecase.GetCurrentUserProfileUseCase
import com.example.dinesplit.domain.usecase.LogoutUseCase
import com.example.dinesplit.domain.usecase.ObserveSessionUseCase
import com.example.dinesplit.domain.usecase.UpdateProfileUseCase
import com.example.dinesplit.domain.usecase.UploadAvatarUseCase
import com.example.dinesplit.domain.validation.ProfileInputValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val errorMessage: String? = null,
    val isLoggingOut: Boolean = false,
    val posts: List<Post> = emptyList(),
    val savedPosts: List<Post> = emptyList(),
    val taggedBills: List<LinkedBillSummary> = emptyList(),
    val isSettingsDialogOpen: Boolean = false,
)

data class EditProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val avatarLocalUri: String? = null,
    val avatarError: String? = null,
    val isAvatarUploading: Boolean = false,
    val displayNameError: String? = null,
    val usernameError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val selectedStyles: List<String> = emptyList(),
)

sealed interface ProfileUiEffect {
    data object LogoutSuccess : ProfileUiEffect

    data object SaveSuccess : ProfileUiEffect
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val observeSessionUseCase: ObserveSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase =
        AppContainer.getCurrentUserProfileUseCase(application)
    private val updateProfileUseCase: UpdateProfileUseCase = AppContainer.updateProfileUseCase(application)
    private val uploadAvatarUseCase: UploadAvatarUseCase = AppContainer.uploadAvatarUseCase(application)
    private val logoutUseCase: LogoutUseCase = AppContainer.logoutUseCase(application)

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _editUiState = MutableStateFlow(EditProfileUiState())
    val editUiState: StateFlow<EditProfileUiState> = _editUiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileUiEffect>()
    val effect: SharedFlow<ProfileUiEffect> = _effect.asSharedFlow()

    private var postsJob: kotlinx.coroutines.Job? = null
    private var savedPostsJob: kotlinx.coroutines.Job? = null
    private var taggedBillsJob: kotlinx.coroutines.Job? = null

    init {
        loadProfile()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun loadProfile() {
        val session = observeSessionUseCase().value
        if (session == null) {
            _profileUiState.value = ProfileUiState(isLoading = false, errorMessage = "Session expired")
            _editUiState.value = EditProfileUiState()
            return
        }

        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            AppContainer.feedRepository().getUserPosts(session.uid).collect { posts ->
                _profileUiState.value = _profileUiState.value.copy(posts = posts)
            }
        }

        savedPostsJob?.cancel()
        savedPostsJob = viewModelScope.launch {
            AppContainer.feedRepository().getSavedPosts(session.uid).collect { savedPosts ->
                _profileUiState.value = _profileUiState.value.copy(savedPosts = savedPosts)
            }
        }

        taggedBillsJob?.cancel()
        taggedBillsJob = viewModelScope.launch {
            val splitRepo = AppContainer.splitRepository()
            splitRepo.getGroups().flatMapLatest { groups ->
                if (groups.isEmpty()) {
                    flowOf(emptyList<LinkedBillSummary>())
                } else {
                    val billFlows = groups.map { group ->
                        splitRepo.getBills(group.id).map { bills ->
                            bills.filter { bill ->
                                bill.payerId == session.uid || bill.shares.containsKey(session.uid)
                            }.map { bill ->
                                val isIPayer = bill.payerId == session.uid
                                val myShare = bill.shares[session.uid] ?: 0.0
                                val isMyPaid = session.uid in bill.paidMemberIds
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
                _profileUiState.value = _profileUiState.value.copy(taggedBills = summaries)
            }
        }

        viewModelScope.launch {
            _profileUiState.value = _profileUiState.value.copy(isLoading = true, errorMessage = null)
            val profile = getCurrentUserProfileUseCase(session.uid)
            if (profile == null) {
                _profileUiState.value =
                    _profileUiState.value.copy(
                        isLoading = false,
                        profile = null,
                        errorMessage = "Profile not completed",
                    )
                _editUiState.value = EditProfileUiState()
                return@launch
            }

            _profileUiState.value =
                _profileUiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    errorMessage = null,
                )
            _editUiState.value =
                EditProfileUiState(
                    displayName = profile.displayName,
                    username = profile.username,
                    bio = profile.bio,
                    avatarUrl = profile.avatarUrl,
                    selectedStyles = profile.diningStyles,
                )
        }
    }

    fun onDisplayNameChange(value: String) {
        _editUiState.value = _editUiState.value.copy(displayName = value, displayNameError = null, submitError = null)
    }

    fun onUsernameChange(value: String) {
        _editUiState.value = _editUiState.value.copy(username = value, usernameError = null, submitError = null)
    }

    fun onBioChange(value: String) {
        _editUiState.value = _editUiState.value.copy(bio = value, submitError = null)
    }

    fun onAvatarSelected(value: Uri) {
        _editUiState.value =
            _editUiState.value.copy(
                avatarUrl = value.toString(),
                avatarLocalUri = value.toString(),
                avatarError = null,
                submitError = null,
            )
    }

    fun onAvatarCleared() {
        _editUiState.value =
            _editUiState.value.copy(
                avatarUrl = "",
                avatarLocalUri = null,
                avatarError = null,
                submitError = null,
            )
    }

    fun toggleDiningStyle(style: String) {
        val currentSelected = _editUiState.value.selectedStyles
        val updated =
            if (currentSelected.contains(style)) {
                currentSelected - style
            } else {
                currentSelected + style
            }
        _editUiState.value = _editUiState.value.copy(selectedStyles = updated)
    }

    fun saveProfile() {
        val profile = _profileUiState.value.profile ?: return
        val current = _editUiState.value
        if (current.isSubmitting) return

        val displayNameError = ProfileInputValidator.validateDisplayName(current.displayName)
        val usernameError = ProfileInputValidator.validateUsername(current.username)
        if (displayNameError != null || usernameError != null) {
            _editUiState.value = current.copy(displayNameError = displayNameError, usernameError = usernameError)
            return
        }

        viewModelScope.launch {
            _editUiState.value =
                _editUiState.value.copy(
                    isSubmitting = true,
                    isAvatarUploading = false,
                    submitError = null,
                    avatarError = null,
                )

            val finalAvatarUrl =
                uploadAvatarIfNeeded(profile.uid, current.avatarLocalUri)
                    ?: return@launch

            val updatedProfile =
                profile.copy(
                    displayName = current.displayName.trim(),
                    username = current.username.trim(),
                    avatarUrl = finalAvatarUrl,
                    bio = current.bio.trim(),
                    diningStyles = current.selectedStyles,
                    updatedAt = Date(),
                )

            updateProfileUseCase(updatedProfile)
                .onSuccess {
                    _profileUiState.value = _profileUiState.value.copy(profile = updatedProfile)
                    _editUiState.value =
                        _editUiState.value.copy(
                            isSubmitting = false,
                            isAvatarUploading = false,
                            avatarUrl = finalAvatarUrl,
                            avatarLocalUri = null,
                            selectedStyles = updatedProfile.diningStyles,
                        )
                    _effect.emit(ProfileUiEffect.SaveSuccess)
                }
                .onFailure { throwable ->
                    _editUiState.value =
                        if (throwable is UsernameAlreadyExistsException) {
                            _editUiState.value.copy(
                                isSubmitting = false,
                                isAvatarUploading = false,
                                usernameError = FirebaseErrorMapper.toUserMessage(throwable),
                                submitError = null,
                            )
                        } else {
                            _editUiState.value.copy(
                                isSubmitting = false,
                                isAvatarUploading = false,
                                submitError = FirebaseErrorMapper.toUserMessage(throwable),
                            )
                        }
                }
        }
    }

    private suspend fun uploadAvatarIfNeeded(
        uid: String,
        avatarLocalUri: String?,
    ): String? {
        if (avatarLocalUri.isNullOrBlank()) {
            return _editUiState.value.avatarUrl.trim()
        }

        _editUiState.value = _editUiState.value.copy(isAvatarUploading = true)
        return uploadAvatarUseCase(uid, avatarLocalUri.toUri())
            .onSuccess { uploadedAvatarUrl ->
                _editUiState.value =
                    _editUiState.value.copy(
                        avatarUrl = uploadedAvatarUrl,
                        avatarLocalUri = null,
                        isAvatarUploading = false,
                    )
            }
            .onFailure { throwable ->
                _editUiState.value =
                    _editUiState.value.copy(
                        isSubmitting = false,
                        isAvatarUploading = false,
                        avatarError = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
            .getOrNull()
    }

    fun logout() {
        if (_profileUiState.value.isLoggingOut) return

        viewModelScope.launch {
            _profileUiState.value = _profileUiState.value.copy(isLoggingOut = true)
            logoutUseCase()
            _profileUiState.value = _profileUiState.value.copy(isLoggingOut = false)
            _effect.emit(ProfileUiEffect.LogoutSuccess)
        }
    }

    fun setAccountPrivacy(isPublic: Boolean) {
        val profile = _profileUiState.value.profile ?: return
        val updatedProfile = profile.copy(isPublic = isPublic)
        viewModelScope.launch {
            updateProfileUseCase(updatedProfile)
                .onSuccess {
                    _profileUiState.value = _profileUiState.value.copy(profile = updatedProfile)
                }
                .onFailure { throwable ->
                    _profileUiState.value = _profileUiState.value.copy(
                        errorMessage = "Failed to update privacy: ${throwable.localizedMessage}"
                    )
                }
        }
    }

    fun setSettingsDialogOpen(open: Boolean) {
        _profileUiState.value = _profileUiState.value.copy(isSettingsDialogOpen = open)
    }
}
