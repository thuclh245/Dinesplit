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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.EmptyStateBlock
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.domain.model.ReminderType
import com.example.dinesplit.domain.model.SpendingReminder
import java.util.Locale

/**
 * Spending Reminder Management Screen - Tuần 3
 * Manage budget alerts and spending milestones
 */
@Composable
fun SpendingReminderScreen(
    onBack: () -> Unit
) {
    val viewModel: SpendingReminderViewModel = viewModel()
    val reminders = viewModel.reminders.collectAsState().value
    val uiState = viewModel.uiState.collectAsState().value

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

            if (reminders.isEmpty()) {
                EmptyStateBlock(
                    title = "No spending reminders yet",
                    subtitle = "Create a reminder to get alerted when you reach budget thresholds.",
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
            onDismiss = { showCreateDialog = false },
            onCreate = { categoryName, budget, threshold, type ->
                viewModel.addReminder(
                    categoryId = null,  // Overall budget
                    categoryName = categoryName,
                    budgetAmount = budget,
                    threshold = threshold,
                    reminderType = type
                )
                showCreateDialog = false
            }
        )
    }

    deletingReminder?.let { reminder ->
        AlertDialog(
            onDismissRequest = { deletingReminder = null },
            title = { Text("Delete Reminder?") },
            text = { Text("This will remove the reminder for ${reminder.categoryName}.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReminder(reminder.id)
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
                    text = "Budget: \$${String.format(Locale.US, "%.2f", reminder.budgetAmount)} • Alert at ${(reminder.threshold * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = reminder.reminderType.name,
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
    onDismiss: () -> Unit,
    onCreate: (String, Double, Float, ReminderType) -> Unit
) {
    var categoryName by rememberSaveable { mutableStateOf("Overall Budget") }
    var budgetAmount by rememberSaveable { mutableStateOf("1000.00") }
    var thresholdPercent by rememberSaveable { mutableStateOf("80") }
    var selectedType by rememberSaveable { mutableStateOf(ReminderType.MONTHLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Spending Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category/Budget Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it },
                    label = { Text("Budget Amount ($)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = thresholdPercent,
                    onValueChange = { thresholdPercent = it },
                    label = { Text("Alert Threshold (%)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Reminder Type:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    ReminderType.entries.forEach { type ->
                        androidx.compose.material3.FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        onCreate(
                            categoryName,
                            budgetAmount.toDouble(),
                            thresholdPercent.toInt() / 100f,
                            selectedType
                        )
                    } catch (e: Exception) {
                        // Handle invalid input
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

