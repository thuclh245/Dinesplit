package com.example.dinesplit.domain.model

data class Group(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val memberCount: Int,
    val totalExpense: Double,
    val yourBalance: Double, // Positive means you are owed, negative means you owe
    val createdAt: Long,
    val ownerId: String? = null
)
