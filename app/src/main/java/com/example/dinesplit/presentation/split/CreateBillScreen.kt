package com.example.dinesplit.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

// --- KHO KHAI BÁO MÀU SẮC (Cb_ prefix) ---
private val Cb_Bg = Color(0xFFF9F9F9)
private val Cb_SurfaceWhite = Color(0xFFFFFFFF)
private val Cb_OrangeStart = Color(0xFFE2725B)
private val Cb_OrangeEnd = Color(0xFF9F402D)
private val Cb_OrangeLightBg = Color(0xFFFFF0ED)
private val Cb_OrangeIconBg = Color(0xFFFFDAD3)
private val Cb_TextMain = Color(0xFF1A1C1C)
private val Cb_TextSub = Color(0xFF56423E)

// --- ENUM & MODELS ---
private enum class Cb_SplitMethod { EQUAL, CUSTOM, ITEMIZED }

private data class Cb_Member(
    val id: String,
    val name: String,
    val initial: String,
    val isMe: Boolean = false
)

private data class Cb_BillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "",
    var price: String = "",
    val sharedByMemberIds: SnapshotStateList<String> = mutableStateListOf()
)

@Composable
fun CreateBillScreen(
    onBack: () -> Unit
) {
    // State form
    var billName by remember { mutableStateOf("") }
    var totalAmountStr by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(Cb_SplitMethod.EQUAL) }
    
    // Members & Items
    val members = remember {
        mutableStateListOf(
            Cb_Member("1", "Bạn", "B", true),
            Cb_Member("2", "Minh", "M"),
            Cb_Member("3", "Sarah Chen", "S")
        )
    }
    val billItems = remember { mutableStateListOf(Cb_BillItem(name = "Món 1")) }
    val customAmounts = remember { mutableStateMapOf<String, String>() }

    val totalAmount: Long = when (selectedMethod) {
        Cb_SplitMethod.EQUAL,
        Cb_SplitMethod.CUSTOM -> totalAmountStr.toLongOrNull() ?: 0L

        Cb_SplitMethod.ITEMIZED -> billItems.sumOf {
            it.price.toLongOrNull() ?: 0L
        }
    }

    val memberIds = members.map { it.id }

    val memberShares: Map<String, Long> = when (selectedMethod) {
        Cb_SplitMethod.EQUAL -> {
            SmartSplitEngine.calculateEqualSplit(
                totalAmount = totalAmount,
                memberIds = memberIds
            )
        }

        Cb_SplitMethod.CUSTOM -> {
            members.associate { member ->
                member.id to (customAmounts[member.id]?.toLongOrNull() ?: 0L)
            }
        }

        Cb_SplitMethod.ITEMIZED -> {
            val items = billItems.map { item ->
                BillItemInput(
                    id = item.id,
                    name = item.name,
                    price = item.price.toLongOrNull() ?: 0L,
                    sharedByMemberIds = item.sharedByMemberIds.toList()
                )
            }

            SmartSplitEngine.calculateItemizedSplit(
                items = items,
                memberIds = memberIds
            )
        }
    }

    val currentTotalCalculated = memberShares.values.sum()
    val remainingAmount = totalAmount - currentTotalCalculated

    val isCustomSplitValid = selectedMethod != Cb_SplitMethod.CUSTOM ||
            SmartSplitEngine.validateCustomSplit(
                totalAmount = totalAmount,
                customAmounts = memberShares
            )

    Scaffold(
        containerColor = Cb_Bg,
        topBar = { Cb_TopBar(onBack = onBack) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Cb_Bg,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)
                ) {
                    val isReady = billName.isNotBlank() &&
                            totalAmount > 0 &&
                            isCustomSplitValid &&
                            (
                                    selectedMethod != Cb_SplitMethod.ITEMIZED ||
                                            billItems.all { item ->
                                                item.price.isNotBlank() &&
                                                        item.sharedByMemberIds.isNotEmpty()
                                            }
                                    )

                    Cb_BottomAction(
                        enabled = isReady,
                        onConfirm = {
                            // sau này điều hướng sang BillDetailScreen
                        }
                    )
                }
            }
        }
    ) { paddingValues ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 16.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Card thông tin chính
                item {
                    Cb_MainInfoCard(
                        billName = billName,
                        onNameChange = { billName = it },
                        totalAmountDisplay = formatCurrency(totalAmount),
                        isEditable = selectedMethod != Cb_SplitMethod.ITEMIZED,
                        totalAmountInput = totalAmountStr,
                        onAmountChange = { if (it.all { c -> c.isDigit() }) totalAmountStr = it }
                    )
                }

                // 2. Người thanh toán (Mặc định là Bạn)
                item { Cb_PayerSection() }

                // 3. Các tab phương thức chia
                item {
                    Cb_SplitMethodTabs(
                        selectedMethod = selectedMethod,
                        onMethodSelect = { selectedMethod = it }
                    )
                }

                // 4. UI cho từng phương thức
                if (selectedMethod == Cb_SplitMethod.ITEMIZED) {
                    item {
                        Text("DANH SÁCH MÓN ĂN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cb_TextSub.copy(alpha = 0.7f), letterSpacing = 1.5.sp)
                    }
                    items(billItems) { item ->
                        Cb_ItemEntryCard(
                            item = item,
                            members = members,
                            onRemove = { if (billItems.size > 1) billItems.remove(item) }
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = { billItems.add(Cb_BillItem(name = "Món ${billItems.size + 1}")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cb_OrangeEnd)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Thêm món mới")
                        }
                    }
                } else if (selectedMethod == Cb_SplitMethod.CUSTOM && totalAmount > 0) {
                    item { Cb_StatusBanner(remainingAmount = remainingAmount) }
                }

                // 5. Kết quả phân chia chi tiết
                item {
                    Text("PHÂN CHIA CHI TIẾT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cb_TextSub.copy(alpha = 0.7f), letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Cb_SplitDetailsList(
                        members = members,
                        method = selectedMethod,
                        memberShares = memberShares,
                        customAmounts = customAmounts,
                        onCustomAmountChange = { id, amt ->
                            if (amt.all { c -> c.isDigit() }) customAmounts[id] = amt
                        }
                    )
                }
            }
    }

}

