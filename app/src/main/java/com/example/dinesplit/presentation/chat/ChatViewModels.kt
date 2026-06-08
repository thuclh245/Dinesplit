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

/**
 * State cho màn hình danh sách chat.
 *
 * @property currentUserId UID người dùng đang xem danh sách thread.
 * @property threads Danh sách thread realtime.
 * @property isLoading true khi đang chờ session hoặc dữ liệu thread đầu tiên.
 * @property errorMessage Lỗi hiển thị cho người dùng nếu observe thất bại.
 */
data class ChatListUiState(
    val currentUserId: String = "",
    val threads: List<ChatThread> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * State cho màn hình chi tiết một cuộc trò chuyện.
 *
 * @property currentUser Hồ sơ người dùng hiện tại.
 * @property otherUser Hồ sơ người đang chat cùng.
 * @property thread Metadata thread hiện tại.
 * @property messages Danh sách message đã lọc theo người dùng.
 * @property input Nội dung đang nhập trong ô chat.
 * @property isLoading true khi đang tạo/lấy thread và tải hồ sơ.
 * @property isSending true khi đang gửi text/media.
 * @property errorMessage Lỗi thao tác gần nhất.
 */
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
    /**
     * UID người dùng hiện tại, trả về rỗng nếu profile chưa tải xong.
     */
    val currentUserId: String
        get() = currentUser?.uid.orEmpty()

    /**
     * ID thread hiện tại, trả về rỗng nếu thread chưa sẵn sàng.
     */
    val threadId: String
        get() = thread?.id.orEmpty()

    /**
     * true khi người còn lại đang gõ trong thread.
     */
    val isOtherTyping: Boolean
        get() {
            val otherUid = otherUser?.uid ?: return false
            return thread?.typing?.get(otherUid) == true
        }

    /**
     * ID cuộc gọi đang hoạt động trong thread nếu có.
     */
    val activeCallId: String
        get() = thread?.activeCallId.orEmpty()
}

/**
 * State cho màn hình gọi thoại.
 *
 * @property currentUserId UID participant hiện tại.
 * @property call Phiên gọi đang observe.
 * @property isLoading true khi đang chờ document call đầu tiên.
 * @property errorMessage Lỗi call/audio gần nhất.
 * @property hasMicrophonePermission true nếu Android runtime permission đã được cấp.
 * @property audioState Trạng thái kết nối audio WebRTC.
 */
data class ChatCallUiState(
    val currentUserId: String = "",
    val call: ChatCallSession? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val hasMicrophonePermission: Boolean = false,
    val audioState: VoiceCallAudioState = VoiceCallAudioState(),
) {
    /**
     * true khi đây là cuộc gọi đến đang ringing với người hiện tại là callee.
     */
    val isIncoming: Boolean
        get() = call?.calleeId == currentUserId && call.status == ChatCallStatus.RINGING

    /**
     * true khi cuộc gọi đã được accepted.
     */
    val isConnected: Boolean
        get() = call?.status == ChatCallStatus.ACCEPTED

    /**
     * true khi cuộc gọi đã từ chối/kết thúc/missed.
     */
    val isFinished: Boolean
        get() = call?.status in setOf(ChatCallStatus.DECLINED, ChatCallStatus.ENDED, ChatCallStatus.MISSED)

    /**
     * true nếu người hiện tại đang tắt micro.
     */
    val isMuted: Boolean
        get() = call?.mutedBy?.get(currentUserId) == true

    /**
     * true nếu UI được phép khởi động audio WebRTC.
     */
    val canUseAudio: Boolean
        get() = hasMicrophonePermission && !isFinished
}

/**
 * Side effect một lần phát từ màn hình chat detail.
 */
sealed interface ChatEffect {
    /**
     * Yêu cầu UI điều hướng sang màn hình cuộc gọi.
     *
     * @property threadId ID thread chứa call.
     * @property callId ID phiên gọi cần mở.
     */
    data class OpenCall(
        val threadId: String,
        val callId: String,
    ) : ChatEffect
}

