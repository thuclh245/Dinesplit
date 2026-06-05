package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.OutlinedAppCard
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.core.firebase.FirebaseProviders
import com.example.dinesplit.core.ui.DineAvatarImage
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.BillItem
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.SplitMethod
import kotlinx.coroutines.launch

@Composable
fun CreateBillScreen(
    onBack: () -> Unit,
    groupId: String = "g1",
    billId: String? = null,
    viewModel: CreateBillViewModel? = null,
    onBillSavedForPersonal: (Bill) -> Unit = {},
) {
    val context = LocalContext.current
    val vm =
        viewModel ?: remember(groupId, billId) {
            CreateBillViewModel(
                repository = AppContainer.splitRepository(context),
                groupId = groupId,
                currentUserId = FirebaseProviders.auth.currentUser?.uid,
                editBillId = billId,
            )
        }
    val uiState by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CreateBillTopBar(
                onBack = onBack,
                onSave = vm::saveBill,
                isLoading = uiState.isLoading,
                isEditMode = uiState.isEditMode || !billId.isNullOrBlank(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = AppDimens.spaceXl, end = AppDimens.spaceXl, top = AppDimens.spaceLg, bottom = 132.dp),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXl),
            ) {
                if (uiState.isUsingFallbackMembers) {
                    item { FallbackMembersNotice() }
                }

                item {
                    val itemizedTotal = vm.billItems.sumOf { it.price }.toLong().toString()
                    CreateBillMainInfoCard(
                        billName = uiState.billName,
                        onNameChange = vm::onBillNameChange,
                        totalAmount =
                            if (uiState.selectedMethod == SplitMethod.ITEMIZED) {
                                itemizedTotal
                            } else {
                                uiState.totalAmountStr
                            },
                        onTotalAmountChange = vm::onTotalAmountChange,
                        isTotalAmountEditable = uiState.selectedMethod != SplitMethod.ITEMIZED,
                    )
                }
                item {
                    CreateBillPayerSection(
                        members = uiState.members,
                        currentPayerId = uiState.payerId,
                        onSelectPayer = vm::setPayer,
                    )
                }
                item {
                    CreateBillSplitMethodTabs(
                        selectedMethod = uiState.selectedMethod,
                        onMethodSelect = vm::onMethodSelect,
                    )
                }
                item {
                    when (uiState.selectedMethod) {
                        SplitMethod.EQUAL ->
                            EqualSplitDetailsList(
                                members = uiState.members,
                                selectedIds = uiState.selectedMemberIds,
                                payerId = uiState.payerId,
                                onToggle = vm::toggleMemberSelection,
                                onSelectPayer = vm::setPayer,
                            )

                        SplitMethod.CUSTOM ->
                            CustomSplitDetailsList(
                                members = uiState.members,
                                selectedIds = uiState.selectedMemberIds,
                                customAmounts = vm.customAmounts,
                                onToggle = vm::toggleMemberSelection,
                                onAmountChange = vm::onCustomAmountChange,
                            )

                        SplitMethod.ITEMIZED ->
                            ItemizedSplitDetailsList(
                                members = uiState.members,
                                billItems = vm.billItems,
                                onAddItem = vm::addItem,
                                onRemoveItem = vm::removeItem,
                                onUpdateItem = vm::updateItem,
                            )
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                CreateBillBottomAction(
                    isLoading = uiState.isLoading,
                    isEditMode = uiState.isEditMode || !billId.isNullOrBlank(),
                    onConfirm = vm::saveBill,
                )
            }
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            uiState.savedBill?.let(onBillSavedForPersonal)
            onBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            coroutineScope.launch { snackbarHostState.showSnackbar(error) }
        }
    }
}

@Composable
private fun CreateBillTopBar(
    onBack: () -> Unit,
    onSave: () -> Unit,
    isLoading: Boolean,
    isEditMode: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.96f))
                .padding(horizontal = AppDimens.spaceXl, vertical = AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(AppDimens.space2Xl)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = colorScheme.primary)
        }

        Text(
            text = if (isEditMode) "Sửa hóa đơn" else "Tạo hóa đơn",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.primary,
        )

        Text(
            text = "Lưu",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) colorScheme.outline else colorScheme.primary,
            modifier = Modifier.clickable(enabled = !isLoading) { onSave() },
        )
    }
}

