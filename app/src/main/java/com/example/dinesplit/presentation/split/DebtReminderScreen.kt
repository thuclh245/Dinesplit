package com.example.dinesplit.presentation.split

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.domain.repository.NotificationRepository
import com.example.dinesplit.domain.repository.SplitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DebtReminderItem(
    val id: String,
    val groupId: String,
    val groupName: String,
    val billId: String,
    val billName: String,
    val billDate: Long,
    val memberId: String,
    val memberName: String,
    val memberInitial: String,
    val amount: Double,
    val isSelected: Boolean = true,
)

data class DebtReminderUiState(
    val items: List<DebtReminderItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val selectedCount: Int
        get() = items.count { it.isSelected }

    val selectedTotal: Double
        get() = items.filter { it.isSelected }.sumOf { it.amount }
}

class DebtReminderViewModel(
    private val splitRepository: SplitRepository,
    private val notificationRepository: NotificationRepository,
    private val currentUserId: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DebtReminderUiState())
    val uiState: StateFlow<DebtReminderUiState> = _uiState.asStateFlow()

    init {
        observeDebts()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeDebts() {
        viewModelScope.launch {
            runCatching {
                splitRepository.getGroups()
                    .flatMapLatest { groups ->
                        if (groups.isEmpty()) {
                            flowOf(emptyList())
                        } else {
                            combineDebtSources(groups)
                        }
                    }
                    .collect { items ->
                        val previousSelection = _uiState.value.items.associate { it.id to it.isSelected }
                        _uiState.update {
                            it.copy(
                                items = items.map { item ->
                                    item.copy(isSelected = previousSelection[item.id] ?: true)
                                },
                                isLoading = false,
                                error = null,
                            )
                        }
                    }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Không thể tải danh sách nhắc nợ",
                    )
                }
            }
        }
    }

    private fun combineDebtSources(groups: List<Group>): Flow<List<DebtReminderItem>> {
        val flows =
            groups.map { group ->
                combine(
                    splitRepository.getBills(group.id),
                    splitRepository.getGroupMembers(group.id),
                ) { bills, members ->
                    buildReminderItems(group, bills, members)
                }
            }
        return combine(flows) { itemGroups ->
            itemGroups
                .flatMap { it }
                .sortedWith(
                    compareByDescending<DebtReminderItem> { it.billDate }
                        .thenBy { it.groupName }
                        .thenBy { it.memberName },
                )
        }
    }

    private fun buildReminderItems(
        group: Group,
        bills: List<Bill>,
        members: List<Member>,
    ): List<DebtReminderItem> {
        val userId = currentUserId.orEmpty()
        if (userId.isBlank()) return emptyList()
        val memberById = members.associateBy { it.id }

        return bills
            .filter { bill -> bill.payerId == userId }
            .flatMap { bill ->
                bill.shares
                    .filter { (memberId, amount) ->
                        memberId != userId &&
                            amount > 0.0 &&
                            memberId !in bill.paidMemberIds
                    }
                    .map { (memberId, amount) ->
                        val member = memberById[memberId]
                        val name = member?.name ?: memberId
                        DebtReminderItem(
                            id = "${bill.id}_$memberId",
                            groupId = group.id,
                            groupName = group.name,
                            billId = bill.id,
                            billName = bill.name,
                            billDate = bill.date,
                            memberId = memberId,
                            memberName = name,
                            memberInitial = member?.initial ?: name.firstOrNull()?.uppercase().orEmpty(),
                            amount = amount,
                        )
                    }
            }
    }

    fun toggleItem(itemId: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) item.copy(isSelected = !item.isSelected) else item
                },
            )
        }
    }

    fun selectAll() {
        _uiState.update { state -> state.copy(items = state.items.map { it.copy(isSelected = true) }) }
    }

    fun clearSelection() {
        _uiState.update { state -> state.copy(items = state.items.map { it.copy(isSelected = false) }) }
    }

    fun sendSelectedReminders() {
        val state = _uiState.value
        if (state.isSending) return
        val selectedItems = state.items.filter { it.isSelected }
        val senderId = currentUserId.orEmpty()
        if (senderId.isBlank()) {
            _uiState.update { it.copy(message = "Bạn cần đăng nhập để nhắc nợ") }
            return
        }
        if (selectedItems.isEmpty()) {
            _uiState.update { it.copy(message = "Chọn ít nhất một khoản nợ để nhắc") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, message = null) }
            val result =
                runCatching {
                    val now = System.currentTimeMillis()
                    selectedItems.forEachIndexed { index, item ->
                        notificationRepository.insertNotification(
                            Notification(
                                id = "${now}_${index}_${item.billId}_${item.memberId}_selected_reminder",
                                userId = item.memberId,
                                title = "Bạn được nhắc thanh toán",
                                subtitle = "${item.billName} - ${formatReminderAmount(item.amount)} đ",
                                type = NotificationType.PAYMENT_PENDING,
                                relatedId = item.billId,
                                isRead = false,
                                createdAt = now + index,
                                updatedAt = now + index,
                                deepLinkDestination = "SPLIT_DETAIL",
                                deepLinkTargetId = item.billId,
                                senderId = senderId,
                                groupId = item.groupId,
                            )
                        )
                    }
                    selectedItems.size
                }

            _uiState.update {
                it.copy(
                    isSending = false,
                    message =
                        result.fold(
                            onSuccess = { count -> "Đã gửi $count lời nhắc thanh toán" },
                            onFailure = { throwable -> throwable.message ?: "Không thể gửi nhắc nợ" },
                        ),
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

@Composable
fun DebtReminderScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel =
        remember {
            DebtReminderViewModel(
                splitRepository = AppContainer.splitRepository(context),
                notificationRepository = AppContainer.notificationRepository(context),
                currentUserId = FirebaseProviders.auth.currentUser?.uid,
            )
        }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DebtReminderTopBar(onBack = onBack)
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.items.isNotEmpty()) {
                DebtReminderBottomBar(
                    selectedCount = uiState.selectedCount,
                    totalCount = uiState.items.size,
                    selectedTotal = uiState.selectedTotal,
                    isSending = uiState.isSending,
                    onSend = viewModel::sendSelectedReminders,
                )
            }
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimens.spaceLg),
                    contentAlignment = Alignment.Center,
                ) {
                    ReminderEmptyCard(
                        title = "Không thể tải danh sách",
                        message = uiState.error.orEmpty(),
                    )
                }
            }

            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimens.spaceLg),
                    contentAlignment = Alignment.Center,
                ) {
                    ReminderEmptyCard(
                        title = "Không có khoản cần nhắc",
                        message = "Hiện chưa có hóa đơn nào mà người khác còn nợ bạn.",
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(AppDimens.spaceLg),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    item {
                        ReminderSummaryCard(
                            totalCount = uiState.items.size,
                            selectedCount = uiState.selectedCount,
                            selectedTotal = uiState.selectedTotal,
                            onSelectAll = viewModel::selectAll,
                            onClearSelection = viewModel::clearSelection,
                        )
                    }
                    items(uiState.items, key = { it.id }) { item ->
                        ReminderDebtCard(
                            item = item,
                            onToggle = { viewModel.toggleItem(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtReminderTopBar(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
        }
        Text(
            text = "Nhắc nợ",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReminderSummaryCard(
    totalCount: Int,
    selectedCount: Int,
    selectedTotal: Double,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceLg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Khoản đang nợ bạn", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("$selectedCount/$totalCount khoản đã chọn", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "${formatReminderAmount(selectedTotal)} đ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                SecondaryButton(text = "Chọn tất cả", onClick = onSelectAll, modifier = Modifier.weight(1f))
                SecondaryButton(text = "Bỏ chọn", onClick = onClearSelection, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReminderDebtCard(
    item: DebtReminderItem,
    onToggle: () -> Unit,
) {
    AppCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            Checkbox(checked = item.isSelected, onCheckedChange = { onToggle() })
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.memberInitial, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.memberName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.billName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.groupName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${formatReminderAmount(item.amount)} đ",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.secondary,
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = AppShapes.small,
                ) {
                    Text(
                        "CHƯA TRẢ",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtReminderBottomBar(
    selectedCount: Int,
    totalCount: Int,
    selectedTotal: Double,
    isSending: Boolean,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$selectedCount khoản", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Text("${formatReminderAmount(selectedTotal)} đ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.secondary)
            }
            PrimaryButton(
                text = when {
                    isSending -> "Đang gửi..."
                    selectedCount == totalCount -> "Nhắc tất cả"
                    else -> "Nhắc nợ đã chọn"
                },
                onClick = onSend,
                enabled = selectedCount > 0 && !isSending,
                isLoading = isSending,
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}

@Composable
private fun ReminderEmptyCard(
    title: String,
    message: String,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceLg),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(44.dp))
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatReminderAmount(amount: Double): String {
    return "%,.0f".format(amount).replace(",", ".")
}
