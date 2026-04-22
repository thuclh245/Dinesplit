package com.example.dinesplit.domain.usecase

import kotlin.math.abs
import kotlin.math.min

// --- CÁC MODEL DỮ LIỆU PHỤ TRỢ CHO USE CASE ---
// 1. Model đại diện cho 1 món ăn trong hóa đơn
data class SplitItem(
    val itemName: String,
    val price: Double,
    val consumerIds: List<String> // Danh sách ID những người ăn món này
)

// 2. Model đại diện cho một khoản nợ thô (A nợ B 50k)
data class RawDebt(
    val debtorId: String,   // Người mắc nợ
    val creditorId: String, // Người cho nợ (người đã trả tiền)
    val amount: Double
)

// 3. Model đại diện cho lệnh chuyển khoản chốt sổ cuối cùng
data class Settlement(
    val fromUserId: String,
    val toUserId: String,
    val amount: Double
)


class CalculateSplitUseCase {

    // ==========================================
    // 1. LOGIC CHIA ĐỀU (EQUAL SPLIT)
    // ==========================================
    fun calculateEqualSplit(totalAmount: Double, memberCount: Int): Double {
        if (memberCount <= 0) return 0.0
        return totalAmount / memberCount
        // Lưu ý: Với dự án thực tế dùng VNĐ, bạn nên cân nhắc dùng kiểu Long
        // và xử lý số dư (phần tiền lẻ) cộng dồn cho người tạo bill.
    }

    // ==========================================
    // 2. LOGIC TỰ NHẬP (CUSTOM SPLIT)
    // ==========================================
    fun validateCustomSplit(totalAmount: Double, memberAmounts: List<Double>): Boolean {
        val sum = memberAmounts.sum()
        // Dùng abs() để so sánh Double, tránh lỗi sai số thập phân (VD: 0.999999 != 1.0)
        return abs(totalAmount - sum) < 0.01
    }

    // ==========================================
    // 3. LOGIC CHIA THEO MÓN (ITEMIZED SPLIT)
    // ==========================================
    /**
     * Tính tiền từng người dựa trên món họ ăn, cộng thêm thuế phí chia theo tỷ lệ.
     * Trả về Map<UserId, Số tiền phải trả>
     */
    fun calculateItemizedSplit(
        items: List<SplitItem>,
        taxAndFee: Double
    ): Map<String, Double> {
        val subTotal = items.sumOf { it.price }
        val userSubtotals = mutableMapOf<String, Double>()

        // Bước 1: Tính tiền món ăn gốc cho từng người
        for (item in items) {
            if (item.consumerIds.isEmpty()) continue
            val splitPrice = item.price / item.consumerIds.size // Giá món chia đều cho số người ăn

            for (userId in item.consumerIds) {
                userSubtotals[userId] = userSubtotals.getOrDefault(userId, 0.0) + splitPrice
            }
        }

        // Bước 2: Phân bổ thuế/phí dịch vụ theo tỷ trọng (ai ăn nhiều trả thuế nhiều)
        val finalAmounts = mutableMapOf<String, Double>()
        for ((userId, amount) in userSubtotals) {
            // Tỷ trọng % của người này so với tổng bill
            val ratio = if (subTotal > 0) amount / subTotal else 0.0
            val proportionalTax = ratio * taxAndFee

            finalAmounts[userId] = amount + proportionalTax
        }

        return finalAmounts
    }

    // ==========================================
    // 4. SMART SPLIT ENGINE (THUẬT TOÁN GREEDY MAPPING)
    // ==========================================
    /**
     * Gom nhóm nợ nần chéo ngoe (A nợ B, B nợ C) thành các lệnh chuyển khoản trực tiếp
     */
    fun calculateSmartSplit(rawDebts: List<RawDebt>): List<Settlement> {
        val balances = mutableMapOf<String, Double>()

        // Bước 1: Tính Net Balance (Số dư ròng) cho mỗi người
        for (debt in rawDebts) {
            // debtor bị trừ tiền (âm)
            balances[debt.debtorId] = balances.getOrDefault(debt.debtorId, 0.0) - debt.amount
            // creditor được cộng tiền (dương)
            balances[debt.creditorId] = balances.getOrDefault(debt.creditorId, 0.0) + debt.amount
        }

        // Bước 2: Tách thành 2 nhóm: Người nợ (Debtors) và Chủ nợ (Creditors)
        // Lấy giá trị tuyệt đối của số âm để dễ tính toán
        val debtors = balances.filter { it.value < -0.01 }.map { it.key to abs(it.value) }.toMutableList()
        val creditors = balances.filter { it.value > 0.01 }.map { it.key to it.value }.toMutableList()

        val settlements = mutableListOf<Settlement>()

        // Bước 3: Thuật toán Tham lam (Greedy Algorithm)
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            // Luôn ưu tiên xử lý người nợ nhiều nhất và chủ nợ lớn nhất trước
            debtors.sortByDescending { it.second }
            creditors.sortByDescending { it.second }

            val maxDebtor = debtors.first()
            val maxCreditor = creditors.first()

            // Số tiền chốt sổ là số NHỎ HƠN giữa cục nợ và cục được nhận
            val settleAmount = min(maxDebtor.second, maxCreditor.second)

            // Sinh ra 1 lệnh chuyển khoản (Settlement)
            settlements.add(
                Settlement(
                    fromUserId = maxDebtor.first,
                    toUserId = maxCreditor.first,
                    amount = settleAmount
                )
            )

            // Cập nhật lại số dư sau khi đã bù trừ lệnh trên
            val newDebtorBalance = maxDebtor.second - settleAmount
            val newCreditorBalance = maxCreditor.second - settleAmount

            // Nếu ai đó đã hết nợ/đủ tiền thì xóa khỏi danh sách
            if (newDebtorBalance < 0.01) debtors.removeAt(0) else debtors[0] = maxDebtor.copy(second = newDebtorBalance)
            if (newCreditorBalance < 0.01) creditors.removeAt(0) else creditors[0] = maxCreditor.copy(second = newCreditorBalance)
        }

        return settlements
    }
}