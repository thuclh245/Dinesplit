# C – Lead Personal: Checklist Tuần 2

> Tuần 1 tập trung vào **design/spec + data model**. Tuần 2 tập trung vào **implement UI placeholder + state + validation + navigation**.

## 1. Implement UI Screens (placeholder)

### 1.1 Personal Dashboard
- Dựng lại dashboard bằng Jetpack Compose dựa trên skeleton của A
- Hiển thị dữ liệu giả từ ViewModel/state
- Gắn đủ 3 trạng thái:
  - Loading state
  - Empty state
  - Has data state
- Có quick actions để test flow:
  - `+ Add Expense`
  - `+ Add Income`

### 1.2 Add Transaction Screen
- Dựng form thêm giao dịch thật, không chỉ mock layout
- State quản lý bằng ViewModel
- Có các field:
  - Amount
  - Type: Income / Expense
  - Category
  - Note
  - Date
- Có nút submit/cancel để test luồng

### 1.3 Transaction History
- Dựng LazyColumn danh sách giao dịch
- Hiển thị fake transactions qua state
- Có filter theo tháng và theo type
- Item phải có:
  - Category icon
  - Amount
  - Date
  - Note (optional)

### 1.4 Category UI (basic)
- Dựng dropdown hoặc list chọn category
- Hiển thị danh sách category cơ bản
- Chuẩn bị sẵn cho flow add transaction

---

## 2. State Management (MVVM)

### 2.1 PersonalViewModel
```kotlin
class PersonalViewModel : ViewModel() {
    val transactions: StateFlow<List<Transaction>>
    val uiState: StateFlow<PersonalUiState>

    fun addTransaction(transaction: Transaction)
    fun filterByMonth(month: Int, year: Int)
}
```

### 2.2 PersonalUiState
```kotlin
data class PersonalUiState(
    val isLoading: Boolean,
    val transactions: List<Transaction>,
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double
)
```

### 2.3 Quy ước state
- UI đọc state từ ViewModel
- Không giữ logic nghiệp vụ trực tiếp trong Composable
- Dữ liệu fake vẫn phải đi qua state flow

---

## 3. Validation Logic (Domain layer)
- Amount > 0
- Không cho submit nếu thiếu field
- Type bắt buộc phải chọn
- Category không được null
- Mapping input → `Transaction` object

---

## 4. Navigation Integration
- Từ Personal → Add Transaction
- Từ History → Detail (optional)
- Back từ màn con quay về Personal dashboard
- Đồng bộ route với navigation tổng của app

---

## 5. Deliverables Tuần 2
- [x] Personal module chạy được với placeholder
- [x] 4 màn hình:
  - Dashboard
  - Add Transaction
  - History
  - Category UI
- [x] ViewModel + state flow cơ bản
- [x] Validation logic
- [x] Navigation hoạt động

---

## 6. Tiến độ code hiện tại
- [x] 1.1 Personal Dashboard
- [x] 1.2 Add Transaction Screen
- [x] 1.3 Transaction History
- [x] 1.4 Category UI (basic)
- [x] 2.1 PersonalViewModel
- [x] 2.2 PersonalUiState
- [x] 2.3 Quy ước state
- [x] 3. Validation Logic (Domain layer)
- [x] 4. Navigation Integration

