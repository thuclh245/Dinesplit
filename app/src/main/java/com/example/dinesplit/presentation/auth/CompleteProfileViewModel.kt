package com.example.dinesplit.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.validation.ProfileInputValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompleteProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val displayNameError: String? = null,
    val usernameError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null
)

sealed interface CompleteProfileUiEffect {
    data object NavigateToMain : CompleteProfileUiEffect
}

class CompleteProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val observeSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val updateProfileUseCase = AppContainer.updateProfileUseCase(application)

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
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            val now = System.currentTimeMillis()
            val profile = UserProfile(
                uid = session.uid,
                displayName = current.displayName.trim(),
                username = current.username.trim(),
                email = session.email,
                bio = current.bio.trim(),
                createdAt = now,
                updatedAt = now
            )
            updateProfileUseCase(profile)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    _effect.emit(CompleteProfileUiEffect.NavigateToMain)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitError = throwable.message ?: "Unable to save profile"
                    )
                }
        }
    }
}

