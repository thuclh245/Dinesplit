package com.example.dinesplit.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.domain.validation.AuthInputValidator
import com.example.dinesplit.domain.validation.ProfileInputValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val isTermsAccepted: Boolean = false,
)

sealed interface RegisterUiEffect {
    data class NavigateToCompleteProfile(val displayName: String) : RegisterUiEffect
    data class NavigateToResolved(val destination: com.example.dinesplit.domain.model.AppStartDestination) : RegisterUiEffect
}

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val registerUseCase = AppContainer.registerUseCase(application)
    private val authRepository = AppContainer.authRepository(application)
    private val resolveStartDestinationUseCase = AppContainer.resolveStartDestinationUseCase(application)

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<RegisterUiEffect>()
    val effect: SharedFlow<RegisterUiEffect> = _effect.asSharedFlow()

    fun onDisplayNameChange(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value, displayNameError = null, submitError = null)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, emailError = null, submitError = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value =
            _uiState.value.copy(
                password = value,
                passwordError = null,
                confirmPasswordError = null,
                submitError = null,
            )
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, confirmPasswordError = null, submitError = null)
    }

    fun onTermsAcceptedChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(isTermsAccepted = value, submitError = null)
    }

    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val displayNameError = ProfileInputValidator.validateDisplayName(current.displayName)
        val emailError = AuthInputValidator.validateEmail(current.email)
        val passwordError = AuthInputValidator.validatePasswordForRegister(current.password)
        val confirmError = AuthInputValidator.validateConfirmPassword(current.password, current.confirmPassword)

        if (displayNameError != null || emailError != null || passwordError != null || confirmError != null) {
            _uiState.value =
                current.copy(
                    displayNameError = displayNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                )
            return
        }

        if (!current.isTermsAccepted) {
            _uiState.value =
                current.copy(
                    submitError = "Bạn phải đồng ý với Điều khoản dịch vụ và Chính sách quyền riêng tư để tiếp tục.",
                )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            registerUseCase(current.email.trim(), current.password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    _effect.emit(RegisterUiEffect.NavigateToCompleteProfile(current.displayName))
                }
                .onFailure { throwable ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            submitError = FirebaseErrorMapper.toUserMessage(throwable),
                        )
                }
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            authRepository.loginWithGoogle(idToken)
                .onSuccess {
                    try {
                        val destination = resolveStartDestinationUseCase()
                        _uiState.value = _uiState.value.copy(isSubmitting = false)
                        _effect.emit(RegisterUiEffect.NavigateToResolved(destination))
                    } catch (e: Exception) {
                        android.util.Log.e("RegisterViewModel", "Error resolving destination", e)
                        _uiState.value =
                            _uiState.value.copy(
                                isSubmitting = false,
                                submitError = "Đăng nhập thành công nhưng không thể tải thông tin cá nhân. Vui lòng kiểm tra kết nối internet.",
                            )
                    }
                }
                .onFailure { throwable ->
                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            submitError = FirebaseErrorMapper.toUserMessage(throwable),
                        )
                }
        }
    }

    fun onGoogleSignInError(message: String) {
        _uiState.value = _uiState.value.copy(isSubmitting = false, submitError = message)
    }
}

