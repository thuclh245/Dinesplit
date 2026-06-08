package com.example.dinesplit.domain.repository

import android.net.Uri
import com.example.dinesplit.domain.model.ChatIceCandidate
import com.example.dinesplit.domain.model.ChatCallSession
import com.example.dinesplit.domain.model.ChatMessage
import com.example.dinesplit.domain.model.ChatThread
import com.example.dinesplit.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Contract cho toàn bộ tính năng chat một-một và gọi thoại WebRTC.
 *
 * Interface này gom các thao tác realtime (thread, message, call session), gửi nội dung chat,
 * cập nhật trạng thái đọc/gõ tin nhắn và lưu signaling data phục vụ cuộc gọi thoại.
 */
interface ChatRepository {
    /**
     * Quan sát danh sách cuộc trò chuyện mà người dùng tham gia.
     *
     * @param userId UID người dùng hiện tại.
     * @return [Flow] phát ra danh sách [ChatThread] mới nhất.
     */
    fun observeThreads(userId: String): Flow<List<ChatThread>>

    /**
     * Quan sát thông tin metadata của một cuộc trò chuyện.
     *
     * @param threadId ID thread cần theo dõi.
     * @return [Flow] phát ra [ChatThread] hoặc null nếu thread không tồn tại.
     */
    fun observeThread(threadId: String): Flow<ChatThread?>

    /**
     * Quan sát danh sách tin nhắn trong một thread.
     *
     * @param threadId ID thread cần theo dõi.
     * @param currentUserId UID người dùng hiện tại, dùng để lọc tin nhắn đã xóa phía mình.
     * @return [Flow] phát ra danh sách [ChatMessage] theo thứ tự thời gian tăng dần.
     */
    fun observeMessages(
        threadId: String,
        currentUserId: String,
    ): Flow<List<ChatMessage>>

    /**
     * Quan sát trạng thái một phiên gọi thoại.
     *
     * @param threadId ID thread chứa cuộc gọi.
     * @param callId ID phiên gọi thoại.
     * @return [Flow] phát ra [ChatCallSession] hoặc null nếu cuộc gọi không tồn tại.
     */
    fun observeCall(
        threadId: String,
        callId: String,
    ): Flow<ChatCallSession?>

    /**
     * Lấy thread giữa hai người dùng hoặc tạo mới nếu chưa có.
     *
     * @param currentUser Hồ sơ người dùng hiện tại.
     * @param otherUser Hồ sơ người dùng còn lại.
     * @return [ChatThread] đã tồn tại hoặc vừa tạo.
     */
    suspend fun getOrCreateThread(
        currentUser: UserProfile,
        otherUser: UserProfile,
    ): ChatThread

    /**
     * Gửi tin nhắn văn bản.
     *
     * @param thread Thread đích.
     * @param sender Người gửi.
     * @param text Nội dung tin nhắn.
     */
    suspend fun sendText(
        thread: ChatThread,
        sender: UserProfile,
        text: String,
    )

    /**
     * Gửi sticker trong thread.
     *
     * @param thread Thread đích.
     * @param sender Người gửi.
     * @param sticker Mã/ký hiệu sticker.
     */
    suspend fun sendSticker(
        thread: ChatThread,
        sender: UserProfile,
        sticker: String,
    )

    /**
     * Tải và gửi media ảnh/video.
     *
     * @param thread Thread đích.
     * @param sender Người gửi.
     * @param uri Uri cục bộ của file media.
     * @param mediaType Loại media theo [com.example.dinesplit.domain.model.ChatMessageType].
     */
    suspend fun sendMedia(
        thread: ChatThread,
        sender: UserProfile,
        uri: Uri,
        mediaType: String,
    )

    /**
     * Cập nhật trạng thái đang gõ của một người dùng trong thread.
     *
     * @param threadId ID thread.
     * @param userId UID người đang gõ.
     * @param isTyping true nếu đang nhập nội dung.
     */
    suspend fun setTyping(
        threadId: String,
        userId: String,
        isTyping: Boolean,
    )

