package com.example.dinesplit.presentation.personal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.data.repository.StoredCategory
import com.example.dinesplit.domain.model.TransactionType
import java.util.Locale

@Composable
fun CategoryManagementRoute(
    onBack: () -> Unit,
    viewModel: PersonalViewModel = viewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    
    val usedCategoryIds = remember(transactions) {
        transactions.map { it.categoryId }.toSet()
    }

    // ✅ Calculate amounts per category from transactions
    val amountsByCategory = remember(transactions) {
        transactions
            .groupBy { it.categoryId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
    }

    // ✅ Map categories to ManagedCategory with computed amounts
    val managedCategories = remember(categories, amountsByCategory) {
        categories.map { category ->
            category.toManagedCategory(
                amountsByCategory[category.id] ?: 0.0
            )
        }
    }

    CategoryManagementScreen(
        categories = managedCategories,
        usedCategoryIds = usedCategoryIds,
        onAddCategory = { input ->
            viewModel.addCategory(
                name = input.name,
                description = input.description,
                type = input.type.toTransactionType(),
                isCustom = input.isCustom
            )
        },
        onUpdateCategory = { category, input ->
            viewModel.updateCategory(
                categoryId = category.id,
                name = input.name,
                description = input.description,
                isActive = category.isActive
            )
        },
        onDeleteCategory = { category ->
            viewModel.deleteCategory(category.id)
        },
        onBack = onBack
    )
}

private fun StoredCategory.toManagedCategory(totalAmount: Double = 0.0): ManagedCategory {
    val formattedAmount = formatCurrencyVnd(totalAmount)
    val totalMaxAmount = 2000.0  // Reference amount for progress bar
    val progress = ((totalAmount / totalMaxAmount).coerceIn(0.0, 1.0)).toFloat()

    return ManagedCategory(
        id = id,
        name = name,
        icon = icon,
        type = if (type == TransactionType.INCOME) CategoryTypeFilter.INCOME else CategoryTypeFilter.EXPENSE,
        isCustom = isCustom,
        description = description,
        amountLabel = formattedAmount, // ✅ Use computed amount instead of hardcoded $0.00
        progress = progress,
        isActive = isActive
    )
}

private fun formatCurrencyVnd(amount: Double): String {
    val grouped = String.format(Locale.US, "%,d", amount.toLong())
    return grouped.replace(',', '.') + "đ"
}

private fun CategoryTypeFilter.toTransactionType(): TransactionType {
    return when (this) {
        CategoryTypeFilter.EXPENSE -> TransactionType.EXPENSE
        CategoryTypeFilter.INCOME -> TransactionType.INCOME
    }
}
