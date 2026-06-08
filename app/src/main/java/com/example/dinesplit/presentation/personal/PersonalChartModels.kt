package com.example.dinesplit.presentation.personal

import com.example.dinesplit.domain.model.GoalStatus
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import java.util.Calendar

/**
 * Mô hình dữ liệu đại diện cho một lát cắt (slice) trong biểu đồ hình tròn theo danh mục chi tiêu.
 *
 * @property category Tên danh mục chi tiêu.
 * @property amount Tổng số tiền chi tiêu cho danh mục này.
 * @property percentage Tỷ lệ phần trăm so với tổng chi tiêu.
 */
data class PieCategorySlice(
    val category: String,
    val amount: Double,
    val percentage: Float,
)

/**
 * Mô hình dữ liệu đại diện cho một cột trong biểu đồ chi tiêu hàng ngày trong tháng.
 *
 * @property dayOfMonth Ngày trong tháng (1-31).
 * @property amount Tổng số tiền chi tiêu trong ngày đó.
 */
data class DailyExpenseBar(
    val dayOfMonth: Int,
    val amount: Double,
)

/**
 * Mô hình tổng kết tài chính hàng tháng hiển thị trên bảng điều khiển (dashboard).
 *
 * @property totalIncome Tổng thu nhập trong tháng.
 * @property totalExpense Tổng chi tiêu trong tháng.
 * @property balance Số dư (thu nhập - chi tiêu).
 */
data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
)

/**
 * Trạng thái biểu đồ kết hợp cho bảng điều khiển tài chính cá nhân.
 * Bao gồm biểu đồ tròn, biểu đồ cột hàng ngày, tổng kết tháng, nhận xét phân tích và dự báo chi tiêu an toàn.
 *
 * @property pieSlices Danh sách lát cắt biểu đồ tròn theo danh mục.
 * @property dailyExpenseBars Danh sách cột chi tiêu hàng ngày.
 * @property monthlySummary Tổng kết thu chi tháng hiện tại.
 * @property insights Danh sách nhận xét phân tích tài chính.
 * @property safeToSpend Dự báo mức chi tiêu an toàn còn lại.
 */
data class PersonalChartState(
    val pieSlices: List<PieCategorySlice> = emptyList(),
    val dailyExpenseBars: List<DailyExpenseBar> = emptyList(),
    val monthlySummary: MonthlySummary = MonthlySummary(0.0, 0.0, 0.0),
    val insights: List<PersonalInsight> = emptyList(),
    val safeToSpend: SafeToSpendForecast = SafeToSpendForecast(),
)

/**
 * Enum định nghĩa giọng điệu (tone) của nhận xét phân tích tài chính.
 * Ảnh hưởng đến màu sắc và biểu tượng hiển thị trên giao diện.
 */
enum class PersonalInsightTone {
    /** Tích cực - Xu hướng tốt, tiết kiệm được chi tiêu. */
    POSITIVE,
    /** Cảnh báo - Xu hướng chi tiêu tăng hoặc vượt mức. */
    WARNING,
    /** Thông tin - Gợi ý hoặc thống kê trung lập. */
    INFO,
}

/**
 * Mô hình dữ liệu cho một nhận xét phân tích tài chính cá nhân.
 *
 * @property title Tiêu đề ngắn gọn của nhận xét.
 * @property message Nội dung chi tiết của nhận xét.
 * @property tone Giọng điệu hiển thị [PersonalInsightTone].
 */
data class PersonalInsight(
    val title: String,
    val message: String,
    val tone: PersonalInsightTone = PersonalInsightTone.INFO,
)

/**
 * Enum định nghĩa trạng thái sức khỏe tài chính dựa trên dự báo chi tiêu an toàn.
 */
enum class SafeToSpendStatus {
    /** Khỏe mạnh - Còn dư địa chi tiêu thoải mái. */
    HEALTHY,
    /** Cần theo dõi - Mức chi tiêu hàng ngày thấp, cần thận trọng. */
    WATCH,
    /** Vượt ngưỡng - Đã chi tiêu vượt quá thu nhập tháng. */
    OVER,
}

/**
 * Mô hình dự báo mức chi tiêu an toàn hàng ngày trong phần còn lại của tháng.
 *
 * @property dailyAmount Số tiền an toàn có thể chi tiêu mỗi ngày.
 * @property daysLeft Số ngày còn lại trong tháng.
 * @property status Trạng thái sức khỏe tài chính [SafeToSpendStatus].
 * @property message Thông điệp hướng dẫn hiển thị cho người dùng.
 */