    /**
     * Đánh dấu thread là đã đọc cho một người dùng.
     *
     * @param threadId ID thread.
     * @param userId UID người đọc.
     */
    suspend fun markThreadRead(
        threadId: String,
        userId: String,
    )

    /**
     * Gán hoặc gỡ reaction trên một tin nhắn.
     *
     * @param threadId ID thread.
     * @param messageId ID tin nhắn.
     * @param userId UID người reaction.
     * @param reaction Reaction mới; null nghĩa là xóa reaction hiện tại.
     */
    suspend fun setReaction(
        threadId: String,
        messageId: String,
        userId: String,
        reaction: String?,
    )

    /**
     * Thu hồi tin nhắn cho mọi người trong thread.
     *
     * @param threadId ID thread.
     * @param messageId ID tin nhắn.
     * @param userId UID người yêu cầu thu hồi; chỉ người gửi hợp lệ.
     */
    suspend fun recallMessage(
        threadId: String,
        messageId: String,
        userId: String,
    )

    /**
     * Xóa tin nhắn ở phía người dùng hiện tại nhưng không xóa cho người còn lại.
     *
     * @param threadId ID thread.
     * @param messageId ID tin nhắn.
     * @param userId UID người xóa.
     */
    suspend fun deleteMessageForMe(
        threadId: String,
        messageId: String,
        userId: String,
    )

    /**
     * Khởi tạo một phiên gọi thoại và ghi message call vào thread.
     *
     * @param thread Thread chứa cuộc gọi.
     * @param caller Người gọi.
     * @param callee Người nhận cuộc gọi.
     * @return Phiên gọi thoại mới ở trạng thái ringing.
     */
    suspend fun startVoiceCall(
        thread: ChatThread,
        caller: UserProfile,
        callee: UserProfile,
    ): ChatCallSession

    /**
     * Chấp nhận cuộc gọi.
     *
     * @param threadId ID thread.
     * @param callId ID phiên gọi.
     * @param userId UID người chấp nhận.
     */
    suspend fun acceptCall(
        threadId: String,
        callId: String,
        userId: String,
    )

    /**
     * Từ chối cuộc gọi.
     *
     * @param threadId ID thread.
     * @param callId ID phiên gọi.
     * @param userId UID người từ chối.
     */
    suspend fun declineCall(
        threadId: String,
        callId: String,
        userId: String,
    )

    /**
     * Kết thúc cuộc gọi đang diễn ra.
     *
     * @param threadId ID thread.
     * @param callId ID phiên gọi.
     * @param userId UID người kết thúc.
     */
    suspend fun endCall(
        threadId: String,
        callId: String,
        userId: String,
    )

    /**
     * Cập nhật trạng thái tắt/bật micro của một người tham gia cuộc gọi.
     *
     * @param threadId ID thread.
     * @param callId ID phiên gọi.
     * @param userId UID người cần cập nhật.
     * @param muted true nếu tắt micro.
     */
    suspend fun setCallMuted(
        threadId: String,
        callId: String,
        userId: String,
        muted: Boolean,
    )

    /**
     * Lưu SDP offer của WebRTC caller lên signaling document.
     *
     * @param threadId ID thread.
     * @param callId ID phiên gọi.
     * @param sdp Nội dung SDP offer.
     */
    suspend fun saveCallOffer(
        threadId: String,
        callId: String,
        sdp: String,
    )

    /**
     * Lưu SDP answer của WebRTC callee lên signaling document.
     *
     * @param threadId ID thread.
     * @param callId ID phiên gọi.
     * @param sdp Nội dung SDP answer.
     */
    suspend fun saveCallAnswer(
        threadId: String,
        callId: String,
        sdp: String,
    )

    /**
     * Thêm ICE candidate vào danh sách candidate của người gọi hoặc người nhận.
     *
     * @param threadId ID thread.
     * @param callId ID phiên gọi.
     * @param userId UID người tạo candidate.
     * @param candidate ICE candidate cần lưu.
     */
    suspend fun addCallIceCandidate(
        threadId: String,
        callId: String,
        userId: String,
        candidate: ChatIceCandidate,
    )
}
