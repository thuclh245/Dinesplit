package com.example.dinesplit.core.firebase

import com.example.dinesplit.domain.exception.UsernameAlreadyExistsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

object FirebaseErrorMapper {
    fun toUserMessage(throwable: Throwable): String {
        return when (throwable) {
            is UsernameAlreadyExistsException -> "Username already exists"
            is FirebaseAuthUserCollisionException -> "Email already registered"
            is FirebaseAuthWeakPasswordException -> "Password is too weak"
            is FirebaseAuthInvalidUserException -> "Account not found"
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
            is FirebaseAuthException -> mapAuthCode(throwable.errorCode)
            is FirebaseFirestoreException -> mapFirestoreCode(throwable.code)
            is IOException -> "No internet connection"
            else -> throwable.message?.takeIf { it.isNotBlank() } ?: "Firebase temporarily unavailable"
        }
    }

    private fun mapAuthCode(errorCode: String): String {
        return when (errorCode) {
            "ERROR_USER_DISABLED" -> "This account is disabled"
            "ERROR_WRONG_PASSWORD" -> "Invalid email or password"
            "ERROR_USER_NOT_FOUND" -> "Account not found"
            else -> "Authentication failed"
        }
    }

    private fun mapFirestoreCode(code: FirebaseFirestoreException.Code): String {
        return when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Permission denied"
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Authentication failed"
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.ABORTED -> "Firebase temporarily unavailable"
            FirebaseFirestoreException.Code.INVALID_ARGUMENT -> "Invalid data"
            FirebaseFirestoreException.Code.NOT_FOUND -> "Data not found"
            else -> "Unable to save profile"
        }
    }
}

