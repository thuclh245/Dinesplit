package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.ElevatedAppCard
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.core.ui.TertiaryButton
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.domain.model.QrPayment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillItem
import com.example.dinesplit.domain.model.BillStatus
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.PaymentStatus
import com.example.dinesplit.domain.model.QrPaymentStatus
import com.example.dinesplit.domain.model.SplitMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class BillSplitRow(
    val memberId: String,
    val name: String,
    val initial: String,
    val amount: Double,
    val paymentStatus: PaymentStatus,
    val isMe: Boolean,
    val pendingPaymentId: String? = null,
    val paymentRequestStatus: String? = null,
    val canConfirmPayment: Boolean = false,
    val canSendReminder: Boolean = false,
) {
    val isPayer: Boolean
        get() = paymentStatus == PaymentStatus.PAYER

    val isPaid: Boolean
        get() = paymentStatus == PaymentStatus.PAYER || paymentStatus == PaymentStatus.PAID
}

@Composable
fun BillDetailScreen(
    groupId: String,
    billId: String,
    onBack: () -> Unit,
    onEditBill: (groupId: String, billId: String) -> Unit = { _, _ -> },
    onBillChangedForPersonal: (Bill) -> Unit = {},
    onBillRemovedForPersonal: (groupId: String, billId: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val viewModel =
        remember(groupId, billId) {
            BillDetailViewModel(
                repository = AppContainer.splitRepository(context),
                notificationRepository = AppContainer.notificationRepository(context),
                qrPaymentRepository = AppContainer.qrPaymentRepository(context),
                groupId = groupId,
                billId = billId,
                currentUserId = FirebaseProviders.auth.currentUser?.uid,
            )
        }
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.bill) {
        uiState.bill?.let(onBillChangedForPersonal)
    }

    LaunchedEffect(uiState.isLoading, uiState.bill, groupId, billId) {
        if (!uiState.isLoading && uiState.bill == null && !uiState.isUnauthorized) {
            onBillRemovedForPersonal(groupId, billId)
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBillRemovedForPersonal(groupId, billId)
            onBack()
        }
    }

    LaunchedEffect(uiState.paymentMessage) {
        uiState.paymentMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumePaymentMessage()
        }
    }

    val activeQrPayment = uiState.activeQrPayment
    if (activeQrPayment != null) {
        val bill = uiState.bill
        val payerName = resolveMemberName(uiState.bill?.payerId.orEmpty(), uiState.members)
        QrPaymentDialog(
            payment = activeQrPayment,
            payerName = payerName,
            bankCode = bill?.paymentQrBankCode.orEmpty(),
            accountNumber = bill?.paymentQrAccountNumber.orEmpty(),
            accountName = bill?.paymentQrAccountName.orEmpty(),
            onCancel = viewModel::cancelQrPayment,
            onMarkTransferred = viewModel::markActivePaymentTransferred
        )
    }
    Scaffold(
        containerColor = colorScheme.surface,
        topBar = {
            BdTopBar(
                onBack = onBack,
                canManageBill = uiState.canManageBill,
                isDeleting = uiState.isDeleting,
                onEdit = { onEditBill(groupId, billId) },
                onDelete = { showDeleteDialog = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            uiState.bill?.let { bill ->
                val myShare = bill.shares[uiState.currentMemberId] ?: 0.0
                val myPaymentStatus =
                    uiState.qrPayments
                        .filter { payment -> payment.payerUid == uiState.currentMemberId && payment.billId == bill.id }
                        .maxByOrNull { payment -> payment.updatedAt?.time ?: payment.createdAt?.time ?: 0L }
                        ?.status
                BdBottomAction(
                    payerName = resolveMemberName(bill.payerId, uiState.members),
                    currentMemberId = uiState.currentMemberId,
                    isCurrentMemberPayer = uiState.currentMemberId == bill.payerId,
                    isCurrentMemberPaid = bill.paidMemberIds.contains(uiState.currentMemberId),
                    paymentRequestStatus = myPaymentStatus,
                    hasPaymentQr = bill.hasPaymentQr,
                    isUpdating = uiState.isUpdatingPayment,
                    onPayWithQr = {
                        viewModel.initiateQrPayment(myShare, bill.payerId)
                    }
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = AppDimens.spaceXl),
            contentPadding = PaddingValues(top = AppDimens.spaceLg, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXl),
        ) {
            when {
                uiState.isLoading ->
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = AppDimens.space4Xl),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                uiState.error != null ->
                    item {
                        BdMessageCard(
                            title = if (uiState.isUnauthorized) "Không có quyền truy cập" else "Không thể tải hóa đơn",
                            message = uiState.error.orEmpty(),
                        )
                    }

                uiState.bill != null -> {
                    val bill = uiState.bill!!
                    val splitRows = buildSplitRows(
                        bill = bill,
                        members = uiState.members,
                        payments = uiState.qrPayments,
                        currentUserId = uiState.currentMemberId,
                    )

                    item {
                        BdReceiptHeaderCard(
                            bill = bill,
                            payerName = resolveMemberName(bill.payerId, uiState.members),
                            payerInitial = resolveMemberInitial(bill.payerId, uiState.members),
                        )
                    }
                    item {
                        BdSplitBreakdown(
                            rows = splitRows,
                            onConfirmPayment = viewModel::confirmQrPayment,
                            onRejectPayment = viewModel::rejectQrPayment,
                            onSendReminder = viewModel::sendPaymentReminder
                        )
                    }
                    if (bill.items.isNotEmpty()) {
                        item { BdItemBreakdown(items = bill.items) }
                    }
                    item { BdFooterInfo(bill = bill) }
                }
            }

        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) showDeleteDialog = false },
            title = { Text("Xóa hóa đơn?") },
            text = { Text("Hành động này sẽ xóa bill khỏi nhóm và gỡ khoản chi đồng bộ trong Ví DineSplit.") },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isDeleting,
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteBill()
                    },
                ) {
                    Text("Xóa", color = colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isDeleting,
                    onClick = { showDeleteDialog = false },
                ) {
                    Text("Hủy")
                }
            },
        )
    }
}