data class SafeToSpendForecast(
    val dailyAmount: Double = 0.0,
    val daysLeft: Int = 0,
    val status: SafeToSpendStatus = SafeToSpendStatus.WATCH,
    val message: String = "Thêm thu nhập và chi tiêu để mở khóa hướng dẫn hàng ngày."
)

/**
 * Tính toán tổng kết tài chính hàng tháng từ danh sách giao dịch.
 *
 * @return Đối tượng [MonthlySummary] chứa tổng thu nhập, tổng chi tiêu và số dư.
 */
fun List<Transaction>.toMonthlySummary(): MonthlySummary {
    val income = filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense = filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    return MonthlySummary(
        totalIncome = income,
        totalExpense = expense,
        balance = income - expense,
    )
}

/**
 * Chuyển đổi danh sách giao dịch thành danh sách lát cắt biểu đồ tròn theo danh mục chi tiêu.
 * Chỉ bao gồm các giao dịch chi tiêu (EXPENSE), sắp xếp giảm dần theo giá trị.
 *
 * @return Danh sách [PieCategorySlice] sẵn sàng vẽ biểu đồ tròn.
 */
fun List<Transaction>.toPieCategorySlices(): List<PieCategorySlice> {
    val expenseTransactions = filter { it.type == TransactionType.EXPENSE }
    if (expenseTransactions.isEmpty()) return emptyList()

    val totalsByCategory =
        expenseTransactions.groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

    val totalAmount = totalsByCategory.values.sum()
    if (totalAmount <= 0.0) return emptyList()

    return totalsByCategory.entries
        .sortedByDescending { it.value }
        .map { (category, amount) ->
            PieCategorySlice(
                category = category,
                amount = amount,
                percentage = ((amount / totalAmount) * 100.0).toFloat(),
            )
        }
}

/**
 * Chuyển đổi danh sách giao dịch thành danh sách cột biểu đồ chi tiêu hàng ngày.
 * Nhóm các giao dịch chi tiêu theo ngày trong tháng và tính tổng cho mỗi ngày.
 *
 * @return Danh sách [DailyExpenseBar] sắp xếp theo thứ tự ngày tăng dần.
 */
