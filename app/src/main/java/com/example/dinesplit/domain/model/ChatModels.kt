package com.example.dinesplit.domain.model

import java.util.Date

object ChatMessageType {
    const val TEXT = "TEXT"
    const val IMAGE = "IMAGE"
    const val VIDEO = "VIDEO"
    const val STICKER = "STICKER"
    const val CALL = "CALL"
}

object ChatCallStatus {
    const val RINGING = "RINGING"
    const val ACCEPTED = "ACCEPTED"
    const val DECLINED = "DECLINED"
    const val ENDED = "ENDED"
    const val MISSED = "MISSED"
}

data class ChatThread(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantLookup: Map<String, Boolean> = emptyMap(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantAvatars: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageType: String = ChatMessageType.TEXT,
    val lastMessageSenderId: String = "",
    val lastMessageAt: Date? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    val typing: Map<String, Boolean> = emptyMap(),
    val activeCallId: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
) {
    fun otherParticipant(currentUserId: String): String {
        return participants.firstOrNull { it != currentUserId }.orEmpty()
    }

    fun displayNameFor(uid: String): String {
        return participantNames[uid]?.takeIf { it.isNotBlank() } ?: uid.take(8)
    }

    fun avatarFor(uid: String): String {
        return participantAvatars[uid].orEmpty()
    }
}

data class ChatMessage(
    val id: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val type: String = ChatMessageType.TEXT,
    val text: String = "",
    val mediaUrl: String = "",
    val mediaMimeType: String = "",
    val mediaName: String = "",
    val sticker: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val deliveredReceipts: Map<String, Long> = emptyMap(),
    val readReceipts: Map<String, Long> = emptyMap(),
    val deletedFor: List<String> = emptyList(),
    val recalled: Boolean = false,
    val recalledAt: Date? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
) {
    fun visibleText(): String {
        if (recalled) return "Tin nhắn đã được thu hồi"
        return when (type) {
            ChatMessageType.IMAGE -> "Ảnh"
            ChatMessageType.VIDEO -> "Video"
            ChatMessageType.STICKER -> sticker
            ChatMessageType.CALL -> text.ifBlank { "Cuộc gọi" }
            else -> text
        }
    }
}

data class ChatCallSession(
    val id: String = "",
    val threadId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val participants: List<String> = emptyList(),
    val status: String = ChatCallStatus.RINGING,
    val mutedBy: Map<String, Boolean> = emptyMap(),
    val offerSdp: String = "",
    val answerSdp: String = "",
    val callerCandidates: List<ChatIceCandidate> = emptyList(),
    val calleeCandidates: List<ChatIceCandidate> = emptyList(),
    val createdAt: Date? = null,
    val answeredAt: Date? = null,
    val endedAt: Date? = null,
    val updatedAt: Date? = null,
)

data class ChatIceCandidate(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val candidate: String = "",
    val fromUserId: String = "",
    val createdAt: Long = 0L,
) {
    fun stableKey(): String {
        return "$sdpMid:$sdpMLineIndex:$candidate"
    }
}
