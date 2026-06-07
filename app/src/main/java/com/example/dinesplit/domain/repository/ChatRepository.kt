package com.example.dinesplit.domain.repository

import android.net.Uri
import com.example.dinesplit.domain.model.ChatIceCandidate
import com.example.dinesplit.domain.model.ChatCallSession
import com.example.dinesplit.domain.model.ChatMessage
import com.example.dinesplit.domain.model.ChatThread
import com.example.dinesplit.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeThreads(userId: String): Flow<List<ChatThread>>

    fun observeThread(threadId: String): Flow<ChatThread?>

    fun observeMessages(
        threadId: String,
        currentUserId: String,
    ): Flow<List<ChatMessage>>

    fun observeCall(
        threadId: String,
        callId: String,
    ): Flow<ChatCallSession?>

    suspend fun getOrCreateThread(
        currentUser: UserProfile,
        otherUser: UserProfile,
    ): ChatThread

    suspend fun sendText(
        thread: ChatThread,
        sender: UserProfile,
        text: String,
    )

    suspend fun sendSticker(
        thread: ChatThread,
        sender: UserProfile,
        sticker: String,
    )

    suspend fun sendMedia(
        thread: ChatThread,
        sender: UserProfile,
        uri: Uri,
        mediaType: String,
    )

    suspend fun setTyping(
        threadId: String,
        userId: String,
        isTyping: Boolean,
    )

    suspend fun markThreadRead(
        threadId: String,
        userId: String,
    )

    suspend fun setReaction(
        threadId: String,
        messageId: String,
        userId: String,
        reaction: String?,
    )

    suspend fun recallMessage(
        threadId: String,
        messageId: String,
        userId: String,
    )

    suspend fun deleteMessageForMe(
        threadId: String,
        messageId: String,
        userId: String,
    )

    suspend fun startVoiceCall(
        thread: ChatThread,
        caller: UserProfile,
        callee: UserProfile,
    ): ChatCallSession

    suspend fun acceptCall(
        threadId: String,
        callId: String,
        userId: String,
    )

    suspend fun declineCall(
        threadId: String,
        callId: String,
        userId: String,
    )

    suspend fun endCall(
        threadId: String,
        callId: String,
        userId: String,
    )

    suspend fun setCallMuted(
        threadId: String,
        callId: String,
        userId: String,
        muted: Boolean,
    )

    suspend fun saveCallOffer(
        threadId: String,
        callId: String,
        sdp: String,
    )

    suspend fun saveCallAnswer(
        threadId: String,
        callId: String,
        sdp: String,
    )

    suspend fun addCallIceCandidate(
        threadId: String,
        callId: String,
        userId: String,
        candidate: ChatIceCandidate,
    )
}
