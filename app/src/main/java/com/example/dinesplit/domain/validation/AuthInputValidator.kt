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

    fun validateLoginIdentifier(identifier: String): String? {
        val normalized = identifier.trim()
        if (normalized.isBlank()) return "Vui lòng nhập Email hoặc Tên người dùng"
        if (normalized.contains("@")) {
            if (!EMAIL_REGEX.matches(normalized)) return "Định dạng Email không hợp lệ"
        } else {
            if (normalized.length < 3) return "Tên người dùng phải từ 3 ký tự trở lên"
        }
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
