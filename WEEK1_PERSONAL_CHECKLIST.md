# C – Lead Personal: Checklist Tuần 1

## 1. Thiết kế UI (Figma)

### 1.1 Personal Dashboard
- Tổng chi tiêu tháng hiện tại
- Tổng thu nhập tháng
- Balance = income - expense
- Biểu đồ đơn giản (pie/bar)
- Quick actions:
  - `+ Add Expense`
  - `+ Add Income`
- Cần có 3 trạng thái:
  - Empty state
  - Loading state
  - Has data

### 1.2 Add Transaction Screen
- Amount (bắt buộc)
- Type: Income / Expense
- Category (dropdown)
- Note (optional)
- Date (mặc định = hôm nay)
- Validation:
  - Amount > 0
  - Category không được null
  - Type bắt buộc

### 1.3 Transaction History
- Danh sách giao dịch cuộn được
- Filter theo:
  - tháng
  - type (income / expense)
- Mỗi item gồm:
  - Category icon
  - Amount
  - Date
  - Note (optional)

### 1.4 Category Management (basic)
- Danh sách category cơ bản: Food, Drink, Travel...
- Nếu kịp thì cho thêm category custom

---

## 2. Data Model

### 2.1 Transaction Model
```kotlin
data class Transaction(
    val id: String,
    val userId: String,
    val amount: Double,
    val type: TransactionType, // INCOME / EXPENSE
    val category: String,
    val note: String?,
    val date: Long, // timestamp
    val createdAt: Long
)
```

### 2.2 Category Model
```kotlin
data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val type: TransactionType
)
```

### 2.3 Enum
```kotlin
enum class TransactionType {
    INCOME,
    EXPENSE
}
```

---

## 3. Chart Requirements

### 3.1 Pie Chart
- % theo category

### 3.2 Bar Chart
- Chi tiêu theo ngày trong tháng

### 3.3 Monthly Summary
- `total_income`
- `total_expense`
- `balance`

---

## 4. Deliverables Tuần 1
- [ ] Figma cho:
  - Personal Dashboard
  - Add Transaction
  - History
- [ ] Data models: `Transaction`, `Category`
- [ ] List state + validation rules
- [ ] Chart requirement notes

