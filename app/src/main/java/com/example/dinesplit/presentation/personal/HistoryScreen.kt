package com.example.dinesplit.presentation.personal

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.NumberFormat
import java.util.Locale

data class HistoryTransactionItem(
    val id: String,
    val categoryIcon: String,
    val category: String,
    val amount: String,
    val date: String,
    val month: String,
    val type: TransactionType,
    val note: String?,
)

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    transactions: List<HistoryTransactionItem> = emptyList(),
    onTransactionClick: (HistoryTransactionItem) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTypeFilter by rememberSaveable { mutableStateOf<TransactionType?>(null) }

    // CHỐT CHẶN HIỆU NĂNG 1: Lọc chuỗi và danh mục giao dịch an toàn bằng remember
    val filteredTransactions = remember(transactions, query, selectedTypeFilter) {
        transactions.filter { item ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val needle = query.trim().lowercase()
                listOfNotNull(
                    item.category,
                    item.amount,
                    item.date,
                    item.month,
                    item.note,
                    item.type.name,
                ).any { value -> value.lowercase().contains(needle) }
            }

            val matchesType = selectedTypeFilter == null || item.type == selectedTypeFilter
            matchesQuery && matchesType
        }
    }

    // CHỐT CHẶN HIỆU NĂNG 2: Gom nhóm hóa đơn theo Ngày/Tháng, cô lập logic xử lý khỏi luồng render tự do
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { historyGroupLabel(it) }
    }

    // CHỐT CHẶN HIỆU NĂNG 3: Tính toán số liệu tổng Thu/Chi nhanh gọn trong bộ nhớ đệm
    val summaryStats = remember(filteredTransactions) {
        val income = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { parseAmount(it.amount) }
        val expense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { parseAmount(it.amount) }
        Pair(income, expense)
    }

    AppScaffold(
        title = "Sổ thu chi",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            Text(
                text = "Ledger.",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Thẻ tổng quan Thu - Chi thiết kế tinh tế toàn cục
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Khoản thu (Income)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "+${formatHistoryMoney(summaryStats.first)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Khoản chi (Expense)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "-${formatHistoryMoney(summaryStats.second)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thực tế (Net):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        val netBalance = summaryStats.first - summaryStats.second
                        Text(
                            text = if (netBalance >= 0) "+${formatHistoryMoney(netBalance)}" else formatHistoryMoney(netBalance),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Thanh tìm kiếm hóa đơn thông minh
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm kiếm hạng mục, ghi chú, số tiền...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                ),
            )

            // Thanh trượt ngang lọc nhanh Loại giao dịch bằng Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                contentPadding = PaddingValues(bottom = AppDimens.spaceXs)
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("Tất cả") },
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.INCOME,
                        onClick = { selectedTypeFilter = TransactionType.INCOME },
                        label = { Text("Khoản thu") },
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.EXPENSE,
                        onClick = { selectedTypeFilter = TransactionType.EXPENSE },
                        label = { Text("Khoản chi") },
                    )
                }
            }

            // Xử lý các trạng thái rẽ nhánh hiển thị nội dung trống hoặc danh sách ảo hóa
            if (groupedTransactions.isEmpty()) {
                EmptyStateBlock(
                    title = "Không tìm thấy giao dịch",
                    subtitle = if (transactions.isEmpty()) {
                        "Hãy thêm giao dịch đầu tiên để xây dựng sổ thu chi của bạn."
                    } else {
                        "Không tìm thấy kết quả phù hợp. Vui lòng thử lại bằng từ khóa khác."
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // DUYỆT PHÂN VÙNG GOM CỤM ĐỒNG BỘ: Giữ spec card đồng nhất, gán key cho header ổn định
                    groupedTransactions.forEach { (sectionTitle, itemsInSection) ->
                        item(key = "header_$sectionTitle") {
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = AppDimens.spaceSm, bottom = AppDimens.spaceXs),
                            )
                        }

                        item(key = "section_$sectionTitle") {
                            // Toàn bộ mảng phần tử trong cùng một ngày được ôm gọn trong 1 tấm thẻ AppCard theo đúng Spec UI của nhóm
                            AppCard {
                                Column {
                                    itemsInSection.forEachIndexed { index, item ->
                                        HistoryTransactionRow(
                                            item = item,
                                            onClick = { onTransactionClick(item) },
                                        )
                                        if (index != itemsInSection.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun historyGroupLabel(item: HistoryTransactionItem): String {
    val dateLower = item.date.lowercase()
    return when {
        dateLower.contains("today") -> "HÔM NAY"
        dateLower.contains("yesterday") -> "HÔM QUA"
        else -> item.month.uppercase()
    }
}

private fun parseAmount(amountStr: String): Double {
    return amountStr.replace(Regex("[^\\d.-]"), "").toDoubleOrNull() ?: 0.0
}

private fun formatHistoryMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} đ"
}

@Composable
private fun HistoryTransactionRow(
    item: HistoryTransactionItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AppDimens.spaceMd),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.medium,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.categoryIcon,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.note ?: item.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val isIncome = item.type == TransactionType.INCOME
            Text(
                text = if (isIncome) "+${item.amount}" else "-${item.amount}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (isIncome) "RECEIVED" else "PERSONAL",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistoryScreenPreview() {
    DineSplitTheme {
        HistoryScreen(onBack = {})
    }
}
