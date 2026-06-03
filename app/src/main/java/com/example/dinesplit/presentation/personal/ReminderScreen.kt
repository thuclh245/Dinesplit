package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dinesplit.core.ui.AppButton
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.ErrorStateBlock
import com.example.dinesplit.domain.model.ReminderType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReminderScreen(
    viewModel: ReminderViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    AppScaffold(
        title = "Spending Reminders",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Text(
                text = "Budget Alerts.",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            when (val state = uiState) {
                is ReminderUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is ReminderUiState.Error -> {
                    ErrorStateBlock(
                        title = "Cannot load reminders",
                        subtitle = state.message,
                        onRetryClick = { viewModel.loadReminders() }
                    )
                }
                is ReminderUiState.Success -> {
                    if (state.reminders.isEmpty()) {
                        EmptyStateBlock(
                            title = "No alerts configured",
                            subtitle = "Set up boundaries to track your spending limits automatically.",
                            actionText = "Create Alert",
                            onActionClick = { showCreateDialog = true }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(state.reminders, key = { it.id }) { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    onDelete = { viewModel.deleteReminder(it.id) }
                                )
                            }
                        }
                        
                        AppButton(
                            text = "Add Spending Reminder",
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(AppDimens.spaceXl))
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateReminderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, budget, threshold, type ->
                viewModel.createReminder(null, name, budget, threshold, type)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderItem,
    onDelete: (ReminderItem) -> Unit
) {
    val progress by remember(reminder.currentSpent, reminder.budgetAmount) {
        derivedStateOf {
            if (reminder.budgetAmount > 0) {
                (reminder.currentSpent / reminder.budgetAmount).toFloat().coerceIn(0f, 1f)
            } else 0f
        }
    }
    
    val isOverThreshold by remember(reminder.currentSpent, reminder.budgetAmount, reminder.threshold) {
        derivedStateOf {
            if (reminder.budgetAmount > 0) {
                (reminder.currentSpent / reminder.budgetAmount) >= reminder.threshold
            } else false
        }
    }

    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                }
                IconButton(onClick = { onDelete(reminder) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete reminder",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (isOverThreshold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            if (isOverThreshold) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
                        .padding(AppDimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Alert: You have reached ${(reminder.threshold * 100).toInt()}% of your budget!",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateReminderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Double, Float, ReminderType) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var budgetAmount by remember { mutableStateOf("") }
    var thresholdPercent by remember { mutableStateOf("80") }
    var selectedType by remember { mutableStateOf(ReminderType.MONTHLY) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Spending Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                AppTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = "Category / Name",
                    placeholder = "E.g., Groceries"
                )

                AppTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it },
                    label = "Budget Amount",
                    placeholder = "E.g., 5000000"
                )

                AppTextField(
                    value = thresholdPercent,
                    onValueChange = { thresholdPercent = it },
                    label = "Alert Threshold (%)",
                    placeholder = "80"
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
                        require(categoryName.isNotBlank()) { "Name cannot be empty." }
                        require(normalizedBudget > 0.0) { "Budget must be > 0." }
                        require(normalizedThreshold in 1..100) { "Threshold must be between 1 and 100." }
                        
                        onCreate(
                            categoryName,
                            normalizedBudget,
                            normalizedThreshold / 100f,
                            selectedType
                        )
                    } catch (e: Exception) {
                        validationMessage = e.message ?: "Invalid details"
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

private fun formatReminderMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} VND"
}