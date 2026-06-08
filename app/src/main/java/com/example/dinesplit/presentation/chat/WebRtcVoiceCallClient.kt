package com.example.dinesplit.presentation.chat

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import com.example.dinesplit.domain.model.ChatCallSession
import com.example.dinesplit.domain.model.ChatCallStatus
import com.example.dinesplit.domain.model.ChatIceCandidate
import com.example.dinesplit.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * State tối thiểu của tầng audio WebRTC để UI hiển thị trạng thái cuộc gọi.
 *
 * @property isStarting true khi peer connection/audio track đang được khởi tạo.
 * @property isConnected true khi ICE connection đã kết nối hoặc hoàn tất.
 * @property errorMessage Lỗi audio/signaling cần hiển thị cho người dùng.
 */
data class VoiceCallAudioState(
    val isStarting: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Client quản lý kết nối âm thanh WebRTC cho một phiên gọi thoại DineSplit.
 *
 * Lớp này tạo local audio track, thiết lập PeerConnection, trao đổi SDP offer/answer và ICE
 * candidate qua [ChatRepository], đồng thời đổi AudioManager sang chế độ thoại trong lúc gọi.
 *
 * @param context Context dùng để khởi tạo WebRTC và truy cập AudioManager.
 * @property repository Repository dùng làm signaling channel qua Firestore.
 * @property scope CoroutineScope của ViewModel call.
 * @property onStateChanged Callback phát trạng thái audio mới cho UI.
 */
class WebRtcVoiceCallClient(
    context: Context,
    private val repository: ChatRepository,
    private val scope: CoroutineScope,
    private val onStateChanged: (VoiceCallAudioState) -> Unit,
) {
    private val appContext = context.applicationContext
    private val signalingMutex = Mutex()
    private val remoteCandidatesAdded = mutableSetOf<String>()

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var originalAudioMode: Int? = null
    private var originalCommunicationDevice: AudioDeviceInfo? = null
    private var originalSpeakerphoneOn: Boolean? = null
    private var originalMicrophoneMute: Boolean? = null
    private var currentCallKey: String = ""
    private var currentUserId: String = ""
    private var remoteDescriptionSet = false
    private var localOfferCreated = false
    private var localAnswerCreated = false
    private var closed = false

    /**
     * Xử lý bản ghi [ChatCallSession] mới nhất từ Firestore.
     *
     * Hàm chỉ bắt đầu audio khi user thuộc cuộc gọi và trạng thái cho phép gọi. Nếu cuộc gọi đã
     * kết thúc, client sẽ giải phóng toàn bộ tài nguyên cục bộ.
     *
     * @param call Phiên gọi mới nhất.
     * @param userId UID người dùng hiện tại.
     */
    fun handleCall(
        call: ChatCallSession,
        userId: String,
    ) {
        if (userId.isBlank()) return
        if (call.status in finishedStatuses) {
            close()
            return
        }

        val isCaller = call.callerId == userId
        val canStart = (isCaller && call.status == ChatCallStatus.RINGING) || call.status == ChatCallStatus.ACCEPTED
        if (!canStart) return

        ensurePeerConnection(call, userId)
        scope.launch(Dispatchers.IO) {
            signalingMutex.withLock {
                runCatching { applySignaling(call, userId) }
                    .onFailure { error ->
                        onStateChanged(
                            VoiceCallAudioState(
                                isStarting = false,
                                isConnected = false,
                                errorMessage = error.message ?: "Không thể kết nối âm thanh",
                            ),
                        )
                    }
            }
        }
    }

    /**
     * Bật/tắt local audio track.
     *
     * @param muted true nếu cần tắt micro.
     */
    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
        audioDeviceModule?.setMicrophoneMute(muted)
    }

    /**
     * Đóng hoàn toàn kết nối WebRTC và khôi phục AudioManager về mode ban đầu.
     */
    fun close() {
        if (closed) return
        closed = true
        runCatching { localAudioTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        runCatching { factory?.dispose() }
        runCatching { audioDeviceModule?.release() }
        restoreAudioMode()
        factory = null
        peerConnection = null
        audioDeviceModule = null
        audioSource = null
        localAudioTrack = null
        currentCallKey = ""
        remoteCandidatesAdded.clear()
        remoteDescriptionSet = false
        localOfferCreated = false
        localAnswerCreated = false
        onStateChanged(VoiceCallAudioState())
    }

    /**
     * Đảm bảo PeerConnection cho đúng cuộc gọi đang tồn tại.
     *
     * Nếu chuyển sang call khác, client sẽ cleanup tài nguyên cũ rồi tạo factory, audio source,
     * local audio track và PeerConnection mới.
     *
     * @param call Phiên gọi cần kết nối.
     * @param userId UID người hiện tại.
     */
    private fun ensurePeerConnection(
        call: ChatCallSession,
        userId: String,
    ) {
        val nextCallKey = "${call.threadId}:${call.id}"
        if (peerConnection != null && currentCallKey == nextCallKey) return

        closeForRestart()
        closed = false
        currentCallKey = nextCallKey
        currentUserId = userId
        configureAudioMode()
        initializeFactory()

        val iceServers =
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            )
        val config =
            PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                iceCandidatePoolSize = 2
            }
        val nextPeerConnection =
            factory?.createPeerConnection(config, createObserver(call.threadId, call.id))
                ?: error("Không thể tạo kết nối WebRTC")
        val nextAudioSource = factory?.createAudioSource(MediaConstraints()) ?: error("Không thể mở nguồn âm thanh")
        val nextAudioTrack = factory?.createAudioTrack("audio_$userId", nextAudioSource) ?: error("Không thể tạo audio track")

        nextAudioTrack.setEnabled(true)
        nextPeerConnection.addTrack(nextAudioTrack, listOf("dinesplit_${call.id}"))

        peerConnection = nextPeerConnection
        audioSource = nextAudioSource
        localAudioTrack = nextAudioTrack
        onStateChanged(VoiceCallAudioState(isStarting = true))
    }

    /**
     * Áp dụng quy trình signaling WebRTC dựa trên vai trò caller/callee.
     *
     * Caller tạo offer, lưu lên repository, chờ answer rồi add candidate của callee.
     * Callee nhận offer, tạo answer, lưu lại rồi add candidate của caller.
     *
     * @param call Phiên gọi chứa SDP/candidate hiện tại.
     * @param userId UID người hiện tại.
     */
    private suspend fun applySignaling(
        call: ChatCallSession,
        userId: String,
    ) {
        val connection = peerConnection ?: return
        val isCaller = call.callerId == userId

        if (isCaller) {
            if (call.offerSdp.isBlank() && !localOfferCreated) {
                val offer = connection.createOfferSuspend()
                connection.setLocalDescriptionSuspend(offer)
                localOfferCreated = true
                repository.saveCallOffer(call.threadId, call.id, offer.description)
            }
            if (call.answerSdp.isNotBlank() && !remoteDescriptionSet) {
                connection.setRemoteDescriptionSuspend(
                    SessionDescription(SessionDescription.Type.ANSWER, call.answerSdp),
                )
                remoteDescriptionSet = true
            }
            if (remoteDescriptionSet) addRemoteCandidates(call.calleeCandidates)
        } else {
            if (call.offerSdp.isNotBlank() && !remoteDescriptionSet) {
                connection.setRemoteDescriptionSuspend(
                    SessionDescription(SessionDescription.Type.OFFER, call.offerSdp),
                )
                remoteDescriptionSet = true
            }
            if (remoteDescriptionSet && call.answerSdp.isBlank() && !localAnswerCreated) {
                val answer = connection.createAnswerSuspend()
                connection.setLocalDescriptionSuspend(answer)
                localAnswerCreated = true
                repository.saveCallAnswer(call.threadId, call.id, answer.description)
            }
            if (remoteDescriptionSet) addRemoteCandidates(call.callerCandidates)
        }
    }

    /**
     * Thêm các ICE candidate từ phía remote vào PeerConnection.
     *
     * Candidate đã thêm được lưu key để tránh add trùng khi Firestore phát lại snapshot.
     *
     * @param candidates Danh sách candidate từ caller hoặc callee.
     */
    private fun addRemoteCandidates(candidates: List<ChatIceCandidate>) {
        val connection = peerConnection ?: return
        candidates
            .filter { it.fromUserId != currentUserId && it.candidate.isNotBlank() }
            .forEach { candidate ->
                if (remoteCandidatesAdded.add(candidate.stableKey())) {
                    connection.addIceCandidate(
                        IceCandidate(
                            candidate.sdpMid,
                            candidate.sdpMLineIndex,
                            candidate.candidate,
                        ),
                    )
                }
            }
    }

    /**
     * Tạo observer cho PeerConnection để cập nhật trạng thái ICE và đẩy local candidate lên repository.
     *
     * @param threadId ID thread dùng khi lưu candidate.
     * @param callId ID phiên gọi dùng khi lưu candidate.
     * @return [PeerConnection.Observer] gắn vào PeerConnection.
     */
    private fun createObserver(
        threadId: String,
        callId: String,
    ): PeerConnection.Observer {
        return object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED,
                    -> {
                        onStateChanged(VoiceCallAudioState(isConnected = true))
                    }
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    -> {
                        onStateChanged(
                            VoiceCallAudioState(
                                isConnected = false,
                                errorMessage = "Âm thanh bị ngắt kết nối",
                            ),
                        )
                    }
                    else -> Unit
                }
            }

            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                when (state) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        onStateChanged(VoiceCallAudioState(isConnected = true))
                    }
                    PeerConnection.PeerConnectionState.FAILED,
                    PeerConnection.PeerConnectionState.DISCONNECTED,
                    -> {
                        onStateChanged(
                            VoiceCallAudioState(
                                isConnected = false,
                                errorMessage = "Âm thanh bị ngắt kết nối",
                            ),
                        )
                    }
                    else -> Unit
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit

            override fun onIceCandidate(candidate: IceCandidate) {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        repository.addCallIceCandidate(
                            threadId = threadId,
                            callId = callId,
                            userId = currentUserId,
                            candidate =
                                ChatIceCandidate(
                                    sdpMid = candidate.sdpMid.orEmpty(),
                                    sdpMLineIndex = candidate.sdpMLineIndex,
                                    candidate = candidate.sdp.orEmpty(),
                                    fromUserId = currentUserId,
                                ),
                        )
                    }
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit

            override fun onAddStream(stream: MediaStream) {
                stream.audioTracks.forEach { track ->
                    track.setEnabled(true)
                    track.setVolume(1.0)
                }
            }

            override fun onRemoveStream(stream: MediaStream) = Unit

            override fun onDataChannel(channel: DataChannel) = Unit

            override fun onRenegotiationNeeded() = Unit

            override fun onAddTrack(
                receiver: RtpReceiver,
                mediaStreams: Array<out MediaStream>,
            ) {
                enableRemoteAudio(receiver)
            }
        }
    }

    /**
     * Khởi tạo WebRTC global factory một lần và tạo [PeerConnectionFactory] cho client hiện tại.
     */
    private fun initializeFactory() {
        if (!factoryInitialized) {
            synchronized(WebRtcVoiceCallClient::class.java) {
                if (!factoryInitialized) {
                    PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(appContext)
                            .createInitializationOptions(),
                    )
                    factoryInitialized = true
                }
            }
        }
        val nextAudioDeviceModule = createAudioDeviceModule()
        audioDeviceModule = nextAudioDeviceModule
        factory =
            PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .setAudioDeviceModule(nextAudioDeviceModule)
                .createPeerConnectionFactory()
    }

    /**
     * Tạo [JavaAudioDeviceModule] để WebRTC dùng audio source phù hợp cho cuộc gọi thoại.
     *
     * @return [JavaAudioDeviceModule] đã được cấu hình echo cancellation, noise suppress và callback lỗi.
     */
    private fun createAudioDeviceModule(): JavaAudioDeviceModule {
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

        return JavaAudioDeviceModule.builder(appContext)
            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setAudioAttributes(audioAttributes)
            .setUseHardwareAcousticEchoCanceler(JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported())
            .setUseHardwareNoiseSuppressor(JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported())
            .setAudioRecordErrorCallback(
                object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                    override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                        reportAudioError("Không thể khởi tạo micro: $errorMessage")
                    }

                    override fun onWebRtcAudioRecordStartError(
                        errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode,
                        errorMessage: String,
                    ) {
                        reportAudioError("Không thể bật micro: $errorMessage")
                    }

                    override fun onWebRtcAudioRecordError(errorMessage: String) {
                        reportAudioError("Micro bị lỗi: $errorMessage")
                    }
                },
            )
            .setAudioTrackErrorCallback(
                object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                    override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                        reportAudioError("Không thể khởi tạo loa: $errorMessage")
                    }

                    override fun onWebRtcAudioTrackStartError(
                        errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode,
                        errorMessage: String,
                    ) {
                        reportAudioError("Không thể phát âm thanh: $errorMessage")
                    }

                    override fun onWebRtcAudioTrackError(errorMessage: String) {
                        reportAudioError("Phát âm thanh bị lỗi: $errorMessage")
                    }
                },
            )
            .createAudioDeviceModule()
            .also { module ->
                module.setMicrophoneMute(false)
                module.setSpeakerMute(false)
            }
    }

    /**
     * Chuyển Android audio mode sang [AudioManager.MODE_IN_COMMUNICATION] để ưu tiên luồng thoại.
     */
    private fun configureAudioMode() {
        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = manager
        originalAudioMode = manager.mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            originalCommunicationDevice = manager.communicationDevice
        } else {
            @Suppress("DEPRECATION")
            originalSpeakerphoneOn = manager.isSpeakerphoneOn
        }
        originalMicrophoneMute = manager.isMicrophoneMute
        manager.mode = AudioManager.MODE_IN_COMMUNICATION
        manager.isMicrophoneMute = false
        routeAudioToSpeaker(manager)
    }

    /**
     * Khôi phục audio mode ban đầu sau khi đóng cuộc gọi.
     */
    private fun restoreAudioMode() {
        val manager = audioManager ?: return
        originalMicrophoneMute?.let { manager.isMicrophoneMute = it }
        restoreAudioRoute(manager)
        originalAudioMode?.let { manager.mode = it }
        audioManager = null
        originalAudioMode = null
        originalCommunicationDevice = null
        originalSpeakerphoneOn = null
        originalMicrophoneMute = null
    }

    /**
     * Ưu tiên route âm thanh ra loa ngoài để người dùng nghe được cuộc gọi thoại.
     *
     * @param manager AudioManager đang được cấu hình cho cuộc gọi.
     */
    private fun routeAudioToSpeaker(manager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker =
                manager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
            if (speaker != null) {
                manager.setCommunicationDevice(speaker)
            }
        } else {
            @Suppress("DEPRECATION")
            manager.isSpeakerphoneOn = true
        }
    }

    /**
     * Khôi phục route âm thanh về thiết bị đã dùng trước khi vào cuộc gọi.
     *
     * @param manager AudioManager đang được khôi phục.
     */
    private fun restoreAudioRoute(manager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val previousDevice = originalCommunicationDevice
            if (previousDevice != null) {
                manager.setCommunicationDevice(previousDevice)
            } else {
                manager.clearCommunicationDevice()
            }
        } else {
            originalSpeakerphoneOn?.let { wasSpeakerOn ->
                @Suppress("DEPRECATION")
                manager.isSpeakerphoneOn = wasSpeakerOn
            }
        }
    }

    /**
     * Cleanup tài nguyên hiện tại trước khi chuyển sang một cuộc gọi khác.
     */
    private fun closeForRestart() {
        runCatching { localAudioTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        runCatching { factory?.dispose() }
        runCatching { audioDeviceModule?.release() }
        restoreAudioMode()
        peerConnection = null
        factory = null
        audioDeviceModule = null
        audioSource = null
        localAudioTrack = null
        remoteCandidatesAdded.clear()
        remoteDescriptionSet = false
        localOfferCreated = false
        localAnswerCreated = false
    }

    /**
     * Tạo SDP offer bằng callback WebRTC và chuyển sang suspend.
     *
     * @return [SessionDescription] dạng OFFER.
     */
    private suspend fun PeerConnection.createOfferSuspend(): SessionDescription {
        return suspendCancellableCoroutine { continuation ->
            createOffer(
                object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) {
                        continuation.resume(description)
                    }

                    override fun onSetSuccess() = Unit

                    override fun onCreateFailure(error: String) {
                        continuation.resumeWithException(IllegalStateException(error))
                    }

                    override fun onSetFailure(error: String) = Unit
                },
                audioOnlyConstraints(),
            )
        }
    }

    /**
     * Tạo SDP answer bằng callback WebRTC và chuyển sang suspend.
     *
     * @return [SessionDescription] dạng ANSWER.
     */
    private suspend fun PeerConnection.createAnswerSuspend(): SessionDescription {
        return suspendCancellableCoroutine { continuation ->
            createAnswer(
                object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) {
                        continuation.resume(description)
                    }

                    override fun onSetSuccess() = Unit

                    override fun onCreateFailure(error: String) {
                        continuation.resumeWithException(IllegalStateException(error))
                    }

                    override fun onSetFailure(error: String) = Unit
                },
                audioOnlyConstraints(),
            )
        }
    }

    /**
     * Set local SDP description bằng callback WebRTC và chuyển sang suspend.
     *
     * @param description SDP offer/answer phía local.
     */
    private suspend fun PeerConnection.setLocalDescriptionSuspend(description: SessionDescription) {
        suspendCancellableCoroutine<Unit> { continuation ->
            setLocalDescription(
                object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) = Unit

                    override fun onSetSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onCreateFailure(error: String) = Unit

                    override fun onSetFailure(error: String) {
                        continuation.resumeWithException(IllegalStateException(error))
                    }
                },
                description,
            )
        }
    }

    /**
     * Set remote SDP description bằng callback WebRTC và chuyển sang suspend.
     *
     * @param description SDP offer/answer phía remote.
     */
    private suspend fun PeerConnection.setRemoteDescriptionSuspend(description: SessionDescription) {
        suspendCancellableCoroutine<Unit> { continuation ->
            setRemoteDescription(
                object : SdpObserver {
                    override fun onCreateSuccess(description: SessionDescription) = Unit

                    override fun onSetSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onCreateFailure(error: String) = Unit

                    override fun onSetFailure(error: String) {
                        continuation.resumeWithException(IllegalStateException(error))
                    }
                },
                description,
            )
        }
    }

    /**
     * Tạo constraint cho cuộc gọi chỉ nhận audio, không nhận video.
     *
     * @return [MediaConstraints] dùng khi create offer/answer.
     */
    private fun audioOnlyConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
    }

    /**
     * Bật remote audio track khi PeerConnection nhận được track từ phía bên kia.
     *
     * @param receiver Receiver của track vừa được thêm vào PeerConnection.
     */
    private fun enableRemoteAudio(receiver: RtpReceiver) {
        val track = receiver.track() ?: return
        if (track.kind() != MediaStreamTrack.AUDIO_TRACK_KIND) return

        track.setEnabled(true)
        (track as? AudioTrack)?.setVolume(1.0)
        onStateChanged(VoiceCallAudioState(isConnected = true))
    }

    /**
     * Đẩy lỗi audio lên UI để người dùng biết cuộc gọi không thể khởi tạo/phát âm thanh.
     *
     * @param message Nội dung lỗi thân thiện với người dùng.
     */
    private fun reportAudioError(message: String) {
        onStateChanged(
            VoiceCallAudioState(
                isStarting = false,
                isConnected = false,
                errorMessage = message,
            ),
        )
    }

    companion object {
        private val finishedStatuses = setOf(ChatCallStatus.DECLINED, ChatCallStatus.ENDED, ChatCallStatus.MISSED)

        @Volatile
        private var factoryInitialized = false
    }
}
