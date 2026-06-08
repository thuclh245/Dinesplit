package com.example.dinesplit.presentation.personal

import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.PersonalWallet
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.Transaction

/**
 * Trạng thái giao diện tổng hợp cho màn hình tài chính cá nhân.
 * Chứa toàn bộ dữ liệu cần thiết để render giao diện bao gồm danh sách giao dịch, danh mục,
 * quy tắc lặp lại, mục tiêu tiết kiệm, ví, và các chỉ số tài chính tổng hợp.
 *
 * @property isLoading true khi đang tải dữ liệu từ cơ sở dữ liệu.
 * @property isSaving true khi đang thực hiện thao tác ghi dữ liệu (thêm/sửa/xóa).
 * @property errorMessage Thông báo lỗi hiển thị cho người dùng (null nếu không có lỗi).
 * @property currentUserId ID của người dùng hiện tại đang đăng nhập.
 * @property transactions Danh sách giao dịch trong tháng hiện tại (đã lọc).
 * @property categories Danh sách các danh mục tài chính (Thu nhập/Chi tiêu).
 * @property recurringRules Danh sách các quy tắc giao dịch tự động lặp lại.
 * @property goals Danh sách các mục tiêu tiết kiệm.
 * @property wallets Danh sách các ví cá nhân.
 * @property totalIncome Tổng thu nhập trong tháng hiện tại.
 * @property totalExpense Tổng chi tiêu trong tháng hiện tại.
 * @property balance Số dư (thu nhập - chi tiêu) của tháng hiện tại.
 */
data class PersonalUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: String = "",
    val transactions: List<Transaction> = emptyList(),
    val categories: List<StoredCategory> = emptyList(),
    val recurringRules: List<RecurringRule> = emptyList(),
    val goals: List<PersonalGoal> = emptyList(),
    val wallets: List<PersonalWallet> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
)
