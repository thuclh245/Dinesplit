package com.example.dinesplit.presentation.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseErrorMapper
import com.example.dinesplit.domain.model.ChatCallSession
import com.example.dinesplit.domain.model.ChatCallStatus
import com.example.dinesplit.domain.model.ChatMessage
import com.example.dinesplit.domain.model.ChatMessageType
import com.example.dinesplit.domain.model.ChatThread
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ChatRepository
import com.example.dinesplit.domain.repository.ProfileRepository
import com.example.dinesplit.domain.usecase.GetCurrentUserProfileUseCase
import com.example.dinesplit.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListUiState(
    val currentUserId: String = "",
    val threads: List<ChatThread> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

data class ChatDetailUiState(
    val currentUser: UserProfile? = null,
    val otherUser: UserProfile? = null,
    val thread: ChatThread? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
) {
    val currentUserId: String
        get() = currentUser?.uid.orEmpty()

    val threadId: String
        get() = thread?.id.orEmpty()

    val isOtherTyping: Boolean
        get() {
            val otherUid = otherUser?.uid ?: return false
            return thread?.typing?.get(otherUid) == true
        }

    val activeCallId: String
        get() = thread?.activeCallId.orEmpty()
}

data class ChatCallUiState(
    val currentUserId: String = "",
    val call: ChatCallSession? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val hasMicrophonePermission: Boolean = false,
    val audioState: VoiceCallAudioState = VoiceCallAudioState(),
) {
    val isIncoming: Boolean
        get() = call?.calleeId == currentUserId && call.status == ChatCallStatus.RINGING

    val isConnected: Boolean
        get() = call?.status == ChatCallStatus.ACCEPTED

    val isFinished: Boolean
        get() = call?.status in setOf(ChatCallStatus.DECLINED, ChatCallStatus.ENDED, ChatCallStatus.MISSED)

    val isMuted: Boolean
        get() = call?.mutedBy?.get(currentUserId) == true

    val canUseAudio: Boolean
        get() = hasMicrophonePermission && !isFinished
}

sealed interface ChatEffect {
    data class OpenCall(
        val threadId: String,
        val callId: String,
    ) : ChatEffect
}

class ChatListViewModel(application: Application) : AndroidViewModel(application) {
    private val chatRepository: ChatRepository = AppContainer.chatRepository(application)
    private val observeSessionUseCase: ObserveSessionUseCase = AppContainer.observeSessionUseCase(application)

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private var threadsJob: Job? = null

    init {
        viewModelScope.launch {
            observeSessionUseCase().collectLatest { session ->
                val uid = session?.uid.orEmpty()
                threadsJob?.cancel()
                if (uid.isBlank()) {
                    _uiState.value = ChatListUiState(isLoading = false, errorMessage = "Session expired")
                    return@collectLatest
                }
                observeThreads(uid)
            }
        }
    }

    fun retry() {
        val uid = _uiState.value.currentUserId
        if (uid.isNotBlank()) observeThreads(uid)
    }

    private fun observeThreads(uid: String) {
        threadsJob?.cancel()
        _uiState.value = ChatListUiState(currentUserId = uid, isLoading = true)
        threadsJob =
            viewModelScope.launch {
                runCatching {
                    chatRepository.observeThreads(uid).collect { threads ->
                        _uiState.value =
                            ChatListUiState(
                                currentUserId = uid,
                                threads = threads,
                                isLoading = false,
                            )
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.value =
                        ChatListUiState(
                            currentUserId = uid,
                            isLoading = false,
                            errorMessage = FirebaseErrorMapper.toUserMessage(error),
                        )
                }
            }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatListViewModel(application) as T
        }
    }
}

class ChatDetailViewModel(
    application: Application,
    private val otherUserId: String,
) : AndroidViewModel(application) {
    private val chatRepository: ChatRepository = AppContainer.chatRepository(application)
    private val profileRepository: ProfileRepository = AppContainer.profileRepository(application)
    private val observeSessionUseCase: ObserveSessionUseCase = AppContainer.observeSessionUseCase(application)
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase = AppContainer.getCurrentUserProfileUseCase(application)

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ChatEffect>()
    val effect: SharedFlow<ChatEffect> = _effect.asSharedFlow()

    private var threadJob: Job? = null
    private var messagesJob: Job? = null
    private var typingJob: Job? = null

    init {
        load()
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
        val threadId = _uiState.value.threadId
        val currentUserId = _uiState.value.currentUserId
        if (threadId.isBlank() || currentUserId.isBlank()) return

        typingJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.setTyping(threadId, currentUserId, value.isNotBlank()) }
        }
        typingJob =
            viewModelScope.launch(Dispatchers.IO) {
                delay(1600)
                runCatching { chatRepository.setTyping(threadId, currentUserId, false) }
            }
    }

    fun sendText() {
        val state = _uiState.value
        val thread = state.thread ?: return
        val sender = state.currentUser ?: return
        val text = state.input.trim()
        if (text.isBlank()) return

        _uiState.update { it.copy(input = "", isSending = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                chatRepository.sendText(thread, sender, text)
                chatRepository.setTyping(thread.id, sender.uid, false)
            }.onFailure { throwable ->
                _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) }
            }
            _uiState.update { it.copy(isSending = false) }
        }
    }

    fun sendSticker(sticker: String) {
        val state = _uiState.value
        val thread = state.thread ?: return
        val sender = state.currentUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.sendSticker(thread, sender, sticker) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    fun sendMedia(
        uri: Uri,
        type: String,
    ) {
        val state = _uiState.value
        val thread = state.thread ?: return
        val sender = state.currentUser ?: return
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.sendMedia(thread, sender, uri, type) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
            _uiState.update { it.copy(isSending = false) }
        }
    }

    fun toggleReaction(
        message: ChatMessage,
        reaction: String,
    ) {
        val state = _uiState.value
        val uid = state.currentUserId
        val nextReaction = if (message.reactions[uid] == reaction) null else reaction
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.setReaction(state.threadId, message.id, uid, nextReaction) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    fun recallMessage(message: ChatMessage) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.recallMessage(state.threadId, message.id, state.currentUserId) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    fun deleteMessageForMe(message: ChatMessage) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.deleteMessageForMe(state.threadId, message.id, state.currentUserId) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    fun startVoiceCall() {
        val state = _uiState.value
        val thread = state.thread ?: return
        val caller = state.currentUser ?: return
        val callee = state.otherUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.startVoiceCall(thread, caller, callee) }
                .onSuccess { call -> _effect.emit(ChatEffect.OpenCall(thread.id, call.id)) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    fun openActiveCall() {
        val state = _uiState.value
        val callId = state.activeCallId
        val threadId = state.threadId
        if (threadId.isBlank() || callId.isBlank()) return
        viewModelScope.launch { _effect.emit(ChatEffect.OpenCall(threadId, callId)) }
    }

    private fun load() {
        viewModelScope.launch {
            val session = observeSessionUseCase().value
            val uid = session?.uid.orEmpty()
            if (uid.isBlank() || otherUserId.isBlank()) {
                _uiState.value = ChatDetailUiState(isLoading = false, errorMessage = "Session expired")
                return@launch
            }

            runCatching {
                val currentUserDeferred = async(Dispatchers.IO) { getCurrentUserProfileUseCase(uid) }
                val otherUserDeferred = async(Dispatchers.IO) { profileRepository.getProfile(otherUserId) }
                val currentUser = currentUserDeferred.await() ?: error("Profile not found")
                val otherUser = otherUserDeferred.await() ?: error("User not found")
                val thread = chatRepository.getOrCreateThread(currentUser, otherUser)

                _uiState.value =
                    ChatDetailUiState(
                        currentUser = currentUser,
                        otherUser = otherUser,
                        thread = thread,
                        isLoading = false,
                    )

                threadJob?.cancel()
                threadJob =
                    launch {
                        runCatching {
                            chatRepository.observeThread(thread.id).collect { freshThread ->
                                if (freshThread != null) {
                                    _uiState.update { it.copy(thread = freshThread) }
                                }
                            }
                        }.onFailure { throwable ->
                            if (throwable is CancellationException) throw throwable
                            _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) }
                        }
                    }

                messagesJob?.cancel()
                messagesJob =
                    launch {
                        runCatching {
                            chatRepository.observeMessages(thread.id, currentUser.uid).collect { messages ->
                                _uiState.update { it.copy(messages = messages) }
                                launch(Dispatchers.IO) {
                                    runCatching { chatRepository.markThreadRead(thread.id, currentUser.uid) }
                                }
                            }
                        }.onFailure { throwable ->
                            if (throwable is CancellationException) throw throwable
                            _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) }
                        }
                    }
            }.onFailure { throwable ->
                _uiState.value =
                    ChatDetailUiState(
                        isLoading = false,
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
            }
        }
    }

    override fun onCleared() {
        val state = _uiState.value
        val threadId = state.threadId
        val uid = state.currentUserId
        if (threadId.isNotBlank() && uid.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { chatRepository.setTyping(threadId, uid, false) }
            }
        }
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val otherUserId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatDetailViewModel(application, otherUserId) as T
        }
    }
}

