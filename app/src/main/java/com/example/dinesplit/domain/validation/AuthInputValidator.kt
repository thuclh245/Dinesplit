package com.example.dinesplit.domain.validation

object AuthInputValidator {
    private const val MIN_PASSWORD_LENGTH = 6
    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    fun validateEmail(email: String): String? {
        val normalized = email.trim()
        if (normalized.isBlank()) return "Email is required"
        if (!EMAIL_REGEX.matches(normalized)) return "Email format is invalid"
        return null
    }

    fun validatePasswordForLogin(password: String): String? {
        if (password.isBlank()) return "Password is required"
        return null
    }

    fun validatePasswordForRegister(password: String): String? {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return "Password must be at least $MIN_PASSWORD_LENGTH characters"
        }
        return null
    }

    fun validateConfirmPassword(
        password: String,
        confirmPassword: String,
    ): String? {
        if (confirmPassword != password) return "Password confirmation does not match"
        return null
    }
}
