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
    val progress: Float = 0f,
    val destination: AppStartDestination? = null,
    val errorMessage: String? = null,
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
            // Khởi động tải trước (Prefetch) dữ liệu song song để lưu vào bộ nhớ đệm Firestore
            viewModelScope.launch {
                runCatching {
                    val feedRepo = AppContainer.feedRepository()
                    val profileRepo = AppContainer.profileRepository(getApplication())

                    // 1. Tải trước 10 bài đăng đầu tiên cho FeedScreen
                    feedRepo.getFeedPostsBatch(limit = 10, lastPostId = null)

                    // 2. Tải trước 100 bài đăng để làm ấm bộ đệm cho thống kê Trending Places trong Search
                    feedRepo.getFeedPostsBatch(limit = 100, lastPostId = null)

                    // 3. Tải trước danh sách người dùng gợi ý
                    profileRepo.searchProfiles("", limit = 10)
                }
            }

            // Smoothly animate progress from 0 to 1.0 (100%)
            val duration = 2000L // 2 seconds
            val steps = 50
            val delayPerStep = duration / steps

            for (i in 1..steps) {
                kotlinx.coroutines.delay(delayPerStep)
                _uiState.value = _uiState.value.copy(progress = i.toFloat() / steps)
            }

            runCatching {
                resolveStartDestinationUseCase()
            }.onSuccess { destination ->
                _uiState.value = _uiState.value.copy(isLoading = false, destination = destination)
            }.onFailure { throwable ->
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        destination = AppStartDestination.AUTH,
                        errorMessage = throwable.message ?: "Unable to restore session",
                    )
            }
        }
    }
}
