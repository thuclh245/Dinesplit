package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AddTransactionDraft(
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val note: String?,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    initialType: TransactionType? = null,
    onSave: (AddTransactionDraft) -> Unit = {}
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedTypeName by rememberSaveable { mutableStateOf(initialType?.name.orEmpty()) }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var isCategoryMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isSubmitAttempted by rememberSaveable { mutableStateOf(false) }

    val currentDate = remember { currentDateLabel() }
    val selectedType = transactionTypeFromRoute(selectedTypeName)
    val amountValue = amountText.toDoubleOrNull()

    val categoryOptions = when (selectedType) {
        TransactionType.INCOME -> listOf("Salary", "Bonus", "Gift", "Other")
        TransactionType.EXPENSE -> listOf("Food", "Drink", "Travel", "Shopping", "Other")
        null -> emptyList()
    }

    LaunchedEffect(selectedTypeName) {
        if (selectedCategory.isNotBlank() && selectedCategory !in categoryOptions) {
            selectedCategory = ""
        }
    }

    val isAmountValid = amountValue != null && amountValue > 0
    val isTypeValid = selectedType != null
    val isCategoryValid = selectedCategory.isNotBlank()
    val isFormValid = isAmountValid && isTypeValid && isCategoryValid

    AppScaffold(
        title = "Add Transaction",
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
            AppTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Amount",
                placeholder = "0",
                isError = isSubmitAttempted && !isAmountValid,
                supportingText = if (isSubmitAttempted && !isAmountValid) "Amount must be greater than 0" else null
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
                ) {
                    TransactionType.entries.forEach { typeOption ->
                        FilterChip(
                            selected = selectedType == typeOption,
                            onClick = { selectedTypeName = typeOption.name },
                            label = { Text(typeOption.displayLabel()) }
                        )
                    }
                }

                if (isSubmitAttempted && !isTypeValid) {
                    Text(
                        text = "Please choose transaction type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = isCategoryMenuExpanded,
                onExpandedChange = {
                    if (categoryOptions.isNotEmpty()) {
                        isCategoryMenuExpanded = !isCategoryMenuExpanded
                    }
                }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    readOnly = true,
                    label = {
                        Text("Category")
                    },
                    placeholder = {
                        Text(if (isTypeValid) "Select category" else "Select type first")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded)
                    },
                    isError = isSubmitAttempted && !isCategoryValid,
                    supportingText = {
                        if (isSubmitAttempted && !isCategoryValid) {
                            Text("Category is required")
                        }
                    }
                )

                ExposedDropdownMenu(
                    expanded = isCategoryMenuExpanded,
                    onDismissRequest = { isCategoryMenuExpanded = false }
                ) {
                    categoryOptions.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                isCategoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            AppTextField(
                value = note,
                onValueChange = { note = it },
                label = "Note (optional)",
                placeholder = "Add note",
                singleLine = false
            )

            AppTextField(
                value = currentDate,
                onValueChange = {},
                label = "Date",
                enabled = false
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onBack
                )

                PrimaryButton(
                    text = "Save Transaction",
                    onClick = {
                        isSubmitAttempted = true
                        if (!isFormValid) return@PrimaryButton
                        val type = transactionTypeFromRoute(selectedTypeName) ?: return@PrimaryButton
                        val amount = amountText.toDoubleOrNull() ?: return@PrimaryButton

                        onSave(
                            AddTransactionDraft(
                                amount = amount,
                                type = type,
                                category = selectedCategory,
                                note = note.ifBlank { null },
                                date = currentDate
                            )
                        )
                        onBack()
                    },
                    enabled = isFormValid
                )
            }
        }
    }
}

private fun currentDateLabel(): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date())
}

