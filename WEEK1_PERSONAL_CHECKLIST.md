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
- [x] Data models: `Transaction`, `Category`, `TransactionType`
- [x] List state + validation rules (loading/empty/has-data, form validation)
- [x] Chart requirement notes (đã chuẩn bị data models: pie/bar/monthly summary)

---

## 5. Tiến độ code hiện tại
- [x] 1.1 Personal Dashboard
- [x] 1.2 Add Transaction Screen
- [x] 1.3 Transaction History
- [x] 1.4 Category Management (basic)
- [x] 2.1 Transaction Model
- [x] 2.2 Category Model
- [x] 2.3 TransactionType enum
- [x] 3.1 Pie chart requirement (data contract)
- [x] 3.2 Bar chart requirement (data contract)
- [x] 3.3 Monthly summary requirement (data contract)

