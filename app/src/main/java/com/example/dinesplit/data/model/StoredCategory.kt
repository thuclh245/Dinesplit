package com.example.dinesplit.data.model

import com.example.dinesplit.domain.model.TransactionType

/**
 * Data-layer model representing a category as stored in SQLite.
 * Maps directly to the `categories` table schema.
 */
data class StoredCategory(
    val id: String,
    val name: String,
    val icon: String,
    val type: TransactionType,
    val isCustom: Boolean,
    val description: String,
    val amountLabel: String,
    val progress: Float,
    val isActive: Boolean
)
