package com.example.dinesplit.presentation.chat

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.LoadingBlock
import com.example.dinesplit.domain.model.ChatCallStatus
import com.example.dinesplit.domain.model.ChatMessage
import com.example.dinesplit.domain.model.ChatMessageType
import com.example.dinesplit.domain.model.ChatThread
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: ChatListViewModel = viewModel(
        factory = ChatListViewModel.Factory(LocalContext.current.applicationContext as Application),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(
        title = "Tin nhắn",
        navigationIcon = { BackNavigationButton(onClick = onBack) },
    ) {
        when {
            uiState.isLoading -> LoadingBlock(message = "Đang tải hội thoại...")
            uiState.errorMessage != null -> {
                ErrorStateBlock(
                    title = "Không thể tải tin nhắn",
                    subtitle = uiState.errorMessage.orEmpty(),
                    onRetryClick = viewModel::retry,
                )
            }
            uiState.threads.isEmpty() -> {
                EmptyStateBlock(
                    title = "Chưa có hội thoại",
                    subtitle = "Mở profile người khác và bấm Nhắn tin để bắt đầu.",
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                    contentPadding = PaddingValues(bottom = AppDimens.spaceXl),
                ) {
                    items(uiState.threads, key = { it.id }) { thread ->
                        ChatThreadRow(
                            thread = thread,
                            currentUserId = uiState.currentUserId,
                            onClick = {
                                val otherUid = thread.otherParticipant(uiState.currentUserId)
                                if (otherUid.isNotBlank()) onOpenChat(otherUid)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDetailScreen(
    otherUserId: String,
    onBack: () -> Unit,
    onOpenCall: (String, String) -> Unit,
    viewModel: ChatDetailViewModel = viewModel(
        factory = ChatDetailViewModel.Factory(
            LocalContext.current.applicationContext as Application,
            otherUserId,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.sendMedia(uri, ChatMessageType.IMAGE)
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.sendMedia(uri, ChatMessageType.VIDEO)
    }
    var showStickers by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            if (effect is ChatEffect.OpenCall) {
                onOpenCall(effect.threadId, effect.callId)
            }
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    AppScaffold(
        title = uiState.otherUser?.displayName?.takeIf { it.isNotBlank() } ?: "Tin nhắn",
        navigationIcon = { BackNavigationButton(onClick = onBack) },
        actions = {
            IconButton(onClick = viewModel::startVoiceCall, enabled = uiState.thread != null && uiState.otherUser != null) {
                Icon(Icons.Filled.Call, contentDescription = "Gọi thoại")
            }
        },
        contentPadding = PaddingValues(0.dp),
    ) {
        when {
            uiState.isLoading -> LoadingBlock(message = "Đang mở hội thoại...")
            uiState.errorMessage != null && uiState.thread == null -> {
                ErrorStateBlock(
                    title = "Không thể mở chat",
                    subtitle = uiState.errorMessage.orEmpty(),
                    retryText = "Quay lại",
                    onRetryClick = onBack,
                    modifier = Modifier.padding(AppDimens.spaceLg),
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                ) {
                    ActiveCallBanner(uiState = uiState, onOpen = viewModel::openActiveCall)

                    MessageList(
                        listState = listState,
                        messages = uiState.messages,
                        currentUserId = uiState.currentUserId,
                        otherUserId = uiState.otherUser?.uid.orEmpty(),
                        onReaction = viewModel::toggleReaction,
                        onRecall = viewModel::recallMessage,
                        onDelete = viewModel::deleteMessageForMe,
                        modifier = Modifier.weight(1f),
                    )

                    if (uiState.isOtherTyping) {
                        Text(
                            text = "${uiState.otherUser?.displayName ?: "Đối phương"} đang nhập...",
                            style = MaterialTheme.typography.labelMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceXs),
                        )
                    }

                    if (showStickers) {
                        StickerTray(onSticker = viewModel::sendSticker)
                    }

                    ChatInputBar(
                        input = uiState.input,
                        isSending = uiState.isSending,
                        onInputChange = viewModel::onInputChange,
                        onSend = viewModel::sendText,
                        onPickImage = { imagePicker.launch("image/*") },
                        onPickVideo = { videoPicker.launch("video/*") },
                        onToggleStickers = { showStickers = !showStickers },
                    )
                }
            }
        }
    }
}

@Composable
fun ChatCallScreen(
    threadId: String,
    callId: String,
    onBack: () -> Unit,
    viewModel: ChatCallViewModel = viewModel(
        factory = ChatCallViewModel.Factory(
            LocalContext.current.applicationContext as Application,
            threadId,
            callId,
        ),
    ),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val call = uiState.call
    var pendingAccept by remember { mutableStateOf(false) }
    val micPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onMicrophonePermissionResult(granted)
            if (granted && pendingAccept) {
                viewModel.accept()
            }
            pendingAccept = false
        }

    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun requestMicrophoneForCall() {
        if (hasMicrophonePermission()) {
            viewModel.onMicrophonePermissionResult(true)
        } else {
            pendingAccept = false
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun requestMicrophoneForAccept() {
        if (hasMicrophonePermission()) {
            viewModel.onMicrophonePermissionResult(true)
            viewModel.accept()
        } else {
            pendingAccept = true
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(call?.id, call?.status, uiState.currentUserId, uiState.hasMicrophonePermission) {
        val activeCall = call ?: return@LaunchedEffect
        if (
            activeCall.callerId == uiState.currentUserId &&
            !uiState.isFinished &&
            !uiState.hasMicrophonePermission
        ) {
            requestMicrophoneForCall()
        }
    }

    AppScaffold(
        title = "Cuộc gọi",
        navigationIcon = { BackNavigationButton(onClick = onBack) },
    ) {
        when {
            uiState.isLoading -> LoadingBlock(message = "Đang kết nối cuộc gọi...")
            call == null -> {
                ErrorStateBlock(
                    title = "Cuộc gọi không còn tồn tại",
                    subtitle = "Cuộc gọi này đã bị đóng.",
                    retryText = "Quay lại",
                    onRetryClick = onBack,
                )
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val peerName =
                        if (call.callerId == uiState.currentUserId) call.calleeName else call.callerName
                    DineAvatarImage(imageUrl = null, name = peerName, size = 96.dp)
                    Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                    Text(peerName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = call.status.toCallStatusText(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CallAudioStatus(uiState)
                    Spacer(modifier = Modifier.height(AppDimens.space2Xl))

                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)) {
                        if (uiState.isIncoming) {
                            CallActionButton(
                                icon = Icons.Filled.Close,
                                label = "Từ chối",
                                color = MaterialTheme.colorScheme.error,
                                onClick = viewModel::decline,
                            )
                            CallActionButton(
                                icon = Icons.Filled.Check,
                                label = "Nhận",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { requestMicrophoneForAccept() },
                            )
                        } else {
                            CallActionButton(
                                icon = if (uiState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                label = if (uiState.isMuted) "Bật mic" else "Tắt mic",
                                color = MaterialTheme.colorScheme.secondary,
                                enabled = !uiState.isFinished,
                                onClick = viewModel::toggleMute,
                            )
                            CallActionButton(
                                icon = Icons.Filled.CallEnd,
                                label = "Kết thúc",
                                color = MaterialTheme.colorScheme.error,
                                enabled = !uiState.isFinished,
                                onClick = viewModel::end,
                            )
                        }
                    }

                    if (uiState.isFinished) {
                        Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                        TextButton(onClick = onBack) {
                            Text("Quay lại chat")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallAudioStatus(uiState: ChatCallUiState) {
    val statusText =
        when {
            uiState.isFinished -> ""
            !uiState.hasMicrophonePermission -> "Cần quyền micro để kết nối âm thanh"
            uiState.audioState.errorMessage != null -> uiState.audioState.errorMessage
            uiState.audioState.isConnected -> "Âm thanh đã kết nối"
            uiState.audioState.isStarting -> "Đang nối âm thanh..."
            uiState.call?.status == ChatCallStatus.RINGING -> "Đang đổ chuông..."
            else -> "Đang chờ tín hiệu âm thanh..."
        }
    if (statusText.isBlank()) return

    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
    Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        color =
            if (uiState.audioState.errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

@Composable
private fun ChatThreadRow(
    thread: ChatThread,
    currentUserId: String,
    onClick: () -> Unit,
) {
    val otherUid = thread.otherParticipant(currentUserId)
    val unread = thread.unreadCounts[currentUserId] ?: 0L
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            DineAvatarImage(
                imageUrl = thread.avatarFor(otherUid),
                name = thread.displayNameFor(otherUid),
                size = 52.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.displayNameFor(otherUid),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = thread.lastMessageAt.formatChatTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (thread.typing[otherUid] == true) "Đang nhập..." else thread.lastMessage.ifBlank { "Bắt đầu trò chuyện" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unread > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (unread > 0) {
                Badge {
                    Text(if (unread > 99) "99+" else unread.toString())
                }
            }
        }
    }
}

@Composable
private fun ActiveCallBanner(
    uiState: ChatDetailUiState,
    onOpen: () -> Unit,
) {
    if (uiState.activeCallId.isBlank()) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        ) {
            Icon(Icons.Filled.Call, contentDescription = null)
            Text(
                text = "Cuộc gọi đang hoạt động. Bấm để mở.",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun MessageList(
    listState: LazyListState,
    messages: List<ChatMessage>,
    currentUserId: String,
    otherUserId: String,
    onReaction: (ChatMessage, String) -> Unit,
    onRecall: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        items(messages, key = { it.id }) { message ->
            ChatMessageBubble(
                message = message,
                isMine = message.senderId == currentUserId,
                otherUserId = otherUserId,
                onReaction = { reaction -> onReaction(message, reaction) },
                onRecall = { onRecall(message) },
                onDelete = { onDelete(message) },
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    otherUserId: String,
    onReaction: (String) -> Unit,
    onRecall: () -> Unit,
    onDelete: () -> Unit,
) {
    var showActions by remember(message.id) { mutableStateOf(false) }
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .clickable { showActions = !showActions },
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMine) 18.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 18.dp,
                ),
                color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                ) {
                    MessageContent(message = message, onOpenVideo = {
                        openMedia(context, message.mediaUrl, message.mediaMimeType)
                    })
                    if (message.reactions.isNotEmpty()) {
                        Text(
                            text = message.reactions.values.joinToString(" "),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(
                    text = message.createdAt.formatChatTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isMine) {
                    Text(
                        text = message.deliveryText(otherUserId),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (showActions) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChatReactionSet.forEach { reaction ->
                        Text(
                            text = reaction,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onReaction(reaction) }
                                .padding(6.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Xóa")
                    }
                    if (isMine && !message.recalled) {
                        TextButton(onClick = onRecall) {
                            Text("Thu hồi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageContent(
    message: ChatMessage,
    onOpenVideo: () -> Unit,
) {
    when {
        message.recalled -> {
            Text(
                text = "Tin nhắn đã được thu hồi",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            )
        }
        message.type == ChatMessageType.IMAGE -> {
            AsyncImage(
                model = message.mediaUrl,
                contentDescription = message.mediaName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        message.type == ChatMessageType.VIDEO -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenVideo),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                    Text(
                        text = "Mở video",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(AppDimens.spaceMd),
                    )
                }
            }
        }
        message.type == ChatMessageType.STICKER -> {
            Text(text = message.sticker, style = MaterialTheme.typography.displaySmall)
        }
        else -> {
            Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StickerTray(onSticker: (String) -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        ) {
            ChatStickerSet.forEach { sticker ->
                AssistChip(onClick = { onSticker(sticker) }, label = { Text(sticker) })
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onToggleStickers: () -> Unit,
) {
    Surface(tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            IconButton(onClick = onPickImage) {
                Icon(Icons.Filled.Image, contentDescription = "Gửi ảnh")
            }
            IconButton(onClick = onPickVideo) {
                Icon(Icons.Filled.Movie, contentDescription = "Gửi video")
            }
            IconButton(onClick = onToggleStickers) {
                Icon(Icons.Filled.TagFaces, contentDescription = "Sticker")
            }
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhắn tin...") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
            )
            IconButton(onClick = onSend, enabled = !isSending && input.isNotBlank()) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi")
                }
            }
        }
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            modifier = Modifier.size(64.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(AppDimens.spaceXs))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun ChatMessage.deliveryText(otherUserId: String): String {
    return when {
        otherUserId.isNotBlank() && readReceipts.containsKey(otherUserId) -> "Đã đọc"
        otherUserId.isNotBlank() && deliveredReceipts.containsKey(otherUserId) -> "Đã nhận"
        else -> "Đã gửi"
    }
}

private fun java.util.Date?.formatChatTime(): String {
    if (this == null) return ""
    return SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(this)
}

private fun String.toCallStatusText(): String {
    return when (this) {
        ChatCallStatus.RINGING -> "Đang đổ chuông"
        ChatCallStatus.ACCEPTED -> "Đang trong cuộc gọi"
        ChatCallStatus.DECLINED -> "Đã từ chối"
        ChatCallStatus.ENDED -> "Đã kết thúc"
        ChatCallStatus.MISSED -> "Cuộc gọi nhỡ"
        else -> this
    }
}

private fun openMedia(
    context: android.content.Context,
    url: String,
    mimeType: String,
) {
    if (url.isBlank()) return
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), mimeType.ifBlank { "video/*" })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(intent) }
}
