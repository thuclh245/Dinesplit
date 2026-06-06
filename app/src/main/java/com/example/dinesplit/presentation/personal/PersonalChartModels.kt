package com.example.dinesplit.presentation.personal

import com.example.dinesplit.domain.model.GoalStatus
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import java.util.Calendar

/** Biểu đồ hình tròn được sử dụng trong các mô hình dữ liệu danh mục. */
data class PieCategorySlice(
    val category: String,
    val amount: Double,
    val percentage: Float,
)

/** Biểu đồ cột hàng ngày trong một tháng sẵn sàng để biểu diễn. */
data class DailyExpenseBar(
    val dayOfMonth: Int,
    val amount: Double,
)

/** Tổng hàng tháng được sử dụng trên bảng điều khiển, tóm tắt biểu đồ và thông báo. */
data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
)

/** Trạng thái biểu đồ kết hợp cho bảng điều khiển Cá nhân. */
data class PersonalChartState(
    val pieSlices: List<PieCategorySlice> = emptyList(),
    val dailyExpenseBars: List<DailyExpenseBar> = emptyList(),
    val monthlySummary: MonthlySummary = MonthlySummary(0.0, 0.0, 0.0),
    val insights: List<PersonalInsight> = emptyList(),
    val safeToSpend: SafeToSpendForecast = SafeToSpendForecast(),
)

enum class PersonalInsightTone {
    POSITIVE,
    WARNING,
    INFO,
}

data class PersonalInsight(
    val title: String,
    val message: String,
    val tone: PersonalInsightTone = PersonalInsightTone.INFO,
)

enum class SafeToSpendStatus {
    HEALTHY,
    WATCH,
    OVER,
}

data class SafeToSpendForecast(
    val dailyAmount: Double = 0.0,
    val daysLeft: Int = 0,
    val status: SafeToSpendStatus = SafeToSpendStatus.WATCH,
    val message: String = "Thêm thu nhập và chi tiêu để mở khóa hướng dẫn hàng ngày."
)

fun List<Transaction>.toMonthlySummary(): MonthlySummary {
    val income = filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expense = filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    return MonthlySummary(
        totalIncome = income,
        totalExpense = expense,
        balance = income - expense,
    )
}

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

internal fun List<RecurringRule>.toUpcomingRecurringExpense(
    referenceMillis: Long = System.currentTimeMillis(),
): Double = upcomingExpenseRules(referenceMillis).sumOf { it.amount }

private fun List<RecurringRule>.upcomingExpenseRules(referenceMillis: Long): List<RecurringRule> {
    val monthEnd = endOfMonthMillis(referenceMillis)
    return filter { rule ->
        rule.isEnabled &&
            rule.type == TransactionType.EXPENSE &&
            rule.nextRunAt > referenceMillis &&
            rule.nextRunAt <= monthEnd
    }
}

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
