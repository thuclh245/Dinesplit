package com.example.dinesplit.presentation.personal

import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chuyển đổi danh sách danh mục lưu trữ [StoredCategory] thành danh sách danh mục hiển thị [ManagedCategory].
 * Đồng thời tự động tính tổng số tiền giao dịch và tỉ lệ phần trăm chi tiêu/thu nhập của từng danh mục
 * so với danh mục có giá trị lớn nhất cùng loại (để vẽ thanh tiến trình progress bar).
 *
 * @param transactions Danh sách các giao dịch cá nhân dùng để tính tổng số tiền theo danh mục.
 * @return Danh sách [ManagedCategory] dùng cho hiển thị trên UI.
 */
fun List<StoredCategory>.toManagedCategories(transactions: List<Transaction>): List<ManagedCategory> {
    val totalsByCategory =
        transactions
            .groupBy { it.categoryId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
    val maxByType =
        groupBy { it.type }
            .mapValues { (_, categories) ->
                categories.maxOfOrNull { category -> totalsByCategory[category.id] ?: 0.0 } ?: 0.0
            }

    return map { category ->
        val total = totalsByCategory[category.id] ?: 0.0
        val maxForType = maxByType[category.type] ?: 0.0
        ManagedCategory(
            id = category.id,
            name = category.name,
            icon = category.icon,
            type = category.type.toCategoryTypeFilter(),
            isCustom = category.isCustom,
            description = category.description,
            amountLabel = formatPersonalMoney(total),
            progress =
                if (maxForType > 0.0) {
                    (total / maxForType).toFloat()
                } else {
                    0f
                },
            isActive = category.isActive,
        )
    }
}

/**
 * Chuyển đổi danh sách giao dịch cá nhân [Transaction] sang danh sách hiển thị lịch sử giao dịch [HistoryTransactionItem].
 * Giao dịch được sắp xếp theo thứ tự thời gian giảm dần (mới nhất lên đầu) và định dạng số tiền đi kèm ký tự dấu (+/-).
 *
 * @param categories Danh sách các danh mục để lấy biểu tượng hiển thị tương ứng.
 * @return Danh sách [HistoryTransactionItem] đã định dạng sẵn sàng hiển thị trên giao diện lịch sử.
 */
fun List<Transaction>.toHistoryItems(categories: List<StoredCategory>): List<HistoryTransactionItem> {
    val iconsByCategory = categories.associate { it.id to it.icon }

    return sortedByDescending { it.date }.map { transaction ->
        HistoryTransactionItem(
            id = transaction.id,
            categoryIcon = iconsByCategory[transaction.categoryId] ?: transaction.category.take(2).uppercase(),
            category = transaction.category,
            amount = formatSignedPersonalMoney(transaction),
            date = formatHistoryDate(transaction.date),
            month = formatHistoryMonth(transaction.date),
            type = transaction.type,
            note = transaction.note,
        )
    }
}

/**
 * Chuyển đổi bộ lọc loại danh mục của giao diện [CategoryTypeFilter] thành loại giao dịch nghiệp vụ [TransactionType].
 *
 * @return Giá trị [TransactionType] tương ứng.
 */
fun CategoryTypeFilter.toTransactionType(): TransactionType {
    return when (this) {
        CategoryTypeFilter.EXPENSE -> TransactionType.EXPENSE
        CategoryTypeFilter.INCOME -> TransactionType.INCOME
    }
}

/**
 * Chuyển đổi loại giao dịch nghiệp vụ [TransactionType] thành bộ lọc hiển thị giao diện [CategoryTypeFilter].
 *
 * @return Giá trị [CategoryTypeFilter] tương ứng.
 */
private fun TransactionType.toCategoryTypeFilter(): CategoryTypeFilter {
    return when (this) {
        TransactionType.EXPENSE -> CategoryTypeFilter.EXPENSE
        TransactionType.INCOME -> CategoryTypeFilter.INCOME
    }
}

/**
 * Định dạng số tiền giao dịch đi kèm dấu chỉ hướng (+ đối với thu nhập, - đối với chi tiêu) và đơn vị tiền tệ VND.
 * Ví dụ: "+50.000 VND", "-120.000 VND".
 *
 * @param transaction Giao dịch cá nhân [Transaction].
 * @return Chuỗi số tiền đã định dạng có dấu.
 */
private fun formatSignedPersonalMoney(transaction: Transaction): String {
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
    return "$sign${formatPersonalMoney(transaction.amount)}"
}

/**
 * Định dạng số tiền kiểu Double thành chuỗi hiển thị theo định dạng tiền tệ Việt Nam (vi_VN) kèm hậu tố "VND".
 * Ví dụ: 100000.0 -> "100.000 VND".
 *
 * @param amount Số tiền.
 * @return Chuỗi hiển thị tiền tệ thân thiện.
 */
private fun formatPersonalMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} VND"
}

/**
 * Định dạng mốc thời gian dạng epoch millisecond sang chuỗi ngày hiển thị lịch sử dạng "dd MMM yyyy".
 * Ví dụ: "08 thg 6 2026".
 *
 * @param epochMillis Thời gian tính bằng mili-giây.
 * @return Chuỗi ngày đã định dạng.
 */
private fun formatHistoryDate(epochMillis: Long): String {
    return SimpleDateFormat("dd MMM yyyy", Locale("vi", "VN")).format(Date(epochMillis))
}

/**
 * Định dạng mốc thời gian dạng epoch millisecond sang chuỗi tháng/năm hiển thị dạng "MMMM yyyy".
 * Ví dụ: "Tháng sáu 2026".
 *
 * @param epochMillis Thời gian tính bằng mili-giây.
 * @return Chuỗi tháng/năm đã định dạng.
 */
private fun formatHistoryMonth(epochMillis: Long): String {
    return SimpleDateFormat("MMMM yyyy", Locale("vi", "VN")).format(Date(epochMillis))
}