fun List<Transaction>.toDailyExpenseBars(): List<DailyExpenseBar> {
    if (isEmpty()) return emptyList()

    val expenseByDay =
        filter { it.type == TransactionType.EXPENSE }
            .groupBy { transaction ->
                Calendar.getInstance().apply { timeInMillis = transaction.date }
                    .get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

    return expenseByDay.entries
        .sortedBy { it.key }
        .map { (day, amount) ->
            DailyExpenseBar(
                dayOfMonth = day,
                amount = amount,
            )
        }
}

/**
 * Phân tích và tạo danh sách nhận xét tài chính hàng tháng dựa trên so sánh chi tiêu với tháng trước,
 * xác định danh mục chi tiêu lớn nhất, tính tỉ lệ hóa đơn chia tiền, và tìm ngày chi tiêu cao nhất trong tuần.
 * Tối đa trả về 3 nhận xét; nếu không đủ dữ liệu, trả về gợi ý mặc định.
 *
 * @param referenceMillis Thời điểm mốc tham chiếu (mặc định là thời gian hiện tại).
 * @return Danh sách tối đa 3 nhận xét [PersonalInsight].
 */
fun List<Transaction>.toMonthlyInsights(referenceMillis: Long = System.currentTimeMillis()): List<PersonalInsight> {
    val currentMonth = filterByMonthOffset(referenceMillis, 0)
    val previousMonth = filterByMonthOffset(referenceMillis, -1)
    val currentSummary = currentMonth.toMonthlySummary()
    val previousSummary = previousMonth.toMonthlySummary()
    val insights = mutableListOf<PersonalInsight>()

    if (previousSummary.totalExpense > 0.0) {
        val delta = currentSummary.totalExpense - previousSummary.totalExpense
        val percent = (kotlin.math.abs(delta) / previousSummary.totalExpense * 100.0).toInt()
        insights += if (delta > 0.0) {
             PersonalInsight(
                 title = "Chi tiêu tăng",
                 message = "Bạn đã chi tiêu $percent% nhiều hơn tháng trước cho đến nay.",
                 tone = PersonalInsightTone.WARNING
             )
         } else {
             PersonalInsight(
                 title = "Chi tiêu giảm",
                 message = "Bạn đã chi tiêu $percent% ít hơn tháng trước cho đến nay.",
                 tone = PersonalInsightTone.POSITIVE
             )
         }
    }

    currentMonth
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .maxByOrNull { it.value }
        ?.let { (category, amount) ->
             insights += PersonalInsight(
                 title = "Danh mục hàng đầu",
                 message = "$category là chi tiêu lớn nhất của bạn trong tháng này.",
                 tone = if (amount > currentSummary.totalIncome && currentSummary.totalIncome > 0.0) {
                     PersonalInsightTone.WARNING
                 } else {
                     PersonalInsightTone.INFO
                 }
             )
        }

    val splitExpense =
        currentMonth
            .filter { it.type == TransactionType.EXPENSE && it.source.name == "SPLIT" }
            .sumOf { it.amount }
    if (splitExpense > 0.0 && currentSummary.totalExpense > 0.0) {
        val percent = (splitExpense / currentSummary.totalExpense * 100.0).toInt()
         insights += PersonalInsight(
             title = "Tác động chia tiền",
             message = "Hóa đơn chia tiền chiếm $percent% chi tiêu hàng tháng của bạn.",
             tone = PersonalInsightTone.INFO
         )
    }

    val biggestDay =
        currentMonth
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { transaction ->
                Calendar.getInstance().apply { timeInMillis = transaction.date }.get(Calendar.DAY_OF_WEEK)
            }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .maxByOrNull { it.value }

    if (biggestDay != null) {
         insights += PersonalInsight(
             title = "Mô hình chi tiêu",
             message = "${dayName(biggestDay.key)} là ngày chi tiêu cao nhất của bạn trong tháng này.",
             tone = PersonalInsightTone.INFO
         )
    }

     return insights.take(3).ifEmpty {
         listOf(
             PersonalInsight(
                 title = "Bắt đầu theo dõi",
                 message = "Thêm một vài mục nhập khác để mở khóa các mô hình hàng tháng.",
                 tone = PersonalInsightTone.INFO
             )
         )
     }
}

/**
 * Tính dự báo mức chi tiêu an toàn hàng ngày cho phần còn lại của tháng.
 * Công thức: (Thu nhập - Chi tiêu - Chi phí định kỳ sắp tới - Dự trữ tiết kiệm) / Số ngày còn lại.
 *
 * @param referenceMillis Thời điểm mốc tham chiếu.
 * @param upcomingRecurringExpense Tổng chi phí định kỳ sắp phát sinh trong tháng.
 * @param savingsGoal Tổng dự trữ cho mục tiêu tiết kiệm đang hoạt động.
 * @return Đối tượng [SafeToSpendForecast] chứa mức chi tiêu an toàn và trạng thái sức khỏe tài chính.
 */
fun List<Transaction>.toSafeToSpendForecast(
    referenceMillis: Long = System.currentTimeMillis(),
    upcomingRecurringExpense: Double = 0.0,
    savingsGoal: Double = 0.0,
): SafeToSpendForecast {
    val monthTransactions = filterByMonthOffset(referenceMillis, 0)
    val summary = monthTransactions.toMonthlySummary()
    val calendar = Calendar.getInstance().apply { timeInMillis = referenceMillis }
    val daysLeft = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_MONTH) + 1
    val available = summary.totalIncome - summary.totalExpense - upcomingRecurringExpense - savingsGoal
    val daily = if (daysLeft > 0) (available / daysLeft).coerceAtLeast(0.0) else 0.0
    val status = when {
        available <= 0.0 -> SafeToSpendStatus.OVER
        daily < 100_000.0 -> SafeToSpendStatus.WATCH
        else -> SafeToSpendStatus.HEALTHY
    }
    val message = when (status) {
        SafeToSpendStatus.HEALTHY -> "Bạn vẫn còn dư địa chi tiêu và đang đi đúng kế hoạch."
        SafeToSpendStatus.WATCH -> "Hãy giữ chi tiêu thật chặt trong phần còn lại của tháng."
        SafeToSpendStatus.OVER -> "Bạn đã vượt vùng đệm tháng này. Tạm dừng các khoản không thiết yếu."
    }

    return SafeToSpendForecast(
        dailyAmount = daily,
        daysLeft = daysLeft,
        status = status,
        message = message,
    )
}

/**
 * Tính tổng số tiền dự trữ cần thiết cho các mục tiêu tiết kiệm đang hoạt động (ACTIVE).
 * Trừ đi các chi phí định kỳ đã bao phủ cho danh mục liên kết. Giới hạn tối đa bởi [reserveCap].
 *
 * @param categoryTypesById Bản đồ ánh xạ ID danh mục sang loại giao dịch.
 * @param recurringRules Danh sách quy tắc lặp lại định kỳ.
 * @param reserveCap Mức trần dự trữ tối đa (mặc định 5 triệu VND).
 * @param referenceMillis Thời điểm mốc tham chiếu.
 * @return Tổng số tiền dự trữ cần thiết.
 */
