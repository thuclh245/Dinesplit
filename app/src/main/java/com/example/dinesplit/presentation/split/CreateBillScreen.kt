package com.example.dinesplit.presentation.split

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dinesplit.core.common.AppContainer
import com.example.dinesplit.domain.model.BillItem
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.SplitMethod
import kotlinx.coroutines.launch

@Composable
fun CreateBillScreen(
    onBack: () -> Unit,
    groupId: String = "g1",
    viewModel: CreateBillViewModel? = null
) {
    val context = LocalContext.current
    val vm = viewModel ?: remember(groupId) {
        CreateBillViewModel(repository = AppContainer.splitRepository(context), groupId = groupId)
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
                isLoading = uiState.isLoading
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 132.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (uiState.isUsingFallbackMembers) {
                    item { FallbackMembersNotice() }
                }

                item {
                    CreateBillMainInfoCard(
                        billName = uiState.billName,
                        onNameChange = vm::onBillNameChange,
                        totalAmount = uiState.totalAmountStr,
                        onTotalAmountChange = vm::onTotalAmountChange,
                        showTotalAmount = uiState.selectedMethod != SplitMethod.ITEMIZED
                    )
                }
                item {
                    CreateBillPayerSection(
                        members = uiState.members,
                        currentPayerId = uiState.payerId,
                        onSelectPayer = vm::setPayer
                    )
                }
                item {
                    CreateBillSplitMethodTabs(
                        selectedMethod = uiState.selectedMethod,
                        onMethodSelect = vm::onMethodSelect
                    )
                }
                item {
                    when (uiState.selectedMethod) {
                        SplitMethod.EQUAL -> EqualSplitDetailsList(
                            members = uiState.members,
                            selectedIds = uiState.selectedMemberIds,
                            payerId = uiState.payerId,
                            onToggle = vm::toggleMemberSelection,
                            onSelectPayer = vm::setPayer
                        )

                        SplitMethod.CUSTOM -> CustomSplitDetailsList(
                            members = uiState.members,
                            selectedIds = uiState.selectedMemberIds,
                            customAmounts = vm.customAmounts,
                            onToggle = vm::toggleMemberSelection,
                            onAmountChange = vm::onCustomAmountChange
                        )

                        SplitMethod.ITEMIZED -> ItemizedSplitDetailsList(
                            members = uiState.members,
                            billItems = vm.billItems,
                            onAddItem = vm::addItem,
                            onRemoveItem = vm::removeItem,
                            onUpdateItem = vm::updateItem
                        )
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                CreateBillBottomAction(
                    isLoading = uiState.isLoading,
                    onConfirm = vm::saveBill
                )
            }
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
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
    isLoading: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.96f))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = colorScheme.primary)
        }

        Text(
            text = "Tạo hóa đơn",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.primary
        )

        Text(
            text = "Lưu",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) colorScheme.outline else colorScheme.primary,
            modifier = Modifier.clickable(enabled = !isLoading) { onSave() }
        )
    }
}

