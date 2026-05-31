package com.example.dinesplit.presentation.auth

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.data.seeder.DemoDataSeeder
import com.example.dinesplit.domain.exception.UsernameAlreadyExistsException
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.validation.ProfileInputValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class CompleteProfileUiState(
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

sealed interface CompleteProfileUiEffect {
    data object NavigateToMain : CompleteProfileUiEffect
}

class CompleteProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val updateProfileUseCase = AppContainer.updateProfileUseCase(application)
    private val uploadAvatarUseCase = AppContainer.uploadAvatarUseCase(application)

    private val _uiState = MutableStateFlow(CompleteProfileUiState())
    val uiState: StateFlow<CompleteProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CompleteProfileUiEffect>()
    val effect: SharedFlow<CompleteProfileUiEffect> = _effect.asSharedFlow()

    fun onDisplayNameChange(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value, displayNameError = null, submitError = null)
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, usernameError = null, submitError = null)
    }

    fun onBioChange(value: String) {
        _uiState.value = _uiState.value.copy(bio = value, submitError = null)
    }

    fun onAvatarSelected(value: Uri) {
        _uiState.value =
            _uiState.value.copy(
                avatarUrl = value.toString(),
                avatarLocalUri = value.toString(),
                avatarError = null,
                submitError = null,
            )
    }

    fun onAvatarCleared() {
        _uiState.value =
            _uiState.value.copy(
                avatarUrl = "",
                avatarLocalUri = null,
                avatarError = null,
                submitError = null,
            )
    }

    fun toggleDiningStyle(style: String) {
        val currentSelected = _uiState.value.selectedStyles
        val updated =
            if (currentSelected.contains(style)) {
                currentSelected - style
            } else {
                currentSelected + style
            }
        _uiState.value = _uiState.value.copy(selectedStyles = updated)
    }

    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val displayNameError = ProfileInputValidator.validateDisplayName(current.displayName)
        val usernameError = ProfileInputValidator.validateUsername(current.username)
        if (displayNameError != null || usernameError != null) {
            _uiState.value = current.copy(displayNameError = displayNameError, usernameError = usernameError)
            return
        }

        val session = observeSessionUseCase().value
        if (session == null) {
            _uiState.value = current.copy(submitError = "Session expired. Please sign in again.")
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSubmitting = true,
                    isAvatarUploading = false,
                    submitError = null,
                    avatarError = null,
                )

            val now = System.currentTimeMillis()
            val finalAvatarUrl =
                uploadAvatarIfNeeded(session.uid, current.avatarLocalUri)
                    ?: return@launch

            val profile =
                UserProfile(
                    uid = session.uid,
                    displayName = current.displayName.trim(),
                    username = current.username.trim(),
                    email = session.email,
                    avatarUrl = finalAvatarUrl,
                    bio = current.bio.trim(),
                    diningStyles = current.selectedStyles,
                    createdAt = Date(now),
                    updatedAt = Date(now),
                )

            updateProfileUseCase(profile)
                .onSuccess {
                    // Auto-seed demo data on complete profile so feed/split/personal is populated immediately!
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val personalRepo = AppContainer.personalRepository(getApplication())
                            val notificationRepo = AppContainer.notificationRepository(getApplication())
                            val feedRepo = AppContainer.feedRepository()
                            val splitRepo = AppContainer.splitRepository()

                            DemoDataSeeder.seedDemoTransactions(personalRepo, profile.uid)
                            DemoDataSeeder.seedDemoNotifications(notificationRepo, profile.uid)
                            DemoDataSeeder.seedDemoSplit(splitRepo, profile.uid)
                            DemoDataSeeder.seedDemoPosts(feedRepo, profile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            isAvatarUploading = false,
                            avatarUrl = finalAvatarUrl,
                            avatarLocalUri = null,
                        )
                    _effect.emit(CompleteProfileUiEffect.NavigateToMain)
                }
                .onFailure { throwable ->
                    _uiState.value =
                        if (throwable is UsernameAlreadyExistsException) {
                            _uiState.value.copy(
                                isSubmitting = false,
                                isAvatarUploading = false,
                                usernameError = FirebaseErrorMapper.toUserMessage(throwable),
                                submitError = null,
                            )
                        } else {
                            _uiState.value.copy(
                                isSubmitting = false,
                                isAvatarUploading = false,
                                submitError = "Không thể lưu hồ sơ: ${FirebaseErrorMapper.toUserMessage(throwable)}",
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
            return _uiState.value.avatarUrl.trim()
        }

        _uiState.value = _uiState.value.copy(isAvatarUploading = true)
        return uploadAvatarUseCase(uid, Uri.parse(avatarLocalUri))
            .onSuccess { uploadedAvatarUrl ->
                _uiState.value =
                    _uiState.value.copy(
                        avatarUrl = uploadedAvatarUrl,
                        avatarLocalUri = null,
                        isAvatarUploading = false,
                    )
            }
            .onFailure { throwable ->
                _uiState.value =
                    _uiState.value.copy(
                        isSubmitting = false,
                        isAvatarUploading = false,
                        avatarError = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
            .getOrNull()
    }
}