class ChatCallViewModel(
    application: Application,
    private val threadId: String,
    private val callId: String,
) : AndroidViewModel(application) {
    private val chatRepository: ChatRepository = AppContainer.chatRepository(application)
    private val observeSessionUseCase: ObserveSessionUseCase = AppContainer.observeSessionUseCase(application)

    private val _uiState = MutableStateFlow(ChatCallUiState())
    val uiState: StateFlow<ChatCallUiState> = _uiState.asStateFlow()
    private val voiceCallClient =
        WebRtcVoiceCallClient(
            context = application,
            repository = chatRepository,
            scope = viewModelScope,
        ) { audioState ->
            _uiState.update { it.copy(audioState = audioState) }
        }

    init {
        viewModelScope.launch {
            val uid = observeSessionUseCase().value?.uid.orEmpty()
            _uiState.update { it.copy(currentUserId = uid) }
            runCatching {
                chatRepository.observeCall(threadId, callId).collect { call ->
                    _uiState.update {
                        it.copy(
                            call = call,
                            isLoading = false,
                        )
                    }
                    if (call != null) {
                        maybeStartAudio(call)
                    }
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = FirebaseErrorMapper.toUserMessage(throwable),
                    )
                }
            }
        }
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasMicrophonePermission = granted,
                audioState =
                    if (granted) {
                        it.audioState
                    } else {
                        VoiceCallAudioState(errorMessage = "Bạn cần cấp quyền micro để gọi thoại")
                    },
            )
        }
        if (granted) {
            _uiState.value.call?.let { maybeStartAudio(it) }
        }
    }

    fun accept() {
        val uid = _uiState.value.currentUserId
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.acceptCall(threadId, callId, uid) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    fun decline() {
        val uid = _uiState.value.currentUserId
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.declineCall(threadId, callId, uid) }
                .onSuccess { voiceCallClient.close() }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    fun end() {
        val uid = _uiState.value.currentUserId
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.endCall(threadId, callId, uid) }
                .onSuccess { voiceCallClient.close() }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    fun toggleMute() {
        val uid = _uiState.value.currentUserId
        val muted = !_uiState.value.isMuted
        voiceCallClient.setMuted(muted)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.setCallMuted(threadId, callId, uid, muted) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    private fun maybeStartAudio(call: ChatCallSession) {
        val state = _uiState.value
        if (!state.hasMicrophonePermission || state.currentUserId.isBlank()) return
        voiceCallClient.handleCall(call, state.currentUserId)
        voiceCallClient.setMuted(call.mutedBy[state.currentUserId] == true)
    }

    override fun onCleared() {
        voiceCallClient.close()
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val threadId: String,
        private val callId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatCallViewModel(application, threadId, callId) as T
        }
    }
}

val ChatStickerSet = listOf("🍜", "🍕", "🍣", "🥗", "☕", "🔥", "❤️", "😂", "👍", "🙏")
val ChatReactionSet = listOf("❤️", "😂", "😮", "😢", "👍", "🔥")
