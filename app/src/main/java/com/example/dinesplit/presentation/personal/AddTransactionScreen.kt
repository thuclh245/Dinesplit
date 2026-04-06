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
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.validation.TransactionFormInput
import com.example.dinesplit.domain.validation.TransactionFormValidator
import com.example.dinesplit.domain.validation.toTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    initialType: TransactionType? = null,
    onSave: (Transaction) -> Unit = {}
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedTypeName by rememberSaveable { mutableStateOf(initialType?.name.orEmpty()) }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var isCategoryMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isSubmitAttempted by rememberSaveable { mutableStateOf(false) }

    val currentDateMillis = remember { System.currentTimeMillis() }
    val currentDate = remember(currentDateMillis) { currentDateLabel(currentDateMillis) }
    val selectedType = transactionTypeFromRoute(selectedTypeName)

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

    val validation = TransactionFormValidator.validate(
        input = TransactionFormInput(
            amountText = amountText,
            type = selectedType,
            category = selectedCategory,
            note = note,
            dateMillis = currentDateMillis
        ),
        availableCategories = categoryOptions
    )

    val isAmountValid = validation.amountError == null
    val isTypeValid = validation.typeError == null
    val isCategoryValid = validation.categoryError == null
    val isFormValid = validation.isValid

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
                supportingText = if (isSubmitAttempted && !isAmountValid) validation.amountError else null
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
                        text = validation.typeError.orEmpty(),
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
                            Text(validation.categoryError.orEmpty())
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
                        val validInput = validation.validInput ?: return@PrimaryButton

                        onSave(
                            validInput.toTransaction(
                                id = UUID.randomUUID().toString(),
                                userId = "user_1"
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

private fun currentDateLabel(currentDateMillis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(currentDateMillis))
}