/**
 * ViewModel cho màn hình danh sách cuộc trò chuyện.
 *
 * Lớp này theo dõi session hiện tại, sau đó observe các thread mà UID đó tham gia.
 */
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

    /**
     * Thử observe lại danh sách thread khi UI yêu cầu retry.
     */
    fun retry() {
        val uid = _uiState.value.currentUserId
        if (uid.isNotBlank()) observeThreads(uid)
    }

    /**
     * Bắt đầu lắng nghe realtime danh sách thread của một UID.
     *
     * @param uid UID người dùng hiện tại.
     */
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

    /**
     * Factory tạo [ChatListViewModel] với [Application] do Compose/Navigation truyền vào.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatListViewModel(application) as T
        }
    }
}

/**
 * ViewModel cho màn hình chi tiết chat một-một.
 *
 * ViewModel tải profile hai phía, lấy/tạo thread, observe metadata và message realtime, đồng thời
 * điều phối gửi text/sticker/media, typing indicator, reaction, recall/delete message và mở cuộc gọi.
 *
 * @property otherUserId UID người dùng còn lại trong cuộc trò chuyện.
 */
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

    /**
     * Cập nhật input và ghi trạng thái typing lên Firestore.
     *
     * Typing được tự tắt sau một khoảng trễ ngắn nếu người dùng ngừng nhập.
     *
     * @param value Nội dung mới trong ô nhập.
     */
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

    /**
     * Gửi nội dung text hiện tại, reset input và tắt typing sau khi gửi.
     */
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

    /**
     * Gửi một sticker vào thread hiện tại.
     *
     * @param sticker Mã/ký hiệu sticker cần gửi.
     */
    fun sendSticker(sticker: String) {
        val state = _uiState.value
        val thread = state.thread ?: return
        val sender = state.currentUser ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.sendSticker(thread, sender, sticker) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    /**
     * Upload và gửi ảnh/video vào thread hiện tại.
     *
     * @param uri Uri cục bộ của media.
     * @param type Loại media theo [ChatMessageType].
     */
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

    /**
     * Bật/tắt reaction của người hiện tại trên một message.
     *
     * @param message Tin nhắn cần cập nhật reaction.
     * @param reaction Reaction được chọn.
     */
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

    /**
     * Thu hồi một tin nhắn nếu người hiện tại là người gửi.
     *
     * @param message Tin nhắn cần thu hồi.
     */
    fun recallMessage(message: ChatMessage) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.recallMessage(state.threadId, message.id, state.currentUserId) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    /**
     * Xóa một tin nhắn chỉ ở phía người hiện tại.
     *
     * @param message Tin nhắn cần xóa khỏi view của mình.
     */
    fun deleteMessageForMe(message: ChatMessage) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.deleteMessageForMe(state.threadId, message.id, state.currentUserId) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    /**
     * Khởi tạo cuộc gọi thoại với người còn lại và phát effect mở màn hình call.
     */
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

    /**
     * Mở lại cuộc gọi đang hoạt động nếu thread có `activeCallId`.
     */
    fun openActiveCall() {
        val state = _uiState.value
        val callId = state.activeCallId
        val threadId = state.threadId
        if (threadId.isBlank() || callId.isBlank()) return
        viewModelScope.launch { _effect.emit(ChatEffect.OpenCall(threadId, callId)) }
    }

    /**
     * Tải hồ sơ hai người dùng, lấy/tạo thread và đăng ký listener realtime cho thread/messages.
     */
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

    /**
     * Khi ViewModel bị hủy, tắt typing để tránh giữ trạng thái đang gõ sai trên Firestore.
     */
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

    /**
     * Factory tạo [ChatDetailViewModel] với UID người dùng còn lại.
     */
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

/**
 * ViewModel điều phối màn hình cuộc gọi thoại.
 *
 * Lớp này observe [ChatCallSession], xử lý accept/decline/end/mute và kết nối WebRTC audio
 * thông qua [WebRtcVoiceCallClient] sau khi người dùng cấp quyền micro.
 *
 * @property threadId ID thread chứa cuộc gọi.
 * @property callId ID phiên gọi cần observe.
 */
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

    /**
     * Nhận kết quả xin quyền micro từ UI và khởi động audio nếu được cấp.
     *
     * @param granted true nếu quyền micro đã được cấp.
     */
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

    /**
     * Chấp nhận cuộc gọi đến.
     */
    fun accept() {
        val uid = _uiState.value.currentUserId
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.acceptCall(threadId, callId, uid) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    /**
     * Từ chối cuộc gọi và đóng tài nguyên WebRTC cục bộ.
     */
    fun decline() {
        val uid = _uiState.value.currentUserId
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.declineCall(threadId, callId, uid) }
                .onSuccess { voiceCallClient.close() }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    /**
     * Kết thúc cuộc gọi đang diễn ra và đóng tài nguyên WebRTC cục bộ.
     */
    fun end() {
        val uid = _uiState.value.currentUserId
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.endCall(threadId, callId, uid) }
                .onSuccess { voiceCallClient.close() }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    /**
     * Bật/tắt mute cục bộ và đồng bộ trạng thái mute lên Firestore.
     */
    fun toggleMute() {
        val uid = _uiState.value.currentUserId
        val muted = !_uiState.value.isMuted
        voiceCallClient.setMuted(muted)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { chatRepository.setCallMuted(threadId, callId, uid, muted) }
                .onFailure { throwable -> _uiState.update { it.copy(errorMessage = FirebaseErrorMapper.toUserMessage(throwable)) } }
        }
    }

    /**
     * Khởi động hoặc cập nhật WebRTC audio nếu đã đủ điều kiện.
     *
     * @param call Phiên gọi mới nhất từ Firestore.
     */
    private fun maybeStartAudio(call: ChatCallSession) {
        val state = _uiState.value
        if (!state.hasMicrophonePermission || state.currentUserId.isBlank()) return
        voiceCallClient.handleCall(call, state.currentUserId)
        voiceCallClient.setMuted(call.mutedBy[state.currentUserId] == true)
    }

    /**
     * Giải phóng WebRTC khi ViewModel call không còn được sử dụng.
     */
    override fun onCleared() {
        voiceCallClient.close()
        super.onCleared()
    }

    /**
     * Factory tạo [ChatCallViewModel] cho một thread/call cụ thể.
     */
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

/**
 * Bộ sticker nhanh hiển thị trong màn hình chat.
 */
val ChatStickerSet = listOf("🍜", "🍕", "🍣", "🥗", "☕", "🔥", "❤️", "😂", "👍", "🙏")

/**
 * Bộ reaction nhanh cho tin nhắn chat.
 */
val ChatReactionSet = listOf("❤️", "😂", "😮", "😢", "👍", "🔥")