@Composable
private fun FallbackMembersNotice() {
    val colorScheme = MaterialTheme.colorScheme
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceLg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
            Text(
                text = "Nhóm cũ chưa có danh sách thành viên thật, app đang dùng danh sách mặc định để tiếp tục tạo hóa đơn.",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreateBillMainInfoCard(
    billName: String,
    onNameChange: (String) -> Unit,
    totalAmount: String,
    onTotalAmountChange: (String) -> Unit,
    isTotalAmountEditable: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.padding(AppDimens.spaceLg)) {
            AppTextField(
                value = billName,
                onValueChange = onNameChange,
                label = "",
                placeholder = "Tên hóa đơn (VD: Lẩu Haidilao)",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (isTotalAmountEditable) {
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                Text(
                    text = "TỔNG CỘNG (đ)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                AppTextField(
                    value = formatCurrencyInput(totalAmount),
                    onValueChange = onTotalAmountChange,
                    label = "",
                    placeholder = "0",
                    singleLine = true,
                    maxLines = 1,
                    textStyle = MaterialTheme.typography.displaySmall.copy(color = colorScheme.primary, fontWeight = FontWeight.ExtraBold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
                Text(
                    text = "TỔNG CỘNG (đ)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                Text(
                    text = formatCurrencyInput(totalAmount).ifBlank { "0" },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(start = AppDimens.spaceXs),
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .background(colorScheme.surfaceContainerLow, AppShapes.medium)
                        .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceSm),
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(AppDimens.spaceLg),
                )
                Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                Text(text = "Hôm nay", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CreateBillPayerSection(
    members: List<Member>,
    currentPayerId: String,
    onSelectPayer: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val payer = members.find { it.id == currentPayerId } ?: members.firstOrNull()

    Column {
        Text(
            text = "NGƯỜI THANH TOÁN",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = AppDimens.spaceXs, bottom = AppDimens.spaceSm),
        )

        OutlinedAppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .padding(AppDimens.spaceLg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .border(2.dp, colorScheme.primaryContainer, CircleShape)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(payer?.initial ?: "-", color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(payer?.name ?: "Không có", fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                        Text("Trả toàn bộ hóa đơn", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Đổi người", tint = colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                members.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member.name) },
                        onClick = {
                            onSelectPayer(member.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateBillSplitMethodTabs(
    selectedMethod: SplitMethod,
    onMethodSelect: (SplitMethod) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val tabs =
        listOf(
            SplitMethod.EQUAL to "Chia đều",
            SplitMethod.CUSTOM to "Tự nhập",
            SplitMethod.ITEMIZED to "Theo món",
        )

    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = AppShapes.full,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(AppDimens.spaceXs), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { (method, label) ->
                val selected = method == selectedMethod
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .then(
                                if (selected) {
                                    Modifier.background(
                                        brush = Brush.verticalGradient(listOf(colorScheme.primaryContainer, colorScheme.primary)),
                                        shape = AppShapes.full,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onMethodSelect(method) }
                            .padding(vertical = AppDimens.spaceSm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (selected) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualSplitDetailsList(
    members: List<Member>,
    selectedIds: Set<String>,
    payerId: String,
    onToggle: (String) -> Unit,
    onSelectPayer: (String) -> Unit,
) {
    SplitMemberListCard(
        members = members,
        selectedIds = selectedIds,
        payerId = payerId,
        onToggle = onToggle,
        onSelectPayer = onSelectPayer,
    )
}



@Composable
private fun CustomSplitDetailsList(
    members: List<Member>,
    selectedIds: Set<String>,
    customAmounts: Map<String, String>,
    onToggle: (String) -> Unit,
    onAmountChange: (String, String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            members.forEach { member ->
                val included = selectedIds.contains(member.id)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spaceLg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clickable { onToggle(member.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarBubble(member = member, selected = included)
                        Spacer(modifier = Modifier.width(AppDimens.spaceMd))
                        Column {
                            Text(member.name, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            Text(
                                if (included) "Tính vào hóa đơn" else "Không tham gia",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val customAmount = customAmounts[member.id].orEmpty()
                    AppTextField(
                        value = formatCurrencyInput(customAmount),
                        onValueChange = { onAmountChange(member.id, it) },
                        label = "",
                        placeholder = "0 đ",
                        enabled = included,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(104.dp),
                        minHeight = 40.dp
                    )
                }
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun ItemizedSplitDetailsList(
    members: List<Member>,
    billItems: List<BillItem>,
    onAddItem: () -> Unit,
    onRemoveItem: (BillItem) -> Unit,
    onUpdateItem: (BillItem) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.padding(AppDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            billItems.forEachIndexed { index, item ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colorScheme.surfaceContainerLow, AppShapes.large)
                            .padding(AppDimens.spaceLg),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                    ) {
                        AppTextField(
                            value = item.name,
                            onValueChange = { onUpdateItem(item.copy(name = it)) },
                            label = "",
                            placeholder = "Tên món ăn",
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = colorScheme.onSurface),
                            modifier = Modifier.weight(1f),
                            minHeight = 40.dp
                        )

                        AppTextField(
                            value = if (item.price <= 0.0) "" else formatCurrencyInput(item.price.toLong().toString()),
                            onValueChange = { value ->
                                onUpdateItem(item.copy(price = value.onlyDigits().toDoubleOrNull() ?: 0.0))
                            },
                            label = "",
                            placeholder = "0 đ",
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(112.dp),
                            minHeight = 40.dp
                        )
                    }
                    Text(
                        text = "Nhập giá món và chọn người cùng ăn món này",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = AppDimens.spaceSm),
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        members.forEach { member ->
                            val selected = item.sharedByMemberIds.contains(member.id)
                            Box(
                                modifier =
                                    Modifier
                                        .clip(AppShapes.full)
                                        .background(if (selected) colorScheme.primary else colorScheme.surfaceContainerHigh)
                                        .clickable {
                                            val ids = item.sharedByMemberIds.toMutableList()
                                            if (selected) ids.remove(member.id) else ids.add(member.id)
                                            onUpdateItem(item.copy(sharedByMemberIds = ids))
                                        }
                                        .padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
                            ) {
                                Text(
                                    member.initial,
                                    color = if (selected) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    if (billItems.size > 1) {
                        Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                        Text(
                            text = "Xóa món ${index + 1}",
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onRemoveItem(item) },
                        )
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.large)
                        .clickable { onAddItem() }
                        .padding(AppDimens.spaceLg),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thêm món", color = colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SplitMemberListCard(
    members: List<Member>,
    selectedIds: Set<String>,
    payerId: String,
    onToggle: (String) -> Unit,
    onSelectPayer: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            members.forEach { member ->
                val included = selectedIds.contains(member.id)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(if (member.isMe) colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (member.id == payerId) {
                                            colorScheme.primary
                                        } else if (member.isMe) {
                                            colorScheme.primary
                                        } else {
                                            colorScheme.outlineVariant
                                        },
                                    )
                                    .clickable { onSelectPayer(member.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(member.initial, color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(member.name, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            Text(
                                text = if (included) "Đang tham gia" else "Không tham gia",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (member.isMe) colorScheme.primary else colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (included) colorScheme.primary else colorScheme.surfaceContainerHigh)
                                .clickable { onToggle(member.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (included) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun AvatarBubble(
    member: Member,
    selected: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    DineAvatarImage(
        imageUrl = member.avatarUrl,
        name = member.name.ifBlank { member.initial },
        size = 40.dp,
        fallbackContainerColor = if (selected) colorScheme.primary else colorScheme.outlineVariant,
        fallbackContentColor = colorScheme.surfaceContainerLowest
    )
}

@Composable
private fun CreateBillBottomAction(
    isLoading: Boolean = false,
    isEditMode: Boolean = false,
    onConfirm: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, colorScheme.surface, colorScheme.surface),
                            startY = 0f,
                            endY = 100f,
                        ),
                )
                .padding(horizontal = AppDimens.spaceXl, vertical = AppDimens.spaceXl)
                .navigationBarsPadding(),
    ) {
        PrimaryButton(
            text = if (isEditMode) "Lưu thay đổi" else "Xác nhận hóa đơn",
            onClick = onConfirm,
            enabled = !isLoading,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun formatCurrencyInput(value: String): String {
    val digits = value.onlyDigits()
    if (digits.isEmpty()) return ""

    val normalized = digits.trimStart('0').ifEmpty { "0" }
    return normalized
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}

private fun String.onlyDigits(): String {
    return filter { it.isDigit() }
}
