package com.example.dinesplit.presentation.personal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.data.repository.LocalAuthRepository
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: PersonalViewModel = viewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val context = LocalContext.current
    val authRepo = LocalAuthRepository.getInstance(context)
    val currentSession by authRepo.sessionFlow.collectAsState()

    LaunchedEffect(currentSession) {
        currentSession?.let { session ->
            viewModel.setCurrentUserId(session.uid)
        } ?: run {
            viewModel.clearCurrentUserId()
        }
    }

    val categoriesById = remember(categories) { categories.associateBy { it.id } }

    val historyItems = remember(transactions, categoriesById) {
        transactions.map { it.toHistoryItem(categoriesById[it.categoryId]?.icon) }
    }

    HistoryScreen(
        onBack = onBack,
        transactions = historyItems,
        onTransactionClick = { item -> onTransactionClick(item.id) }
    )
}

private fun Transaction.toHistoryItem(icon: String?): HistoryTransactionItem {
    val categoryName = category
    val amountSign = if (type == TransactionType.INCOME) "+" else "-"
    
    return HistoryTransactionItem(
        id = id,
        categoryIcon = icon ?: categoryName.take(2).uppercase(Locale.US),
        category = categoryName,
        amount = "$amountSign${formatCurrencyVnd(amount)}",
        date = formatDateTimeLabel(date),
        month = SimpleDateFormat("MMM yyyy", Locale.US).format(Date(date)),
        type = type,
        note = note
    )
}

private fun formatCurrencyVnd(amount: Double): String {
    val grouped = String.format(Locale.US, "%,d", amount.toLong())
    return grouped.replace(',', '.') + "đ"
}

private fun formatDateTimeLabel(epochMillis: Long): String {
    val date = Date(epochMillis)
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    val oneDay = 24 * 60 * 60 * 1000L
    
    val prefix = when {
        diff < oneDay && isSameDay(now, epochMillis) -> "Today"
        diff < 2 * oneDay && isSameDay(now - oneDay, epochMillis) -> "Yesterday"
        else -> SimpleDateFormat("dd MMM", Locale.US).format(date)
    }
    
    val time = SimpleDateFormat("h:mm a", Locale.US).format(date)
    return "$prefix, $time"
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    return fmt.format(Date(t1)) == fmt.format(Date(t2))
}