internal fun List<PersonalGoal>.toPlanReserve(
    categoryTypesById: Map<String, TransactionType>,
    recurringRules: List<RecurringRule> = emptyList(),
    reserveCap: Double = 5_000_000.0,
    referenceMillis: Long = System.currentTimeMillis(),
): Double {
    val recurringExpenseByCategoryId =
        recurringRules
            .upcomingExpenseRules(referenceMillis)
            .groupBy { it.categoryId }
            .mapValues { (_, rules) -> rules.sumOf { it.amount } }

    return filter { it.status == GoalStatus.ACTIVE }
        .sumOf { goal ->
            val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
            val categoryId = goal.categoryId?.takeIf { it.isNotBlank() }
            when (categoryId?.let { categoryTypesById[it] }) {
                null -> remaining
                TransactionType.INCOME -> remaining
                TransactionType.EXPENSE -> {
                    val coveredByRecurring = recurringExpenseByCategoryId[categoryId] ?: 0.0
                    (remaining - coveredByRecurring).coerceAtLeast(0.0)
                }
            }
        }
        .coerceAtMost(reserveCap)
}

/**
 * Tính tổng chi phí định kỳ sắp phát sinh trong phần còn lại của tháng hiện tại.
 *
 * @param referenceMillis Thời điểm mốc tham chiếu.
 * @return Tổng số tiền chi phí định kỳ sắp tới.
 */
internal fun List<RecurringRule>.toUpcomingRecurringExpense(
    referenceMillis: Long = System.currentTimeMillis(),
): Double = upcomingExpenseRules(referenceMillis).sumOf { it.amount }

/**
 * Lọc các quy tắc lặp lại chi tiêu đang bật và có lịch chạy kế tiếp nằm trong phần còn lại của tháng.
 *
 * @param referenceMillis Thời điểm mốc tham chiếu.
 * @return Danh sách các quy tắc chi tiêu sắp chạy.
 */
private fun List<RecurringRule>.upcomingExpenseRules(referenceMillis: Long): List<RecurringRule> {
    val monthEnd = endOfMonthMillis(referenceMillis)
    return filter { rule ->
        rule.isEnabled &&
            rule.type == TransactionType.EXPENSE &&
            rule.nextRunAt > referenceMillis &&
            rule.nextRunAt <= monthEnd
    }
}

/**
 * Lọc danh sách giao dịch theo tháng tương đối so với thời điểm mốc tham chiếu.
 *
 * @param referenceMillis Thời điểm mốc tham chiếu.
 * @param offset Độ lệch tháng (0 = tháng hiện tại, -1 = tháng trước, v.v.).
 * @return Danh sách giao dịch thuộc tháng được chỉ định.
 */
private fun List<Transaction>.filterByMonthOffset(
    referenceMillis: Long,
    offset: Int,
): List<Transaction> {
    val target =
        Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            add(Calendar.MONTH, offset)
        }
    val targetMonth = target.get(Calendar.MONTH)
    val targetYear = target.get(Calendar.YEAR)

    return filter { transaction ->
        val calendar = Calendar.getInstance().apply { timeInMillis = transaction.date }
        calendar.get(Calendar.MONTH) == targetMonth && calendar.get(Calendar.YEAR) == targetYear
    }
}

/**
 * Tính thời điểm kết thúc của tháng chứa thời điểm truyền vào (23:59:59.999 ngày cuối tháng).
 *
 * @param referenceMillis Thời điểm mốc tham chiếu.
 * @return Thời điểm cuối tháng tính bằng mili-giây.
 */
private fun endOfMonthMillis(referenceMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

/**
 * Chuyển đổi hằng số ngày trong tuần của [Calendar] sang tên tiếng Việt tương ứng.
 *
 * @param dayOfWeek Hằng số ngày trong tuần (Calendar.MONDAY, Calendar.TUESDAY, v.v.).
 * @return Tên ngày tiếng Việt.
 */
private fun dayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        Calendar.MONDAY -> "Thứ Hai"
        Calendar.TUESDAY -> "Thứ Ba"
        Calendar.WEDNESDAY -> "Thứ Tư"
        Calendar.THURSDAY -> "Thứ Năm"
        Calendar.FRIDAY -> "Thứ Sáu"
        Calendar.SATURDAY -> "Thứ Bảy"
        Calendar.SUNDAY -> "Chủ Nhật"
        else -> "Ngày này"
    }
}