@Composable
private fun Cb_TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Cb_OrangeEnd)
        }
        Text("Tạo hóa đơn", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Cb_OrangeEnd)
        Text("Lưu nháp", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cb_OrangeEnd.copy(alpha = 0.6f), modifier = Modifier.clickable { })
    }
}

@Composable
private fun Cb_MainInfoCard(
    billName: String, 
    onNameChange: (String) -> Unit, 
    totalAmountDisplay: String, 
    isEditable: Boolean, 
    totalAmountInput: String, 
    onAmountChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Cb_SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Cb_OrangeIconBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Cb_OrangeEnd, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (billName.isEmpty()) Text("Tên hóa đơn...", color = Color.Gray, fontSize = 16.sp)
                    BasicTextField(
                        value = billName, 
                        onValueChange = onNameChange, 
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Cb_TextMain),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("TỔNG CỘNG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cb_TextSub.copy(alpha = 0.5f), letterSpacing = 1.2.sp)
            if (isEditable) {
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicTextField(
                        value = totalAmountInput,
                        onValueChange = onAmountChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Cb_OrangeEnd),
                        modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(minWidth = 40.dp),
                        decorationBox = { innerTextField ->
                            if (totalAmountInput.isEmpty()) Text("0", style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Cb_OrangeEnd.copy(alpha = 0.3f)))
                            innerTextField()
                        }
                    )
                    Text(" đ", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Cb_OrangeEnd, modifier = Modifier.padding(bottom = 4.dp))
                }
            } else {
                Text(totalAmountDisplay, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Cb_OrangeEnd)
            }
        }
    }
}

@Composable
private fun Cb_PayerSection() {
    Column {
        Text("NGƯỜI THANH TOÁN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Cb_TextSub.copy(alpha = 0.7f), letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(Cb_SurfaceWhite),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).border(2.dp, Cb_OrangeStart, CircleShape).padding(2.dp).clip(CircleShape).background(Color.DarkGray), contentAlignment = Alignment.Center) {
                        Text("B", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Bạn", fontWeight = FontWeight.Bold, color = Cb_TextMain)
                        Text("Trả toàn bộ hóa đơn", fontSize = 12.sp, color = Cb_TextSub)
                    }
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Cb_TextSub)
            }
        }
    }
}

