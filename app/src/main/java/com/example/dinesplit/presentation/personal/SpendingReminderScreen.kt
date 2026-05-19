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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
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
    onDeleteReminder: (String) -> Unit = {}
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var deletingReminder by remember { mutableStateOf<SpendingReminder?>(null) }

    AppScaffold(
        title = "Spending Reminders",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Text(
                text = "Spending Alerts.",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal)
            )

            errorMessage?.let { message ->
                ErrorStateBlock(
                    title = "Cannot update reminders",
                    subtitle = message,
                    onRetryClick = {}
                )
            }

            if (reminders.isEmpty()) {
                EmptyStateBlock(
                    title = "No spending reminders yet",
                    subtitle = "Create a Firebase-backed budget alert for your real spending.",
                    actionText = "Create Reminder",
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                reminders.forEach { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onDelete = { deletingReminder = it }
                    )
                }
            }

            PrimaryButton(
                text = "Add Spending Reminder",
                onClick = { showCreateDialog = true },
                modifier = Modifier.padding(horizontal = AppDimens.screenHorizontal)
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
            }
        )
    }

    deletingReminder?.let { reminder ->
        AlertDialog(
            onDismissRequest = { deletingReminder = null },
            title = { Text("Delete reminder?") },
            text = { Text("This will remove the reminder for ${reminder.categoryName}.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteReminder(reminder.id)
                        deletingReminder = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingReminder = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: SpendingReminder,
    onDelete: (SpendingReminder) -> Unit
) {
    val progress = (reminder.currentSpent / reminder.budgetAmount).toFloat().coerceIn(0f, 1f)
    val isOverThreshold = reminder.currentSpent >= reminder.budgetAmount * reminder.threshold

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                Text(
                    text = reminder.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Spent ${formatReminderMoney(reminder.currentSpent)} of ${formatReminderMoney(reminder.budgetAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isOverThreshold) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${reminder.reminderType.name.lowercase().replaceFirstChar { it.uppercase() }} - alert at ${(reminder.threshold * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = { onDelete(reminder) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete reminder",
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
    onCreate: (String?, String, Double, Float, ReminderType) -> Unit
) {
    val expenseCategories = remember(categories) {
        categories.filter { it.type == TransactionType.EXPENSE }.sortedBy { it.name }
    }
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var budgetAmount by rememberSaveable { mutableStateOf("") }
    var thresholdPercent by rememberSaveable { mutableStateOf("80") }
    var selectedType by rememberSaveable { mutableStateOf(ReminderType.MONTHLY) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Spending Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                Text("Scope", style = MaterialTheme.typography.labelSmall)
                ReminderScopeChips(
                    categories = expenseCategories,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = { selectedCategoryId = it }
                )

                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it },
                    label = { Text("Budget Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = thresholdPercent,
                    onValueChange = { thresholdPercent = it },
                    label = { Text("Alert Threshold (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Reminder Type", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    ReminderType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                validationMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
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
                        require(normalizedBudget > 0.0) { "Budget must be greater than 0." }
                        require(normalizedThreshold in 1..100) { "Threshold must be between 1 and 100." }
                        val category = expenseCategories.firstOrNull { it.id == selectedCategoryId }
                        onCreate(
                            category?.id,
                            category?.name ?: "Overall Budget",
                            normalizedBudget,
                            normalizedThreshold / 100f,
                            selectedType
                        )
                        validationMessage = null
                    } catch (e: Exception) {
                        validationMessage = e.message ?: "Please enter valid reminder details."
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
                label = { Text("Overall") }
            )
            categories.take(1).forEach { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { onSelect(category.id) },
                    label = { Text(category.name) }
                )
            }
        }
        categories.drop(1).chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                rowItems.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onSelect(category.id) },
                        label = { Text(category.name) }
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
