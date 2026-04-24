package com.example.dinesplit.domain.validation

object ProfileInputValidator {
    fun validateDisplayName(value: String): String? {
        if (value.trim().isBlank()) return "Display name is required"
        return null
    }

    fun validateUsername(value: String): String? {
        val username = value.trim()
        if (username.isBlank()) return "Username is required"
        if (username.length < 3) return "Username must be at least 3 characters"
        if (username.contains(' ')) return "Username must not contain spaces"
        return null
    }
}

