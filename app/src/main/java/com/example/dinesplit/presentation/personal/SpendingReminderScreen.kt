package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.dinesplit.core.ui.AppTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.SpendingReminder
import com.example.dinesplit.domain.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SpendingReminderScreen(
    onBack: () -> Unit,
    reminders: List<SpendingReminder> = emptyList(),
    categories: List<StoredCategory> = emptyList(),
    errorMessage: String? = null,
    onCreateReminder: (String?, String, Double, Float, ReminderType) -> Unit = { _, _, _, _, _ -> },
    onDeleteReminder: (String) -> Unit = {},
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var deletingReminder by remember { mutableStateOf<SpendingReminder?>(null) }

    AppScaffold(
        title = "Nhắc nhở chi tiêu",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            Text(
                text = "Cảnh báo chi tiêu.",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal),
            )

            errorMessage?.let { message ->
                ErrorStateBlock(
                    title = "Không thể cập nhật nhắc nhở",
                    subtitle = message,
                    onRetryClick = {},
                )
            }

            if (reminders.isEmpty()) {
                EmptyStateBlock(
                    title = "Chưa có nhắc nhở chi tiêu nào",
                    subtitle = "Tạo cảnh báo ngân sách được Firebase hỗ trợ cho chi tiêu thực tế của bạn.",
                    actionText = "Tạo nhắc nhở",
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                reminders.forEach { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onDelete = { deletingReminder = it },
                    )
                }
            }

            PrimaryButton(
                text = "Thêm nhắc nhở chi tiêu",
                onClick = { showCreateDialog = true },
                modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal),
            )

            Spacer(modifier = Modifier.height(AppDimens.spaceXl))
        }
    }

    if (showCreateDialog) {
        CreateReminderDialog(
            categories = categories,
            onDismiss = { showCreateDialog = false },
            onCreate = { categoryId, categoryName, budget, threshold, type ->
                onCreateReminder(categoryId, categoryName, budget, threshold, type)
                showCreateDialog = false
            },
        )
    }

    deletingReminder?.let { reminder ->
         AlertDialog(
             onDismissRequest = { deletingReminder = null },
             title = { Text("Xóa nhắc nhở?") },
             text = { Text("Điều này sẽ xóa nhắc nhở cho ${reminder.categoryName}.") },
             confirmButton = {
                 TextButton(
                     onClick = {
                         onDeleteReminder(reminder.id)
                         deletingReminder = null
                     }
                 ) {
                     Text("Xóa")
                 }
             },
             dismissButton = {
                 TextButton(onClick = { deletingReminder = null }) {
                     Text("Hủy")
                 }
             }
         )
     }
}

@Composable
private fun ReminderCard(
    reminder: SpendingReminder,
    onDelete: (SpendingReminder) -> Unit,
) {
    val progress = (reminder.currentSpent / reminder.budgetAmount).toFloat().coerceIn(0f, 1f)
    val isOverThreshold = reminder.currentSpent >= reminder.budgetAmount * reminder.threshold

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            ) {
                Text(
                    text = reminder.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                     text = "Đã chi ${formatReminderMoney(reminder.currentSpent)} trong ${formatReminderMoney(reminder.budgetAmount)}",
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color =
                        if (isOverThreshold) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                     text = "${reminder.reminderType.displayLabel()} - cảnh báo tại ${(reminder.threshold * 100).toInt()}%",
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.outline
                 )
            }
            IconButton(onClick = { onDelete(reminder) }) {
                 Icon(
                     imageVector = Icons.Filled.Delete,
                     contentDescription = "Xóa nhắc nhở",
                     tint = MaterialTheme.colorScheme.error
                 )
            }
        }
    }
}

@Composable
private fun CreateReminderDialog(
    categories: List<StoredCategory>,
    onDismiss: () -> Unit,
    onCreate: (String?, String, Double, Float, ReminderType) -> Unit,
) {
    val expenseCategories =
        remember(categories) {
            categories.filter { it.type == TransactionType.EXPENSE }.sortedBy { it.name }
        }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var budgetAmount by rememberSaveable { mutableStateOf("") }
    var thresholdPercent by rememberSaveable { mutableStateOf("80") }
    var selectedType by rememberSaveable { mutableStateOf(ReminderType.MONTHLY) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
         onDismissRequest = onDismiss,
         title = { Text("Tạo Nhắc Nhở Chi Tiêu") },
         text = {
             Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                 Text("Phạm vi", style = MaterialTheme.typography.labelSmall)
                ReminderScopeChips(
                    categories = expenseCategories,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it },
                )

                AppTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it },
                    label = "Số Tiền Ngân Sách",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                AppTextField(
                    value = thresholdPercent,
                    onValueChange = { thresholdPercent = it },
                    label = "Ngưỡng Cảnh Báo (%)",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                 Text("Loại Nhắc Nhở", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                ) {
                    ReminderType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayLabel()) }
                        )
                    }
                }

                validationMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                 onClick = {
                     try {
                         val normalizedBudget = budgetAmount.toDouble()
                         val normalizedThreshold = thresholdPercent.toInt()
                         require(normalizedBudget > 0.0) { "Ngân sách phải lớn hơn 0." }
                         require(normalizedThreshold in 1..100) { "Ngưỡng phải từ 1 đến 100." }
                         val category = expenseCategories.firstOrNull { it.id == selectedCategoryId }
                         onCreate(
                             category?.id,
                             category?.name ?: "Ngân Sách Chung",
                             normalizedBudget,
                             normalizedThreshold / 100f,
                             selectedType
                         )
                         validationMessage = null
                     } catch (e: Exception) {
                         validationMessage = e.message ?: "Vui lòng nhập chi tiết nhắc nhở hợp lệ."
                     }
                 }
             ) {
                 Text("Tạo")
            }
         },
         dismissButton = {
             TextButton(onClick = onDismiss) {
                 Text("Hủy")
             }
         }
     )
 }

 @Composable
 private fun ReminderScopeChips(
     categories: List<StoredCategory>,
     selectedCategoryId: String?,
     onSelect: (String?) -> Unit
 ) {
     Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
         Row(
             modifier = Modifier.fillMaxWidth(),
             horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
         ) {
             FilterChip(
                 selected = selectedCategoryId == null,
                 onClick = { onSelect(null) },
                 label = { Text("Chung") }
            )
            categories.take(1).forEach { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { onSelect(category.id) },
                    label = { Text(category.name) },
                )
            }
        }
        categories.drop(1).chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            ) {
                rowItems.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onSelect(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }
        }
    }
}

private fun formatReminderMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} VND"
}