@Composable
private fun Cb_SplitMethodTabs(selectedMethod: Cb_SplitMethod, onMethodSelect: (Cb_SplitMethod) -> Unit) {
    Surface(color = Color(0xFFF3F3F3), shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(4.dp)) {
            Cb_SplitMethod.entries.forEach { method ->
                val isSelected = selectedMethod == method
                val label = when(method) {
                    Cb_SplitMethod.EQUAL -> "Chia đều"
                    Cb_SplitMethod.CUSTOM -> "Tự nhập"
                    Cb_SplitMethod.ITEMIZED -> "Theo món"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Brush.verticalGradient(listOf(Cb_OrangeStart, Cb_OrangeEnd)) 
                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)), 
                            RoundedCornerShape(50)
                        )
                        .clickable { onMethodSelect(method) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (isSelected) Color.White else Cb_TextSub.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun Cb_ItemEntryCard(item: Cb_BillItem, members: List<Cb_Member>, onRemove: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var price by remember { mutableStateOf(item.price) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Cb_SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it; item.name = it },
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Cb_TextMain),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (name.isEmpty()) Text("Tên món...", color = Color.Gray)
                        innerTextField()
                    }
                )
                BasicTextField(
                    value = price,
                    onValueChange = { if (it.all { c -> c.isDigit() }) { price = it; item.price = it } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Cb_OrangeEnd, textAlign = TextAlign.End),
                    modifier = Modifier.width(80.dp),
                    decorationBox = { innerTextField ->
                        if (price.isEmpty()) Text("0đ", color = Color.Gray, textAlign = TextAlign.End)
                        innerTextField()
                    }
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("AI ĂN MÓN NÀY?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(members) { member ->
                    val isSelected = item.sharedByMemberIds.contains(member.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) item.sharedByMemberIds.remove(member.id)
                            else item.sharedByMemberIds.add(member.id)
                        },
                        label = { Text(member.name, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cb_OrangeLightBg,
                            selectedLabelColor = Cb_OrangeEnd,
                            selectedLeadingIconColor = Cb_OrangeEnd
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun Cb_SplitDetailsList(
    members: List<Cb_Member>, 
    method: Cb_SplitMethod, 
    memberShares: Map<String, Long>, 
    customAmounts: Map<String, String>, 
    onCustomAmountChange: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(Cb_SurfaceWhite), 
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), 
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            members.forEachIndexed { index, member ->
                val amount = memberShares[member.id] ?: 0L
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (member.isMe) Cb_OrangeLightBg.copy(alpha = 0.4f) else Color.Transparent)
                        .padding(16.dp), 
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (member.isMe) Cb_OrangeEnd else Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                            Text(member.initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(member.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Cb_TextMain)
                            if (method != Cb_SplitMethod.CUSTOM) {
                                Text("Phần chia: ${formatCurrency(amount)}", fontSize = 12.sp, color = Cb_TextSub.copy(alpha = 0.8f))
                            }
                        }
                    }
                    if (method == Cb_SplitMethod.CUSTOM) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = customAmounts[member.id] ?: "", 
                                onValueChange = { onCustomAmountChange(member.id, it) }, 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Cb_OrangeEnd, textAlign = TextAlign.End), 
                                modifier = Modifier.width(100.dp).background(Color(0xFFF3F3F3), RoundedCornerShape(8.dp)).padding(8.dp),
                                decorationBox = { innerTextField ->
                                    if ((customAmounts[member.id] ?: "").isEmpty()) Text("0", style = TextStyle(fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.End))
                                    innerTextField()
                                }
                            )
                            Text(" đ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cb_OrangeEnd, modifier = Modifier.padding(start = 4.dp))
                        }
                    } else if (member.isMe) {
                        Icon(Icons.Default.Check, null, tint = Cb_OrangeEnd, modifier = Modifier.size(20.dp))
                    }
                }
                if (index < members.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun Cb_StatusBanner(remainingAmount: Long) {
    val color = when {
        remainingAmount == 0L -> Color(0xFF4CAF50)
        remainingAmount > 0 -> Cb_OrangeEnd
        else -> Color.Red
    }
    Surface(
        color = color.copy(alpha = 0.1f), 
        shape = RoundedCornerShape(12.dp), 
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = when {
                remainingAmount == 0L -> "Đã chia đủ hóa đơn! ✨"
                remainingAmount > 0 -> "Còn thiếu: ${formatCurrency(remainingAmount)}"
                else -> "Vượt mức: ${formatCurrency(-remainingAmount)}"
            },
            modifier = Modifier.padding(12.dp), 
            color = color, 
            fontSize = 13.sp, 
            fontWeight = FontWeight.Bold, 
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Cb_BottomAction(enabled: Boolean, onConfirm: () -> Unit) {
    Button(
        onClick = onConfirm, 
        enabled = enabled, 
        modifier = Modifier.fillMaxWidth().height(56.dp), 
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent), 
        contentPadding = PaddingValues(0.dp), 
        shape = RoundedCornerShape(16.dp)
    ) {
        val bg = if (enabled) Brush.horizontalGradient(listOf(Cb_OrangeStart, Cb_OrangeEnd)) 
                 else Brush.horizontalGradient(listOf(Color.LightGray, Color.Gray))
        Box(modifier = Modifier.fillMaxSize().background(bg, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Text("Xác nhận tạo hóa đơn", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun formatCurrency(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return formatter.format(amount).replace("₫", "đ")
}
