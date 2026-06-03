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
            is UsernameAlreadyExistsException -> "Tên người dùng đã tồn tại"
            is FirebaseAuthUserCollisionException -> "Email đã được đăng ký"
            is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu"
            is FirebaseAuthInvalidUserException -> "Không tìm thấy tài khoản"
            is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu không hợp lệ"
            is FirebaseAuthException -> mapAuthCode(throwable.errorCode)
            is FirebaseFirestoreException -> mapFirestoreCode(throwable.code)
            is IOException -> "Không có kết nối internet"
            else -> throwable.message?.takeIf { it.isNotBlank() } ?: "Firebase tạm thời không khả dụng"
        }
    }

    private fun mapAuthCode(errorCode: String): String {
        return when (errorCode) {
            "ERROR_USER_DISABLED" -> "Tài khoản này đã bị vô hiệu hóa"
            "ERROR_WRONG_PASSWORD" -> "Email hoặc mật khẩu không hợp lệ"
            "ERROR_USER_NOT_FOUND" -> "Không tìm thấy tài khoản"
            else -> "Xác thực thất bại"
        }
    }

    private fun mapFirestoreCode(code: FirebaseFirestoreException.Code): String {
        return when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Bạn không có quyền thực hiện thao tác này"
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Vui lòng đăng nhập lại"
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.ABORTED -> "Firebase tạm thời không khả dụng"
            FirebaseFirestoreException.Code.INVALID_ARGUMENT -> "Dữ liệu không hợp lệ"
            FirebaseFirestoreException.Code.NOT_FOUND -> "Không tìm thấy dữ liệu"
            else -> "Không thể lưu dữ liệu"
        }
    }
}

