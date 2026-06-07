package com.example.dinesplit.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.ChatCallSession
import com.example.dinesplit.domain.model.ChatCallStatus
import com.example.dinesplit.domain.model.ChatIceCandidate
import com.example.dinesplit.domain.model.ChatMessage
import com.example.dinesplit.domain.model.ChatMessageType
import com.example.dinesplit.domain.model.ChatThread
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.repository.ChatRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseChatRepository private constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseProviders.firestore,
) : ChatRepository {
    override fun observeThreads(userId: String): Flow<List<ChatThread>> =
        callbackFlow {
            val listener =
                firestore.collection(COLLECTION_THREADS)
                    .whereEqualTo(FieldPath.of("participantLookup", userId), true)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val threads =
                            snapshot?.documents
                                ?.mapNotNull { it.toChatThread() }
                                ?.sortedByDescending { thread ->
                                    thread.lastMessageAt?.time
                                        ?: thread.updatedAt?.time
                                        ?: thread.createdAt?.time
                                        ?: 0L
                                }
                                ?: emptyList()
                        trySend(threads)
                    }
            awaitClose { listener.remove() }
        }

    override fun observeThread(threadId: String): Flow<ChatThread?> =
        callbackFlow {
            val listener =
                firestore.collection(COLLECTION_THREADS)
                    .document(threadId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.toChatThread())
                    }
            awaitClose { listener.remove() }
        }

    override fun observeMessages(
        threadId: String,
        currentUserId: String,
    ): Flow<List<ChatMessage>> =
        callbackFlow {
            val listener =
                messagesRef(threadId)
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val messages =
                            snapshot?.documents
                                ?.mapNotNull { it.toChatMessage(threadId) }
                                ?.filterNot { currentUserId in it.deletedFor }
                                ?: emptyList()
                        trySend(messages)
                    }
            awaitClose { listener.remove() }
        }

    override fun observeCall(
        threadId: String,
        callId: String,
    ): Flow<ChatCallSession?> =
        callbackFlow {
            val listener =
                callsRef(threadId)
                    .document(callId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.toCallSession(threadId))
                    }
            awaitClose { listener.remove() }
        }

    override suspend fun getOrCreateThread(
        currentUser: UserProfile,
        otherUser: UserProfile,
    ): ChatThread {
        val participants = listOf(currentUser.uid, otherUser.uid).sorted()
        val threadId = participants.joinToString("_")
        val threadRef = firestore.collection(COLLECTION_THREADS).document(threadId)
        val now = Date()
        val lookup = participants.associateWith { true }
        val thread =
            ChatThread(
                id = threadId,
                participants = participants,
                participantLookup = lookup,
                participantNames =
                    mapOf(
                        currentUser.uid to currentUser.displayName.ifBlank { currentUser.username },
                        otherUser.uid to otherUser.displayName.ifBlank { otherUser.username },
                    ),
                participantAvatars =
                    mapOf(
                        currentUser.uid to currentUser.avatarUrl,
                        otherUser.uid to otherUser.avatarUrl,
                    ),
                unreadCounts = participants.associateWith { 0L },
                typing = participants.associateWith { false },
                createdAt = now,
                updatedAt = now,
            )

        val existing = threadRef.get().awaitFirebase().toChatThread()
        if (existing != null) {
            if (existing.participantLookup != lookup) {
                threadRef.set(mapOf("participantLookup" to lookup), SetOptions.merge()).awaitFirebase()
            }
            return existing.copy(participantLookup = lookup)
        }

        threadRef.set(thread).awaitFirebase()
        return thread
    }

    override suspend fun sendText(
        thread: ChatThread,
        sender: UserProfile,
        text: String,
    ) {
        val clean = text.trim()
        if (clean.isBlank()) return
        sendMessage(
            thread = thread,
            sender = sender,
            type = ChatMessageType.TEXT,
            text = clean,
            threadPreview = clean,
        )
    }

    override suspend fun sendSticker(
        thread: ChatThread,
        sender: UserProfile,
        sticker: String,
    ) {
        if (sticker.isBlank()) return
        sendMessage(
            thread = thread,
            sender = sender,
            type = ChatMessageType.STICKER,
            sticker = sticker,
            threadPreview = sticker,
        )
    }

    override suspend fun sendMedia(
        thread: ChatThread,
        sender: UserProfile,
        uri: Uri,
        mediaType: String,
    ) {
        val type =
            when (mediaType) {
                ChatMessageType.VIDEO -> ChatMessageType.VIDEO
                else -> ChatMessageType.IMAGE
            }
        val messageId = messagesRef(thread.id).document().id
        val mime = context.contentResolver.getType(uri) ?: if (type == ChatMessageType.VIDEO) "video/mp4" else "image/jpeg"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: if (type == ChatMessageType.VIDEO) "mp4" else "jpg"
        val storageRef =
            FirebaseProviders.storage.reference
                .child("chat/${thread.id}/${sender.uid}/$messageId.$extension")
        storageRef.putFile(uri).awaitFirebase()
        val url = storageRef.downloadUrl.awaitFirebase().toString()

        sendMessage(
            thread = thread,
            sender = sender,
            messageId = messageId,
            type = type,
            mediaUrl = url,
            mediaMimeType = mime,
            mediaName = "$messageId.$extension",
            threadPreview = if (type == ChatMessageType.VIDEO) "Đã gửi một video" else "Đã gửi một ảnh",
        )
    }

    override suspend fun setTyping(
        threadId: String,
        userId: String,
        isTyping: Boolean,
    ) {
        firestore.collection(COLLECTION_THREADS)
            .document(threadId)
            .update(
                mapOf(
                    "typing.$userId" to isTyping,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .awaitFirebase()
    }

    override suspend fun markThreadRead(
        threadId: String,
        userId: String,
    ) {
        val now = System.currentTimeMillis()
        val snapshot =
            messagesRef(threadId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .awaitFirebase()

        val batch = firestore.batch()
        snapshot.documents.forEach { doc ->
            val senderId = doc.getString("senderId").orEmpty()
            val deletedFor = (doc.get("deletedFor") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            if (senderId != userId && userId !in deletedFor) {
                batch.update(
                    doc.reference,
                    mapOf(
                        "deliveredReceipts.$userId" to now,
                        "readReceipts.$userId" to now,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
        }
        batch.update(
            firestore.collection(COLLECTION_THREADS).document(threadId),
            mapOf(
                "unreadCounts.$userId" to 0,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
        batch.commit().awaitFirebase()
    }

    override suspend fun setReaction(
        threadId: String,
        messageId: String,
        userId: String,
        reaction: String?,
    ) {
        val value = reaction?.takeIf { it.isNotBlank() } ?: FieldValue.delete()
        messagesRef(threadId)
            .document(messageId)
            .update(
                mapOf(
                    "reactions.$userId" to value,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .awaitFirebase()
    }

    override suspend fun recallMessage(
        threadId: String,
        messageId: String,
        userId: String,
    ) {
        val messageRef = messagesRef(threadId).document(messageId)
        val snapshot = messageRef.get().awaitFirebase()
        val senderId = snapshot.getString("senderId").orEmpty()
        require(senderId == userId) { "Only the sender can recall this message." }
        messageRef.update(
            mapOf(
                "recalled" to true,
                "recalledAt" to FieldValue.serverTimestamp(),
                "text" to "",
                "mediaUrl" to "",
                "sticker" to "",
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).awaitFirebase()
    }

    override suspend fun deleteMessageForMe(
        threadId: String,
        messageId: String,
        userId: String,
    ) {
        messagesRef(threadId)
            .document(messageId)
            .update(
                mapOf(
                    "deletedFor" to FieldValue.arrayUnion(userId),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .awaitFirebase()
    }

    override suspend fun startVoiceCall(
        thread: ChatThread,
        caller: UserProfile,
        callee: UserProfile,
    ): ChatCallSession {
        val callRef = callsRef(thread.id).document()
        val call =
            ChatCallSession(
                id = callRef.id,
                threadId = thread.id,
                callerId = caller.uid,
                callerName = caller.displayName.ifBlank { caller.username },
                calleeId = callee.uid,
                calleeName = callee.displayName.ifBlank { callee.username },
                participants = listOf(caller.uid, callee.uid).sorted(),
                status = ChatCallStatus.RINGING,
                mutedBy = mapOf(caller.uid to false, callee.uid to false),
                createdAt = Date(),
                updatedAt = Date(),
            )

        val batch = firestore.batch()
        batch.set(callRef, call)
        batch.set(
            messagesRef(thread.id).document(),
            baseMessageMap(
                thread = thread,
                sender = caller,
                type = ChatMessageType.CALL,
                text = "Cuộc gọi thoại",
            ) + mapOf("callId" to call.id),
        )
        batch.update(
            firestore.collection(COLLECTION_THREADS).document(thread.id),
            threadUpdateMap(
                thread = thread,
                senderId = caller.uid,
                preview = "Cuộc gọi thoại",
                type = ChatMessageType.CALL,
            ) + mapOf("activeCallId" to call.id),
        )
        batch.commit().awaitFirebase()
        return call
    }

    override suspend fun acceptCall(
        threadId: String,
        callId: String,
        userId: String,
    ) {
        updateCallStatus(threadId, callId, ChatCallStatus.ACCEPTED, userId, answered = true)
    }

    override suspend fun declineCall(
        threadId: String,
        callId: String,
        userId: String,
    ) {
        updateCallStatus(threadId, callId, ChatCallStatus.DECLINED, userId, ended = true)
    }

    override suspend fun endCall(
        threadId: String,
        callId: String,
        userId: String,
    ) {
        updateCallStatus(threadId, callId, ChatCallStatus.ENDED, userId, ended = true)
    }

    override suspend fun setCallMuted(
        threadId: String,
        callId: String,
        userId: String,
        muted: Boolean,
    ) {
        callsRef(threadId)
            .document(callId)
            .update(
                mapOf(
                    "mutedBy.$userId" to muted,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .awaitFirebase()
    }

    override suspend fun saveCallOffer(
        threadId: String,
        callId: String,
        sdp: String,
    ) {
        callsRef(threadId)
            .document(callId)
            .update(
                mapOf(
                    "offerSdp" to sdp,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .awaitFirebase()
    }

    override suspend fun saveCallAnswer(
        threadId: String,
        callId: String,
        sdp: String,
    ) {
        callsRef(threadId)
            .document(callId)
            .update(
                mapOf(
                    "answerSdp" to sdp,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .awaitFirebase()
    }

    override suspend fun addCallIceCandidate(
        threadId: String,
        callId: String,
        userId: String,
        candidate: ChatIceCandidate,
    ) {
        val call = callsRef(threadId).document(callId).get().awaitFirebase().toCallSession(threadId)
            ?: error("Call not found")
        val field =
            when (userId) {
                call.callerId -> "callerCandidates"
                call.calleeId -> "calleeCandidates"
                else -> error("User is not part of this call")
            }

        callsRef(threadId)
            .document(callId)
            .update(
                mapOf(
                    field to FieldValue.arrayUnion(
                        candidate.copy(
                            fromUserId = userId,
                            createdAt = System.currentTimeMillis(),
                        ).toFirestoreMap(),
                    ),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .awaitFirebase()
    }

    private suspend fun sendMessage(
        thread: ChatThread,
        sender: UserProfile,
        messageId: String = messagesRef(thread.id).document().id,
        type: String,
        text: String = "",
        mediaUrl: String = "",
        mediaMimeType: String = "",
        mediaName: String = "",
        sticker: String = "",
        threadPreview: String,
    ) {
        val messageRef = messagesRef(thread.id).document(messageId)
        val batch = firestore.batch()
        batch.set(
            messageRef,
            baseMessageMap(
                thread = thread,
                sender = sender,
                type = type,
                text = text,
                mediaUrl = mediaUrl,
                mediaMimeType = mediaMimeType,
                mediaName = mediaName,
                sticker = sticker,
            ),
        )
        batch.update(
            firestore.collection(COLLECTION_THREADS).document(thread.id),
            threadUpdateMap(
                thread = thread,
                senderId = sender.uid,
                preview = threadPreview,
                type = type,
            ),
        )
        batch.commit().awaitFirebase()
    }

    private fun baseMessageMap(
        thread: ChatThread,
        sender: UserProfile,
        type: String,
        text: String = "",
        mediaUrl: String = "",
        mediaMimeType: String = "",
        mediaName: String = "",
        sticker: String = "",
    ): Map<String, Any?> {
        val now = System.currentTimeMillis()
        return mapOf(
            "threadId" to thread.id,
            "senderId" to sender.uid,
            "senderName" to sender.displayName.ifBlank { sender.username },
            "senderAvatar" to sender.avatarUrl,
            "type" to type,
            "text" to text,
            "mediaUrl" to mediaUrl,
            "mediaMimeType" to mediaMimeType,
            "mediaName" to mediaName,
            "sticker" to sticker,
            "reactions" to emptyMap<String, String>(),
            "deliveredReceipts" to mapOf(sender.uid to now),
            "readReceipts" to mapOf(sender.uid to now),
            "deletedFor" to emptyList<String>(),
            "recalled" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
    }

    private fun threadUpdateMap(
        thread: ChatThread,
        senderId: String,
        preview: String,
        type: String,
    ): Map<String, Any> {
        val receiverUpdates =
            thread.participants
                .filterNot { it == senderId }
                .associate { uid -> "unreadCounts.$uid" to FieldValue.increment(1) }

        return mapOf(
            "lastMessage" to preview,
            "lastMessageType" to type,
            "lastMessageSenderId" to senderId,
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "typing.$senderId" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
        ) + receiverUpdates
    }

    private suspend fun updateCallStatus(
        threadId: String,
        callId: String,
        status: String,
        userId: String,
        answered: Boolean = false,
        ended: Boolean = false,
    ) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (answered) updates["answeredAt"] = FieldValue.serverTimestamp()
        if (ended) updates["endedAt"] = FieldValue.serverTimestamp()

        val batch = firestore.batch()
        batch.update(callsRef(threadId).document(callId), updates)
        if (ended || status == ChatCallStatus.DECLINED || status == ChatCallStatus.MISSED) {
            batch.set(
                firestore.collection(COLLECTION_THREADS).document(threadId),
                mapOf(
                    "activeCallId" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }
        batch.commit().awaitFirebase()
    }

    private fun messagesRef(threadId: String) =
        firestore.collection(COLLECTION_THREADS)
            .document(threadId)
            .collection(COLLECTION_MESSAGES)

    private fun callsRef(threadId: String) =
        firestore.collection(COLLECTION_THREADS)
            .document(threadId)
            .collection(COLLECTION_CALLS)

    private fun DocumentSnapshot.toChatThread(): ChatThread? {
        if (!exists()) return null
        return runCatching { toObject(ChatThread::class.java)?.copy(id = id) }.getOrNull()
    }

    private fun DocumentSnapshot.toChatMessage(threadId: String): ChatMessage? {
        if (!exists()) return null
        return runCatching { toObject(ChatMessage::class.java)?.copy(id = id, threadId = threadId) }.getOrNull()
    }

    private fun DocumentSnapshot.toCallSession(threadId: String): ChatCallSession? {
        if (!exists()) return null
        return runCatching {
            toObject(ChatCallSession::class.java)?.copy(
                id = id,
                threadId = threadId,
                callerCandidates = parseIceCandidates("callerCandidates"),
                calleeCandidates = parseIceCandidates("calleeCandidates"),
            )
        }.getOrNull()
    }

    private fun DocumentSnapshot.parseIceCandidates(field: String): List<ChatIceCandidate> {
        val rawCandidates = get(field) as? List<*> ?: return emptyList()
        return rawCandidates.mapNotNull { raw ->
            val data = raw as? Map<*, *> ?: return@mapNotNull raw as? ChatIceCandidate
            ChatIceCandidate(
                sdpMid = data["sdpMid"]?.toString().orEmpty(),
                sdpMLineIndex = (data["sdpMLineIndex"] as? Number)?.toInt() ?: 0,
                candidate = data["candidate"]?.toString().orEmpty(),
                fromUserId = data["fromUserId"]?.toString().orEmpty(),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            )
        }
    }

    private fun ChatIceCandidate.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "sdpMid" to sdpMid,
            "sdpMLineIndex" to sdpMLineIndex,
            "candidate" to candidate,
            "fromUserId" to fromUserId,
            "createdAt" to createdAt,
        )
    }

    private suspend fun <T> Task<T>.awaitFirebase(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase task failed"),
                    )
                }
            }
        }
    }

    companion object {
        private const val COLLECTION_THREADS = "chat_threads"
        private const val COLLECTION_MESSAGES = "messages"
        private const val COLLECTION_CALLS = "calls"

        @Volatile
        private var instance: FirebaseChatRepository? = null

        fun getInstance(context: Context): FirebaseChatRepository {
            return instance ?: synchronized(this) {
                instance ?: FirebaseChatRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
