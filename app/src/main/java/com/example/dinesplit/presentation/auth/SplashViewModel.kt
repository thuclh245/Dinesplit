package com.example.dinesplit.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.AppStartDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SplashUiState(
    val isLoading: Boolean = true,
    val destination: AppStartDestination? = null,
    val errorMessage: String? = null
)

class SplashViewModel(application: Application) : AndroidViewModel(application) {
    private val resolveStartDestinationUseCase = AppContainer.resolveStartDestinationUseCase(application)

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        resolveDestination()
    }

    fun resolveDestination() {
        viewModelScope.launch {
            runCatching {
                resolveStartDestinationUseCase()
            }.onSuccess { destination ->
                _uiState.value = SplashUiState(isLoading = false, destination = destination)
            }.onFailure { throwable ->
                _uiState.value = SplashUiState(
                    isLoading = false,
                    destination = AppStartDestination.AUTH,
                    errorMessage = throwable.message ?: "Unable to restore session"
                )
            }
        }
    }
}

