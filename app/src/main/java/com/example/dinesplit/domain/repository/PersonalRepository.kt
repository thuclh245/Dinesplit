package com.example.dinesplit.domain.repository

import android.net.Uri
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.PersonalWallet
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.SpendingReminder
import com.example.dinesplit.domain.model.Transaction

interface PersonalRepository {
    /**
     * Lấy toàn bộ danh sách giao dịch cá nhân của người dùng hiện tại từ cơ sở dữ liệu.
     *
     * @return Danh sách các đối tượng [Transaction] của người dùng.
     */
    suspend fun getAllTransactions(): List<Transaction>

    /**
     * Lấy toàn bộ danh mục chi tiêu/thu nhập cá nhân. Nếu người dùng chưa có danh mục nào,
     * các danh mục mặc định sẽ được khởi tạo tự động.
     *
     * @return Danh sách các đối tượng [StoredCategory].
     */
    suspend fun getCategories(): List<StoredCategory>

    /**
     * Thêm mới một giao dịch tài chính cá nhân vào cơ sở dữ liệu.
     *
     * @param transaction Đối tượng [Transaction] cần thêm mới.
     */
    suspend fun insertTransaction(transaction: Transaction)

    /**
     * Cập nhật thông tin của một giao dịch tài chính cá nhân đã tồn tại.
     *
     * @param transaction Đối tượng [Transaction] chứa thông tin mới để cập nhật.
     */
    suspend fun updateTransaction(transaction: Transaction)

    /**
     * Xóa một giao dịch tài chính cá nhân dựa trên ID giao dịch.
     *
     * @param transactionId ID của giao dịch cần xóa.
     */
    suspend fun deleteTransaction(transactionId: String)

    /**
     * Tải hình ảnh hóa đơn lên bộ lưu trữ đám mây liên kết với ID giao dịch.
     *
     * @param transactionId ID của giao dịch liên kết với hóa đơn này.
     * @param receiptUri Uri của hình ảnh hóa đơn trong thiết bị.
     * @return [Result] chứa URL hình ảnh sau khi tải lên thành công, hoặc lỗi nếu thất bại.
     */
    suspend fun uploadReceiptImage(
        transactionId: String,
        receiptUri: Uri,
    ): Result<String>

    /**
     * Thêm mới một danh mục thu/chi tùy chỉnh cho người dùng.
     *
     * @param category Đối tượng [StoredCategory] cần lưu trữ.
     */
    suspend fun insertCategory(category: StoredCategory)

    /**
     * Cập nhật thông tin của một danh mục thu/chi đã tồn tại.
     *
     * @param category Đối tượng [StoredCategory] chứa thông tin cập nhật.
     */
    suspend fun updateCategory(category: StoredCategory)

    /**
     * Xóa một danh mục thu/chi cá nhân dựa trên ID danh mục.
     *
     * @param categoryId ID của danh mục cần xóa.
     */
    suspend fun deleteCategory(categoryId: String)

    /**
     * Lấy danh sách các nhắc nhở chi tiêu (cảnh báo ngân sách) của người dùng hiện tại.
     *
     * @return Danh sách các nhắc nhở chi tiêu [SpendingReminder].
     */
    suspend fun getSpendingReminders(): List<SpendingReminder>

    /**
     * Thêm mới một nhắc nhở chi tiêu vào cơ sở dữ liệu.
     *
     * @param reminder Đối tượng [SpendingReminder] cần thêm mới.
     */
    suspend fun insertSpendingReminder(reminder: SpendingReminder)

    /**
     * Cập nhật một nhắc nhở chi tiêu đã tồn tại.
     *
     * @param reminder Đối tượng [SpendingReminder] chứa thông tin cập nhật.
     */
    suspend fun updateSpendingReminder(reminder: SpendingReminder)

    /**
     * Xóa một nhắc nhở chi tiêu dựa trên ID nhắc nhở.
     *
     * @param reminderId ID của nhắc nhở cần xóa.
     */
    suspend fun deleteSpendingReminder(reminderId: String)

    /**
     * Lấy danh sách các quy tắc giao dịch lặp lại định kỳ của người dùng hiện tại.
     *
     * @return Danh sách các quy tắc lặp lại [RecurringRule].
     */
    suspend fun getRecurringRules(): List<RecurringRule>

    /**
     * Thêm mới một quy tắc giao dịch lặp lại định kỳ (ví dụ: hóa đơn hàng tháng).
     *
     * @param rule Đối tượng [RecurringRule] cần thêm mới.
     */
    suspend fun insertRecurringRule(rule: RecurringRule)

    /**
     * Cập nhật quy tắc giao dịch lặp lại định kỳ đã tồn tại.
     *
     * @param rule Đối tượng [RecurringRule] chứa thông tin cập nhật.
     */
    suspend fun updateRecurringRule(rule: RecurringRule)

    /**
     * Xóa quy tắc giao dịch lặp lại dựa trên ID quy tắc.
     *
     * @param ruleId ID của quy tắc lặp lại cần xóa.
     */
    suspend fun deleteRecurringRule(ruleId: String)

    /**
     * Lấy danh sách mục tiêu tiết kiệm của người dùng hiện tại.
     *
     * @return Danh sách các mục tiêu tiết kiệm [PersonalGoal].
     */
    suspend fun getGoals(): List<PersonalGoal>

    /**
     * Thêm mới một mục tiêu tiết kiệm cá nhân.
     *
     * @param goal Đối tượng [PersonalGoal] cần thêm mới.
     */
    suspend fun insertGoal(goal: PersonalGoal)

    /**
     * Cập nhật tiến độ hoặc thông tin của một mục tiêu tiết kiệm.
     *
     * @param goal Đối tượng [PersonalGoal] chứa thông tin cập nhật.
     */
    suspend fun updateGoal(goal: PersonalGoal)

    /**
     * Xóa một mục tiêu tiết kiệm cá nhân.
     *
     * @param goalId ID của mục tiêu cần xóa.
     */
    suspend fun deleteGoal(goalId: String)

    /**
     * Lấy danh sách các ví/tài khoản tài chính cá nhân của người dùng hiện tại.
     *
     * @return Danh sách ví [PersonalWallet].
     */
    suspend fun getWallets(): List<PersonalWallet>

    /**
     * Thêm mới một ví/tài khoản tài chính (ví dụ: Tiền mặt, Thẻ ngân hàng).
     *
     * @param wallet Đối tượng [PersonalWallet] cần thêm mới.
     */
    suspend fun insertWallet(wallet: PersonalWallet)

    /**
     * Cập nhật số dư hoặc thông tin của một ví tài chính đã tồn tại.
     *
     * @param wallet Đối tượng [PersonalWallet] chứa thông tin cập nhật.
     */
    suspend fun updateWallet(wallet: PersonalWallet)

    /**
     * Xóa/ngừng lưu trữ một ví tài chính dựa trên ID ví.
     *
     * @param walletId ID của ví cần xóa.
     */
    suspend fun deleteWallet(walletId: String)
}
