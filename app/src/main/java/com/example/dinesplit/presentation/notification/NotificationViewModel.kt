package com.example.dinesplit.presentation.notification

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.FeedNotificationTrigger
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationFactory
import com.example.dinesplit.domain.model.PersonalNotificationTrigger
import com.example.dinesplit.domain.model.SplitNotificationTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppContainer.notificationRepository(application)

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.observeNotifications()
                .catch { throwable ->
                    _notifications.value = emptyList()
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            currentUserId = currentUserId(),
                            errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                        )
                }
                .collectLatest { notifications ->
                    val orderedNotifications = notifications.orderedNewestFirst()
                    _notifications.value = orderedNotifications
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = null,
                            currentUserId = currentUserId(),
                            unreadCount = orderedNotifications.count { !it.isRead },
                        )
                }
        }
    }

    fun refreshNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            runCatching {
                val notifications = repository.getNotifications().orderedNewestFirst()
                _notifications.value = notifications
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        currentUserId = currentUserId(),
                        unreadCount = notifications.count { !it.isRead },
                    )
            }.onFailure { throwable ->
                _notifications.value = emptyList()
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        currentUserId = currentUserId(),
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    fun markAsRead(notificationId: String) {
        updateNotificationReadState(notificationId = notificationId, isRead = true)
    }

    fun markAsUnread(notificationId: String) {
        updateNotificationReadState(notificationId = notificationId, isRead = false)
    }

    fun markAllAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val unreadNotifications = _notifications.value.filter { !it.isRead }
                unreadNotifications.forEach { notification ->
                    repository.markAsRead(notification.id)
                }
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }.orderedNewestFirst()
                _uiState.value = _uiState.value.copy(unreadCount = 0)
            }.onFailure { throwable ->
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    fun onFeedTrigger(trigger: FeedNotificationTrigger) {
        insertGeneratedNotification(
            notification = NotificationFactory.fromFeedTrigger(trigger, currentUserId()),
        )
    }

    fun onSplitTrigger(trigger: SplitNotificationTrigger) {
        insertGeneratedNotification(
            notification = NotificationFactory.fromSplitTrigger(trigger, currentUserId()),
        )
    }

    fun onPersonalTrigger(trigger: PersonalNotificationTrigger) {
        insertGeneratedNotification(
            notification = NotificationFactory.fromPersonalTrigger(trigger, currentUserId()),
        )
    }

    private fun updateNotificationReadState(
        notificationId: String,
        isRead: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (isRead) {
                    repository.markAsRead(notificationId)
                } else {
                    repository.markAsUnread(notificationId)
                }
                _notifications.value =
                    _notifications.value.map { notification ->
                        if (notification.id == notificationId) {
                            notification.copy(isRead = isRead)
                        } else {
                            notification
                        }
                    }.orderedNewestFirst()
                _uiState.value =
                    _uiState.value.copy(
                        unreadCount = _notifications.value.count { !it.isRead },
                    )
            }.onFailure { throwable ->
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    private fun insertGeneratedNotification(notification: Notification) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.insertNotification(notification)
                _notifications.value = (listOf(notification) + _notifications.value).orderedNewestFirst()
                _uiState.value =
                    _uiState.value.copy(
                        unreadCount = _notifications.value.count { !it.isRead },
                    )
            }.onFailure { throwable ->
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    private fun currentUserId(): String {
        return FirebaseProviders.auth.currentUser?.uid.orEmpty()
    }

    private fun List<Notification>.orderedNewestFirst(): List<Notification> {
        return sortedWith(
            compareByDescending<Notification> { it.createdAt }
                .thenByDescending { it.updatedAt }
                .thenByDescending { it.id }
        )
    }
}

data class NotificationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: String = "",
    val unreadCount: Int = 0,
)
