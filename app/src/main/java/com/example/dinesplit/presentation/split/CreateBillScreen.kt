package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.dinesplit.core.common.AppContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- MÔ HÌNH DỮ LIỆU TẠM ---
private data class Cb_SplitMember(
    val name: String,
    val initial: String,
    val amount: String,
    val isMe: Boolean
)

@Composable
fun CreateBillScreen(
    onBack: () -> Unit,
    groupId: String = "g1",
    viewModel: CreateBillViewModel? = null
) {
    val context = LocalContext.current
    val vm = viewModel ?: remember { CreateBillViewModel(repository = AppContainer.splitRepository(context), groupId = groupId) }
    val colorScheme = MaterialTheme.colorScheme
    var billName by remember { mutableStateOf("") }

    val uiState by vm.uiState.collectAsState()
    val members = uiState.members
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = colorScheme.surface,
        topBar = { Cb_TopBar(onBack = onBack) },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
        // KHÔNG DÙNG bottomBar NỮA ĐỂ KHÔNG BỊ LỖI CUỘN
    ) { paddingValues ->

        // Dùng Box để xếp chồng nút Xác nhận lên trên LazyColumn
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // LỚP DƯỚI: Danh sách có thể cuộn
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { Cb_MainInfoCard(billName = uiState.billName, onNameChange = { vm.onBillNameChange(it) }, totalAmount = uiState.totalAmountStr, onTotalAmountChange = { vm.onTotalAmountChange(it) }) }
                item { Cb_PayerSection(members = members, currentPayerId = uiState.payerId, onSelectPayer = { vm.setPayer(it) }) }
                item { Cb_SplitMethodTabs() }
                item { Cb_SplitDetailsList(members = members, selectedIds = uiState.selectedMemberIds, payerId = uiState.payerId, onToggle = { vm.toggleMemberSelection(it) }, onSelectPayer = { vm.setPayer(it) }) }
            }

            // LỚP TRÊN: Nút bấm nổi lơ lửng ở dưới cùng
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    Cb_BottomAction(
                        isLoading = uiState.isLoading,
                        onConfirm = { vm.saveBill() }
                    )
            }
        }
    }

        LaunchedEffect(uiState.isSaved) {
            if (uiState.isSaved) onBack()
        }

        LaunchedEffect(uiState.error) {
            uiState.error?.let { err ->
                coroutineScope.launch { snackbarHostState.showSnackbar(err) }
            }
        }
}

// --- CÁC COMPONENT GIAO DIỆN ---

@Composable
private fun Cb_TopBar(onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
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
            color = colorScheme.primary,
            modifier = Modifier.clickable { /* Xử lý lưu nháp */ }
        )
    }
}

@Composable
private fun Cb_MainInfoCard(billName: String, onNameChange: (String) -> Unit, totalAmount: String, onTotalAmountChange: (String) -> Unit) {
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "TỔNG CỘNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = totalAmount,
                onValueChange = onTotalAmountChange,
                textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Hôm nay, 10 Tháng 4", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Cb_PayerSection(members: List<com.example.dinesplit.domain.model.Member>, currentPayerId: String, onSelectPayer: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
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
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { expanded = true }) {
                    val payer = members.find { it.id == currentPayerId } ?: members.firstOrNull()
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(2.dp, colorScheme.primaryContainer, CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(colorScheme.onSurfaceVariant),
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
                members.forEach { m ->
                    DropdownMenuItem(text = { Text(m.name) }, onClick = {
                        onSelectPayer(m.id)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
private fun Cb_SplitMethodTabs() {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        brush = Brush.verticalGradient(listOf(colorScheme.primaryContainer, colorScheme.primary)),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Chia đều", color = colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("Tự nhập", color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("Theo món", color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Cb_SplitDetailsList(
    members: List<com.example.dinesplit.domain.model.Member>,
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
                                text = "${if (included) "Đang tham gia" else "Không tham gia"}",
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surface)
                    .clickable { /* Mở modal thêm người */ }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thêm người tham gia", color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun Cb_BottomAction(isLoading: Boolean = false, onConfirm: () -> Unit) {
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
                    Text(
                        text = "Xác nhận hóa đơn",
                        color = colorScheme.surfaceContainerLowest,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
