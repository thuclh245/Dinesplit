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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
)

sealed interface LoginUiEffect {
    data class NavigateToResolved(val destination: com.example.dinesplit.domain.model.AppStartDestination) : LoginUiEffect
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val loginUseCase = AppContainer.loginUseCase(application)
    private val resolveStartDestinationUseCase = AppContainer.resolveStartDestinationUseCase(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LoginUiEffect>()
    val effect: SharedFlow<LoginUiEffect> = _effect.asSharedFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, emailError = null, submitError = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, submitError = null)
    }

    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val emailError = AuthInputValidator.validateEmail(current.email)
        val passwordError = AuthInputValidator.validatePasswordForLogin(current.password)
        if (emailError != null || passwordError != null) {
            _uiState.value = current.copy(emailError = emailError, passwordError = passwordError)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            loginUseCase(current.email.trim(), current.password)
                .onSuccess {
                    try {
                        val destination = resolveStartDestinationUseCase()
                        _uiState.value = _uiState.value.copy(isSubmitting = false)
                        _effect.emit(LoginUiEffect.NavigateToResolved(destination))
                    } catch (e: Exception) {
                        android.util.Log.e("LoginViewModel", "Error resolving destination", e)
                        _uiState.value =
                            _uiState.value.copy(
                                isSubmitting = false,
                                submitError = "Successfully logged in, but couldn't load profile. Please check your internet connection.",
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
}