@Composable
private fun BdTopBar(
    onBack: () -> Unit,
    canManageBill: Boolean,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.98f))
                .padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colorScheme.primary,
            )
        }

        Text(
            text = "Chi tiết hóa đơn",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.onSurface,
        )

        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (canManageBill) {
                IconButton(
                    onClick = { menuExpanded = true },
                    enabled = !isDeleting,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = colorScheme.primary,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Sửa hóa đơn") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa hóa đơn", color = colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BdReceiptHeaderCard(
    bill: Bill,
    payerName: String,
    payerInitial: String,
) {
    val colorScheme = MaterialTheme.colorScheme

    ElevatedAppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.spaceXl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(AppDimens.iconLg),
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            Text(
                text = bill.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            Text(
                text = formatDate(bill.date),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(AppDimens.spaceXl))

            Text(
                text = "${formatAmount(bill.totalAmount)} đ",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(AppDimens.spaceXl))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .background(colorScheme.surfaceContainerLow, AppShapes.full)
                        .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(AppDimens.spaceXl)
                            .clip(CircleShape)
                            .background(colorScheme.onSurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = payerInitial,
                        color = colorScheme.surfaceContainerLowest,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                Text(
                    text = "Thanh toán bởi ",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurfaceVariant,
                )
                Text(
                    text = payerName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.space2Xl))

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BdSplitBreakdown(
    rows: List<BillSplitRow>,
    onConfirmPayment: (String) -> Unit,
    onRejectPayment: (String) -> Unit,
    onSendReminder: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "CHI TIẾT CHIA TIỀN",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = AppDimens.spaceSm, bottom = AppDimens.spaceMd),
        )

        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    BdSplitRow(
                        row = row,
                        onConfirmPayment = onConfirmPayment,
                        onRejectPayment = onRejectPayment,
                        onSendReminder = onSendReminder
                    )
                    if (index < rows.size - 1) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BdSplitRow(
    row: BillSplitRow,
    onConfirmPayment: (String) -> Unit,
    onRejectPayment: (String) -> Unit,
    onSendReminder: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var menuExpanded by remember(row.memberId, row.paymentStatus, row.paymentRequestStatus) { mutableStateOf(false) }
    val canOpenPaymentActions = row.canConfirmPayment || row.canSendReminder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (row.isMe) colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(enabled = canOpenPaymentActions) { menuExpanded = true }
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier =
                Modifier
                    .width(AppDimens.spaceXs)
                    .fillMaxHeight()
                    .background(if (row.isMe) colorScheme.primary else Color.Transparent),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (row.isMe) colorScheme.primary else colorScheme.outlineVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = row.initial,
                        color = colorScheme.surfaceContainerLowest,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.spaceMd))
                Column {
                    Text(
                        text = row.name,
                        fontWeight = FontWeight.Bold,
                        color = if (row.isMe) colorScheme.primary else colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (row.isPayer) {
                        Text(
                            text = "CHỦ CHI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.primary,
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatAmount(row.amount)} đ",
                    fontWeight = FontWeight.Bold,
                    color = if (row.isMe) colorScheme.primary else colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))

                if (row.isPaid) {
                    Row(
                        modifier =
                            Modifier
                                .background(colorScheme.secondaryContainer, AppShapes.full)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "ĐÃ TRẢ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.secondary,
                        )
                    }
                } else if (row.paymentRequestStatus == QrPaymentStatus.MARKED_PAID) {
                    Row(
                        modifier =
                            Modifier
                                .background(colorScheme.primaryContainer, AppShapes.full)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CHỜ XÁC NHẬN",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.primary,
                        )
                    }
                } else if (row.paymentRequestStatus == QrPaymentStatus.REJECTED) {
                    Row(
                        modifier =
                            Modifier
                                .background(colorScheme.errorContainer, AppShapes.full)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CẦN KIỂM TRA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.error,
                        )
                    }
                } else {
                    Row(
                        modifier =
                            Modifier
                                .background(colorScheme.surfaceContainerHigh, AppShapes.full)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CHƯA TRẢ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                if (row.canConfirmPayment && row.pendingPaymentId != null) {
                    DropdownMenuItem(
                        text = { Text("Xac nhan da nhan tien") },
                        onClick = {
                            menuExpanded = false
                            onConfirmPayment(row.pendingPaymentId)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tu choi xac nhan", color = colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onRejectPayment(row.pendingPaymentId)
                        }
                    )
                }
                if (row.canSendReminder) {
                    DropdownMenuItem(
                        text = { Text("Nhắc thanh toán") },
                        onClick = {
                            menuExpanded = false
                            onSendReminder(row.memberId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BdItemBreakdown(items: List<BillItem>) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "MÓN ĐÃ CHIA",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = AppDimens.spaceSm, bottom = AppDimens.spaceMd),
        )

        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.spaceLg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val itemMeta =
                                if (item.quantity > 1) {
                                    "${item.quantity} x ${formatAmount(item.unitPrice)} đ · ${item.sharedByMemberIds.size} người chia"
                                } else {
                                    "${item.sharedByMemberIds.size} người chia"
                                }
                            Text(
                                text = itemMeta,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${formatAmount(item.price)} đ",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.primary,
                        )
                    }
                    if (index < items.size - 1) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BdFooterInfo(bill: Bill) {
    val colorScheme = MaterialTheme.colorScheme
    val isSettled = bill.status == BillStatus.SETTLED

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.padding(AppDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mã hóa đơn", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = colorScheme.onSurfaceVariant)
                Text(
                    "#${bill.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    fontFamily = FontFamily.Monospace,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kiểu chia", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = colorScheme.onSurfaceVariant)
                Text(formatSplitMethod(bill.method), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trạng thái", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = colorScheme.onSurfaceVariant)
                Text(
                    text = if (isSettled) "Đã thanh toán" else "Còn mở",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSettled) colorScheme.secondary else colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun BdMessageCard(
    title: String,
    message: String,
) {
    val colorScheme = MaterialTheme.colorScheme

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceLg),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
            Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BdBottomAction(
    payerName: String,
    currentMemberId: String,
    isCurrentMemberPayer: Boolean,
    isCurrentMemberPaid: Boolean,
    paymentRequestStatus: String?,
    hasPaymentQr: Boolean,
    isUpdating: Boolean,
    onPayWithQr: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val hasPendingConfirmation = paymentRequestStatus == QrPaymentStatus.MARKED_PAID
    val canPay = currentMemberId.isNotBlank() && !isCurrentMemberPayer && !isCurrentMemberPaid && !isUpdating && !hasPendingConfirmation && hasPaymentQr

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.96f))
                .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd)
                .navigationBarsPadding(),
    ) {
        if (canPay) {
            PrimaryButton(
                text = "Thanh toán QR",
                onClick = onPayWithQr,
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        } else {
            val label =
                when {
                    isUpdating -> "Đang cập nhật thanh toán..."
                    hasPendingConfirmation -> "Chờ người nhận xác nhận"
                    isCurrentMemberPayer -> "Bạn là người thanh toán"
                    isCurrentMemberPaid -> "Bạn đã trả cho $payerName"
                    !hasPaymentQr -> "Bill chưa có QR nhận tiền"
                    else -> "Không thể thanh toán"
                }

            PrimaryButton(
                text = label,
                onClick = {},
                enabled = false,
                isLoading = isUpdating,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QrPaymentDialog(
    payment: QrPayment,
    payerName: String,
    bankCode: String,
    accountNumber: String,
    accountName: String,
    onCancel: () -> Unit,
    onMarkTransferred: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val qrUrl = payment.qrContent

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quét mã VietQR",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                Text(
                    text = "Thanh toán hóa đơn cho $payerName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(Color.White, AppShapes.medium)
                        .padding(AppDimens.spaceSm),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = qrUrl,
                            contentDescription = "Mã VietQR",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "Chưa có QR nhận tiền",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.error,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(AppDimens.spaceMd),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ngân hàng:", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            Text(bankCode.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Số tài khoản:", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            Text(accountNumber.ifBlank { "-" }, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tên tài khoản:", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            Text(accountName.ifBlank { payerName }, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Số tiền:", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            Text("${formatAmount(payment.amount)} đ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Nội dung:", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            Text(payment.description, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Trạng thái:", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            Text(payment.status, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (payment.status == QrPaymentStatus.CONFIRMED) colorScheme.secondary else colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                Text(
                    text = "Đang chờ hệ thống xác nhận thanh toán...",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            SecondaryButton(
                text = "Tôi đã chuyển khoản",
                onClick = onMarkTransferred,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TertiaryButton(
                text = "Hủy giao dịch",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

private fun buildSplitRows(
    bill: Bill,
    members: List<Member>,
    payments: List<QrPayment>,
    currentUserId: String,
): List<BillSplitRow> {
    val memberById = members.associateBy { it.id }
    val latestPaymentByPayer =
        payments
            .filter { payment -> payment.billId == bill.id }
            .sortedByDescending { payment -> payment.updatedAt?.time ?: payment.createdAt?.time ?: 0L }
            .associateBy { payment -> payment.payerUid }
    val shareMemberIds = bill.shares.keys
    val ids = (shareMemberIds + bill.payerId).filter { it.isNotBlank() }.distinct()

    return ids.map { memberId ->
        val member = memberById[memberId]
        val name = member?.name ?: fallbackMemberName(memberId)
        val latestPayment = latestPaymentByPayer[memberId]
        val isPayer = memberId == bill.payerId
        val isPaid = memberId in bill.paidMemberIds
        BillSplitRow(
            memberId = memberId,
            name = name,
            initial = member?.initial ?: name.firstOrNull()?.uppercase().orEmpty(),
            amount = bill.shares[memberId] ?: 0.0,
            paymentStatus = bill.paymentStatusFor(memberId),
            isMe = member?.isMe ?: (memberId == "me"),
            pendingPaymentId = latestPayment?.id,
            paymentRequestStatus = latestPayment?.status,
            canConfirmPayment = currentUserId == bill.payerId &&
                !isPayer &&
                !isPaid &&
                latestPayment?.status == QrPaymentStatus.MARKED_PAID,
            canSendReminder = currentUserId == bill.payerId &&
                !isPayer &&
                !isPaid &&
                latestPayment?.status != QrPaymentStatus.MARKED_PAID,
        )
    }.sortedWith(compareByDescending<BillSplitRow> { it.isPayer }.thenByDescending { it.isMe })
}

private fun resolveMemberName(
    memberId: String,
    members: List<Member>,
): String {
    return members.firstOrNull { it.id == memberId }?.name
        ?: fallbackMemberName(memberId).ifBlank { "Người thanh toán" }
}

private fun resolveMemberInitial(
    memberId: String,
    members: List<Member>,
): String {
    val member = members.firstOrNull { it.id == memberId }
    return member?.initial ?: resolveMemberName(memberId, members).firstOrNull()?.uppercase().orEmpty()
}

private fun fallbackMemberName(memberId: String): String {
    return when (memberId) {
        "me" -> "Bạn"
        "minh" -> "Minh"
        "thanh_hang" -> "Thanh Hằng"
        "tuan_anh" -> "Tuấn Anh"
        else -> memberId
    }
}

private fun formatSplitMethod(method: SplitMethod): String {
    return when (method) {
        SplitMethod.EQUAL -> "Chia đều"
        SplitMethod.CUSTOM -> "Tự nhập"
        SplitMethod.ITEMIZED -> "Theo món"
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Chưa có ngày"
    return SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(timestamp))
}

private fun formatAmount(amount: Double): String {
    return "%,.0f".format(amount).replace(",", ".")
}