@Composable
private fun FallbackMembersNotice() {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Nhóm cũ chưa có danh sách thành viên thật, app đang dùng danh sách mặc định để tiếp tục tạo hóa đơn.",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
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
    showTotalAmount: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (billName.isEmpty()) {
                        Text("Tên hóa đơn (VD: Lẩu Haidilao)", color = colorScheme.outline, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    BasicTextField(
                        value = billName,
                        onValueChange = onNameChange,
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (showTotalAmount) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "TỔNG CỘNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = totalAmount,
                    onValueChange = onTotalAmountChange,
                    textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Hôm nay", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CreateBillPayerSection(
    members: List<Member>,
    currentPayerId: String,
    onSelectPayer: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val payer = members.find { it.id == currentPayerId } ?: members.firstOrNull()

    Column {
        Text(
            text = "NGƯỜI THANH TOÁN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(2.dp, colorScheme.primaryContainer, CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(payer?.initial ?: "-", color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(payer?.name ?: "Không có", fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                        Text("Trả toàn bộ hóa đơn", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
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
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateBillSplitMethodTabs(
    selectedMethod: SplitMethod,
    onMethodSelect: (SplitMethod) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val tabs = listOf(
        SplitMethod.EQUAL to "Chia đều",
        SplitMethod.CUSTOM to "Tự nhập",
        SplitMethod.ITEMIZED to "Theo món"
    )

    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { (method, label) ->
                val selected = method == selectedMethod
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (selected) {
                                Modifier.background(
                                    brush = Brush.verticalGradient(listOf(colorScheme.primaryContainer, colorScheme.primary)),
                                    shape = RoundedCornerShape(50)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onMethodSelect(method) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
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
    onSelectPayer: (String) -> Unit
) {
    SplitMemberListCard(
        members = members,
        selectedIds = selectedIds,
        payerId = payerId,
        onToggle = onToggle,
        onSelectPayer = onSelectPayer
    )
}

@Composable
private fun CustomSplitDetailsList(
    members: List<Member>,
    selectedIds: Set<String>,
    customAmounts: Map<String, String>,
    onToggle: (String) -> Unit,
    onAmountChange: (String, String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            members.forEach { member ->
                val included = selectedIds.contains(member.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onToggle(member.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarBubble(member = member, selected = included)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(member.name, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            Text(if (included) "Tính vào hóa đơn" else "Không tham gia", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(104.dp)
                            .background(colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (customAmounts[member.id].isNullOrEmpty()) {
                            Text("0 đ", fontSize = 14.sp, color = colorScheme.outline)
                        }
                        BasicTextField(
                            value = customAmounts[member.id].orEmpty(),
                            onValueChange = { onAmountChange(member.id, it) },
                            enabled = included,
                            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
    onUpdateItem: (BillItem) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            billItems.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = item.name,
                            onValueChange = { onUpdateItem(item.copy(name = it)) },
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = if (item.price <= 0.0) "" else item.price.toLong().toString(),
                            onValueChange = { value -> onUpdateItem(item.copy(price = value.toDoubleOrNull() ?: 0.0)) },
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary),
                            modifier = Modifier.width(96.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { member ->
                            val selected = item.sharedByMemberIds.contains(member.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) colorScheme.primary else colorScheme.surfaceContainerHigh)
                                    .clickable {
                                        val ids = item.sharedByMemberIds.toMutableList()
                                        if (selected) ids.remove(member.id) else ids.add(member.id)
                                        onUpdateItem(item.copy(sharedByMemberIds = ids))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    member.initial,
                                    color = if (selected) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (billItems.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Xóa món ${index + 1}",
                            color = colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onRemoveItem(item) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onAddItem() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
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
    onSelectPayer: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            members.forEach { member ->
                val included = selectedIds.contains(member.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (member.isMe) colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (member.id == payerId) colorScheme.primary else if (member.isMe) colorScheme.primary else colorScheme.outlineVariant)
                                .clickable { onSelectPayer(member.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(member.initial, color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(member.name, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            Text(
                                text = if (included) "Đang tham gia" else "Không tham gia",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (member.isMe) colorScheme.primary else colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (included) colorScheme.primary else colorScheme.surfaceContainerHigh)
                            .clickable { onToggle(member.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = if (included) colorScheme.surfaceContainerLowest else colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
private fun AvatarBubble(member: Member, selected: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) colorScheme.primary else colorScheme.outlineVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(member.initial, color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CreateBillBottomAction(
    isLoading: Boolean = false,
    onConfirm: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, colorScheme.surface, colorScheme.surface),
                    startY = 0f,
                    endY = 100f
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .navigationBarsPadding()
    ) {
        Button(
            onClick = { if (!isLoading) onConfirm() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(50)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = Brush.horizontalGradient(listOf(colorScheme.primaryContainer, colorScheme.primary))),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = colorScheme.surfaceContainerLowest, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Xác nhận hóa đơn", color = colorScheme.surfaceContainerLowest, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
