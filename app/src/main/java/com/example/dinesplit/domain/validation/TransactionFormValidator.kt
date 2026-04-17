package com.example.dinesplit.domain.validation

import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType


data class TransactionFormInput(
    val amountText: String,
    val type: TransactionType?,
    val categoryId: String,
    val categoryName: String,
    val note: String?,
    val dateMillis: Long
)

data class ValidTransactionFormInput(
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val category: String,
    val note: String?,
    val dateMillis: Long
)

data class TransactionFormValidationResult(
    val validInput: ValidTransactionFormInput? = null,
    val amountError: String? = null,
    val typeError: String? = null,
    val categoryError: String? = null
) {
    val isValid: Boolean
        get() = validInput != null
}

object TransactionFormValidator {
    fun validate(
        input: TransactionFormInput,
        availableCategoryIds: List<String> = emptyList()
    ): TransactionFormValidationResult {
        val amountValue = input.amountText.toDoubleOrNull()
        val amountError = when {
            input.amountText.isBlank() -> "Amount is required"
            amountValue == null -> "Amount must be a number"
            amountValue <= 0.0 -> "Amount must be greater than 0"
            else -> null
        }

        val typeError = if (input.type == null) {
            "Please choose transaction type"
        } else {
            null
        }

        val normalizedCategoryId = input.categoryId.trim()
        val categoryError = when {
            normalizedCategoryId.isBlank() -> "Category is required"
            availableCategoryIds.isNotEmpty() && normalizedCategoryId !in availableCategoryIds -> {
                "Please choose a valid category"
            }
            else -> null
        }

        val validInput = if (amountError == null && typeError == null && categoryError == null) {
            ValidTransactionFormInput(
                amount = amountValue!!,
                type = input.type!!,
                categoryId = normalizedCategoryId,
                category = input.categoryName.trim(),
                note = input.note?.trim()?.takeIf { it.isNotBlank() },
                dateMillis = input.dateMillis
            )
        } else {
            null
        }

        return TransactionFormValidationResult(
            validInput = validInput,
            amountError = amountError,
            typeError = typeError,
            categoryError = categoryError
        )
    }
}

fun ValidTransactionFormInput.toTransaction(
    id: String,
    userId: String,
    createdAt: Long = dateMillis
): Transaction {
    return Transaction(
        id = id,
        userId = userId,
        amount = amount,
        type = type,
        categoryId = categoryId,
        category = category,
        note = note,
        date = dateMillis,
        createdAt = createdAt
    )
}

