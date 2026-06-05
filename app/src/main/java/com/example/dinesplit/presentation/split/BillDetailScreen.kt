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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import com.example.dinesplit.domain.model.QrPayment
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.core.ui.TertiaryButton
import androidx.compose.material3.CardDefaults
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
        if (!uiState.isLoading && uiState.bill == null) {
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
        val payerName = resolveMemberName(uiState.bill?.payerId.orEmpty(), uiState.members)
        QrPaymentDialog(
            payment = activeQrPayment,
            payerName = payerName,
            onCancel = viewModel::cancelQrPayment,
            onSimulateSuccess = { viewModel.simulateBankCallback(activeQrPayment.id) }
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
                BdBottomAction(
                    payerName = resolveMemberName(bill.payerId, uiState.members),
                    currentMemberId = uiState.currentMemberId,
                    isCurrentMemberPayer = uiState.currentMemberId == bill.payerId,
                    isCurrentMemberPaid = bill.paidMemberIds.contains(uiState.currentMemberId),
                    isUpdating = uiState.isUpdatingPayment,
                    onMarkPaid = viewModel::markCurrentMemberPaid,
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
                    .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when {
                uiState.isLoading ->
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                uiState.error != null ->
                    item {
                        BdMessageCard(
                            title = "Không thể tải hóa đơn",
                            message = uiState.error.orEmpty(),
                        )
                    }

                uiState.bill != null -> {
                    val bill = uiState.bill!!
                    val splitRows = buildSplitRows(bill, uiState.members)

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
                            onMarkPaid = viewModel::markMemberPaid,
                            onConfirmPayment = viewModel::markMemberPaid,
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
            text = { Text("Hành động này sẽ xóa bill khỏi nhóm và gỡ khoản chi đồng bộ trong Ví cá nhân.") },
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
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
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = bill.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDate(bill.date),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "${formatAmount(bill.totalAmount)} đ",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .background(colorScheme.surfaceContainerLow, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colorScheme.onSurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = payerInitial,
                        color = colorScheme.surfaceContainerLowest,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thanh toán bởi ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurfaceVariant,
                )
                Text(
                    text = payerName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

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
    onMarkPaid: (String) -> Unit,
    onConfirmPayment: (String) -> Unit,
    onSendReminder: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "CHI TIẾT CHIA TIỀN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    BdSplitRow(
                        row = row,
                        onMarkPaid = onMarkPaid,
                        onConfirmPayment = onConfirmPayment,
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
    onMarkPaid: (String) -> Unit,
    onConfirmPayment: (String) -> Unit,
    onSendReminder: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var menuExpanded by remember(row.memberId, row.paymentStatus) { mutableStateOf(false) }
    val canOpenPaymentActions = !row.isPayer && !row.isPaid

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
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (row.isMe) colorScheme.primary else Color.Transparent),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
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
                Spacer(modifier = Modifier.width(12.dp))
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
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
                Spacer(modifier = Modifier.height(4.dp))

                if (row.isPaid) {
                    Row(
                        modifier =
                            Modifier
                                .background(colorScheme.secondaryContainer, RoundedCornerShape(50))
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
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.secondary,
                        )
                    }
                } else {
                    Row(
                        modifier =
                            Modifier
                                .background(colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CHƯA TRẢ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Đánh dấu đã trả") },
                    onClick = {
                        menuExpanded = false
                        onMarkPaid(row.memberId)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Xác nhận thanh toán") },
                    onClick = {
                        menuExpanded = false
                        onConfirmPayment(row.memberId)
                    }
                )
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

@Composable
private fun BdItemBreakdown(items: List<BillItem>) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Text(
            text = "MÓN ĐÃ CHIA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${item.sharedByMemberIds.size} người chia",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${formatAmount(item.price)} đ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mã hóa đơn", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurfaceVariant)
                Text(
                    "#${bill.id.takeLast(6).uppercase()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kiểu chia", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurfaceVariant)
                Text(formatSplitMethod(bill.method), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trạng thái", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurfaceVariant)
                Text(
                    text = if (isSettled) "Đã thanh toán" else "Còn mở",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text(message, fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BdBottomAction(
    payerName: String,
    currentMemberId: String,
    isCurrentMemberPayer: Boolean,
    isCurrentMemberPaid: Boolean,
    isUpdating: Boolean,
    onMarkPaid: () -> Unit,
    onPayWithQr: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val canPay = currentMemberId.isNotBlank() && !isCurrentMemberPayer && !isCurrentMemberPaid && !isUpdating

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.96f))
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .navigationBarsPadding(),
    ) {
        if (canPay) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Manual mark as paid button
                SecondaryButton(
                    text = "Báo đã trả",
                    onClick = onMarkPaid,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                PrimaryButton(
                    text = "Thanh toán QR",
                    onClick = onPayWithQr,
                    modifier = Modifier.weight(1.2f),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        } else {
            val label =
                when {
                    isUpdating -> "Đang cập nhật thanh toán..."
                    isCurrentMemberPayer -> "Bạn là người thanh toán"
                    isCurrentMemberPaid -> "Bạn đã trả cho $payerName"
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
    onCancel: () -> Unit,
    onSimulateSuccess: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val qrUrl = "https://img.vietqr.io/image/MB-1903678999999-compact2.png?amount=${payment.amount.toInt()}&addInfo=${payment.description}&accountName=${payerName}"

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quét mã VietQR",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Thanh toán hóa đơn cho $payerName",
                    fontSize = 14.sp,
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
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = qrUrl,
                        contentDescription = "Mã VietQR",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Số tiền:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                            Text("${formatAmount(payment.amount)} đ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Nội dung:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                            Text(payment.description, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Trạng thái:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                            Text(payment.status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (payment.status == "VERIFIED") colorScheme.secondary else colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Đang chờ hệ thống xác nhận thanh toán...",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            SecondaryButton(
                text = "Giả lập Chuyển khoản thành công",
                onClick = onSimulateSuccess,
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
): List<BillSplitRow> {
    val memberById = members.associateBy { it.id }
    val shareMemberIds = bill.shares.keys
    val ids = (shareMemberIds + bill.payerId).filter { it.isNotBlank() }.distinct()

    return ids.map { memberId ->
        val member = memberById[memberId]
        val name = member?.name ?: fallbackMemberName(memberId)
        BillSplitRow(
            memberId = memberId,
            name = name,
            initial = member?.initial ?: name.firstOrNull()?.uppercase().orEmpty(),
            amount = bill.shares[memberId] ?: 0.0,
            paymentStatus = bill.paymentStatusFor(memberId),
            isMe = member?.isMe ?: (memberId == "me"),
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
