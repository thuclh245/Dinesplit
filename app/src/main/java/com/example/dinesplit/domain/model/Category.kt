package com.example.dinesplit.domain.model

/**
 * Category used by personal transactions.
 * icon can store a short code, emoji, or icon key for UI mapping.
 */
data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val type: TransactionType,
)
