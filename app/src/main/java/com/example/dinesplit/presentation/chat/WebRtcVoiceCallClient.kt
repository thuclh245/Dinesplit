package com.example.dinesplit.presentation.chat

import android.content.Context
import android.media.AudioManager
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
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class VoiceCallAudioState(
    val isStarting: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
)

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
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var originalAudioMode: Int? = null
    private var currentCallKey: String = ""
    private var currentUserId: String = ""
    private var remoteDescriptionSet = false
    private var localOfferCreated = false
    private var localAnswerCreated = false
    private var closed = false

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

    fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun close() {
        if (closed) return
        closed = true
        runCatching { localAudioTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        runCatching { factory?.dispose() }
        restoreAudioMode()
        factory = null
        peerConnection = null
        audioSource = null
        localAudioTrack = null
        currentCallKey = ""
        remoteCandidatesAdded.clear()
        remoteDescriptionSet = false
        localOfferCreated = false
        localAnswerCreated = false
        onStateChanged(VoiceCallAudioState())
    }

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

    private fun createObserver(
        threadId: String,
        callId: String,
    ): PeerConnection.Observer {
        return object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        onStateChanged(VoiceCallAudioState(isConnected = true))
                    }
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
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

            override fun onAddStream(stream: MediaStream) = Unit

            override fun onRemoveStream(stream: MediaStream) = Unit

            override fun onDataChannel(channel: DataChannel) = Unit

            override fun onRenegotiationNeeded() = Unit

            override fun onAddTrack(
                receiver: RtpReceiver,
                mediaStreams: Array<out MediaStream>,
            ) = Unit
        }
    }

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
        factory =
            PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
    }

    private fun configureAudioMode() {
        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = manager
        originalAudioMode = manager.mode
        manager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    private fun restoreAudioMode() {
        val manager = audioManager ?: return
        originalAudioMode?.let { manager.mode = it }
        audioManager = null
        originalAudioMode = null
    }

    private fun closeForRestart() {
        runCatching { localAudioTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        runCatching { factory?.dispose() }
        restoreAudioMode()
        peerConnection = null
        factory = null
        audioSource = null
        localAudioTrack = null
        remoteCandidatesAdded.clear()
        remoteDescriptionSet = false
        localOfferCreated = false
        localAnswerCreated = false
    }

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

    private fun audioOnlyConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
    }

    companion object {
        private val finishedStatuses = setOf(ChatCallStatus.DECLINED, ChatCallStatus.ENDED, ChatCallStatus.MISSED)

        @Volatile
        private var factoryInitialized = false
    }
}
