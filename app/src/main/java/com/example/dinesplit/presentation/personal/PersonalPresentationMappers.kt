package com.example.dinesplit.presentation.personal

import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun List<StoredCategory>.toManagedCategories(
    transactions: List<Transaction>
): List<ManagedCategory> {
    val totalsByCategory = transactions
        .groupBy { it.categoryId }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
    val maxByType = groupBy { it.type }
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
            progress = if (maxForType > 0.0) {
                (total / maxForType).toFloat()
            } else {
                0f
            },
            isActive = category.isActive
        )
    }
}

fun List<Transaction>.toHistoryItems(
    categories: List<StoredCategory>
): List<HistoryTransactionItem> {
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
            note = transaction.note
        )
    }
}

fun CategoryTypeFilter.toTransactionType(): TransactionType {
    return when (this) {
        CategoryTypeFilter.EXPENSE -> TransactionType.EXPENSE
        CategoryTypeFilter.INCOME -> TransactionType.INCOME
    }
}

private fun TransactionType.toCategoryTypeFilter(): CategoryTypeFilter {
    return when (this) {
        TransactionType.EXPENSE -> CategoryTypeFilter.EXPENSE
        TransactionType.INCOME -> CategoryTypeFilter.INCOME
    }
}

private fun formatSignedPersonalMoney(transaction: Transaction): String {
    val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
    return "$sign${formatPersonalMoney(transaction.amount)}"
}

private fun formatPersonalMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())}đ"
}

private fun formatHistoryDate(epochMillis: Long): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
}

private fun formatHistoryMonth(epochMillis: Long): String {
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(epochMillis))
}
