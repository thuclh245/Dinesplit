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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var observeJob: Job? = null
    private var lastObservedUserId: String? = null

    init {
        setupAuthStateListener()
        handleAuthUserChanged(currentUserId().takeIf { it.isNotBlank() })
    }

    private fun setupAuthStateListener() {
        authStateListener =
            FirebaseAuth.AuthStateListener { auth ->
                handleAuthUserChanged(auth.currentUser?.uid)
            }
        FirebaseProviders.auth.addAuthStateListener(authStateListener!!)
    }

    private fun handleAuthUserChanged(userId: String?) {
        if (userId.isNullOrBlank()) {
            observeJob?.cancel()
            lastObservedUserId = null
            clearNotificationState()
            return
        }

        if (userId != lastObservedUserId || _uiState.value.currentUserId != userId) {
            observeJob?.cancel()
            lastObservedUserId = userId
            clearNotificationState(currentUserId = userId, isLoading = true)
            observeNotifications(expectedUserId = userId)
        }
    }

    private fun clearNotificationState(
        currentUserId: String = "",
        isLoading: Boolean = false,
    ) {
        _notifications.value = emptyList()
        _uiState.value =
            NotificationUiState(
                isLoading = isLoading,
                currentUserId = currentUserId,
            )
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { listener ->
            FirebaseProviders.auth.removeAuthStateListener(listener)
        }
        observeJob?.cancel()
    }

    private fun observeNotifications(expectedUserId: String) {
        observeJob =
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = true,
                        errorMessage = null,
                        currentUserId = expectedUserId,
                    )
                repository.observeNotifications()
                    .catch { throwable ->
                        if (!isCurrentUser(expectedUserId)) return@catch
                        _notifications.value = emptyList()
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                currentUserId = expectedUserId,
                                errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                            )
                    }
                    .collectLatest { notifications ->
                        if (!isCurrentUser(expectedUserId)) return@collectLatest
                        val orderedNotifications =
                            notifications
                                .filter { notification -> notification.userId == expectedUserId }
                                .orderedNewestFirst()
                        _notifications.value = orderedNotifications
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                errorMessage = null,
                                currentUserId = expectedUserId,
                                unreadCount = orderedNotifications.count { !it.isRead },
                            )
                    }
            }
    }

    fun refreshNotifications() {
        val expectedUserId = currentUserId()
        if (expectedUserId.isBlank()) {
            clearNotificationState()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null,
                    currentUserId = expectedUserId,
                )

            runCatching {
                val notifications =
                    repository.getNotifications()
                        .filter { notification -> notification.userId == expectedUserId }
                        .orderedNewestFirst()
                if (!isCurrentUser(expectedUserId)) return@runCatching

                _notifications.value = notifications
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        currentUserId = expectedUserId,
                        unreadCount = notifications.count { !it.isRead },
                    )
            }.onFailure { throwable ->
                if (!isCurrentUser(expectedUserId)) return@onFailure
                _notifications.value = emptyList()
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        currentUserId = expectedUserId,
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
        val expectedUserId = currentUserId()
        if (expectedUserId.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (!isCurrentUser(expectedUserId)) return@runCatching

                val unreadNotifications =
                    _notifications.value.filter { notification ->
                        !notification.isRead && notification.userId == expectedUserId
                    }
                unreadNotifications.forEach { notification ->
                    if (!isCurrentUser(expectedUserId)) return@runCatching
                    repository.markAsRead(notification.id)
                }
                if (!isCurrentUser(expectedUserId)) return@runCatching

                _notifications.value =
                    _notifications.value.map { notification ->
                        if (notification.userId == expectedUserId) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }.orderedNewestFirst()
                _uiState.value = _uiState.value.copy(unreadCount = 0)
            }.onFailure { throwable ->
                if (!isCurrentUser(expectedUserId)) return@onFailure
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    fun onFeedTrigger(trigger: FeedNotificationTrigger) {
        val userId = currentUserId()
        if (userId.isBlank()) return

        insertGeneratedNotification(
            notification = NotificationFactory.fromFeedTrigger(trigger, userId),
        )
    }

    fun onSplitTrigger(trigger: SplitNotificationTrigger) {
        val userId = currentUserId()
        if (userId.isBlank()) return

        insertGeneratedNotification(
            notification = NotificationFactory.fromSplitTrigger(trigger, userId),
        )
    }

    fun onPersonalTrigger(trigger: PersonalNotificationTrigger) {
        val userId = currentUserId()
        if (userId.isBlank()) return

        insertGeneratedNotification(
            notification = NotificationFactory.fromPersonalTrigger(trigger, userId),
        )
    }

    private fun updateNotificationReadState(
        notificationId: String,
        isRead: Boolean,
    ) {
        val expectedUserId = currentUserId()
        if (expectedUserId.isBlank()) return
        if (_notifications.value.none { it.id == notificationId && it.userId == expectedUserId }) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (!isCurrentUser(expectedUserId)) return@runCatching

                if (isRead) {
                    repository.markAsRead(notificationId)
                } else {
                    repository.markAsUnread(notificationId)
                }
                if (!isCurrentUser(expectedUserId)) return@runCatching

                _notifications.value =
                    _notifications.value.map { notification ->
                        if (notification.id == notificationId && notification.userId == expectedUserId) {
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
                if (!isCurrentUser(expectedUserId)) return@onFailure
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    private fun insertGeneratedNotification(notification: Notification) {
        val expectedUserId = notification.userId.ifBlank { currentUserId() }
        if (expectedUserId.isBlank() || !isCurrentUser(expectedUserId)) return
        val normalizedNotification =
            if (notification.userId.isBlank()) {
                notification.copy(userId = expectedUserId)
            } else {
                notification
            }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (!isCurrentUser(expectedUserId)) return@runCatching

                repository.insertNotification(normalizedNotification)
                if (!isCurrentUser(expectedUserId)) return@runCatching

                _notifications.value = (listOf(normalizedNotification) + _notifications.value).orderedNewestFirst()
                _uiState.value =
                    _uiState.value.copy(
                        unreadCount = _notifications.value.count { !it.isRead },
                    )
            }.onFailure { throwable ->
                if (!isCurrentUser(expectedUserId)) return@onFailure
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

    private fun isCurrentUser(expectedUserId: String): Boolean {
        return expectedUserId.isNotBlank() && currentUserId() == expectedUserId
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
