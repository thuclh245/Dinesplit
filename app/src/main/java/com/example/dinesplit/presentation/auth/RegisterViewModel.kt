package com.example.dinesplit.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.domain.validation.AuthInputValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null
)

sealed interface RegisterUiEffect {
    data object NavigateToCompleteProfile : RegisterUiEffect
}

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val registerUseCase = AppContainer.registerUseCase(application)

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<RegisterUiEffect>()
    val effect: SharedFlow<RegisterUiEffect> = _effect.asSharedFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, emailError = null, submitError = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            passwordError = null,
            confirmPasswordError = null,
            submitError = null
        )
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, confirmPasswordError = null, submitError = null)
    }

    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val emailError = AuthInputValidator.validateEmail(current.email)
        val passwordError = AuthInputValidator.validatePasswordForRegister(current.password)
        val confirmError = AuthInputValidator.validateConfirmPassword(current.password, current.confirmPassword)

        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.value = current.copy(
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            registerUseCase(current.email.trim(), current.password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    _effect.emit(RegisterUiEffect.NavigateToCompleteProfile)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitError = FirebaseErrorMapper.toUserMessage(throwable)
                    )
                }
        }
    }
}

