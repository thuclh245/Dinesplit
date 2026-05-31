# 📊 Personal Finance Module - Giải Thích Toàn Bộ

**Ngày cập nhật**: May 31, 2026

---

## 📚 Mục Lục

1. [Kiến Trúc Tổng Thể](#kiến-trúc-tổng-thể)
2. [Data Models (Định Nghĩa Dữ Liệu)](#data-models)
3. [Layer Repository (Lưu Trữ Dữ Liệu)](#layer-repository)
4. [ViewModel (Quản Lý State)](#viewmodel)
5. [UI Screens (Giao Diện)](#ui-screens)
6. [Workflow (Quy Trình Hoạt Động)](#workflow)
7. [Feature Details (Chi Tiết Tính Năng)](#feature-details)

---

## 🏗️ Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────────────────────────────┐
│                     UI Layer (Screens)                       │
│  PersonalScreen, PersonalPlansScreen, PersonalInsightsScreen│
├─────────────────────────────────────────────────────────────┤
│                   ViewModel (State Management)               │
│          PersonalViewModel (Quản lý state và logic)         │
├─────────────────────────────────────────────────────────────┤
│                 Repository (Data Access)                     │
│   FirebasePersonalRepository (Interface với Firestore)       │
├─────────────────────────────────────────────────────────────┤
│              Firebase Firestore (Backend Database)            │
│  Collections: categories, transactions, goals, wallets,      │
│  recurring_rules, reminders                                  │
└─────────────────────────────────────────────────────────────┘
```

**Luồng Data:**
```
Firestore ← Repository ← ViewModel ← UI State ← Screens
```

---

## 💾 Data Models

### 1. **Transaction** (Giao Dịch)
```kotlin
data class Transaction(
    val id: String,                    // ID duy nhất
    val userId: String,                // User sở hữu
    val amount: Double,                // Số tiền
    val type: TransactionType,         // INCOME hoặc EXPENSE
    val categoryId: String,            // ID danh mục
    val category: String,              // Tên danh mục
    val note: String?,                 // Ghi chú tùy chọn
    val date: Long,                    // Ngày (milliseconds)
    val createdAt: Long,               // Thời gian tạo
    val source: TransactionSource,     // MANUAL, SPLIT, RECURRING, RECEIPT
    val sourceGroupId: String?,        // ID nhóm (nếu từ Split)
    val sourceBillId: String?,         // ID bill (nếu từ Split)
    val recurringRuleId: String?,      // ID recurring rule
    val receiptImageUrl: String?,      // URL ảnh hóa đơn
    val walletId: String?              // ID ví (nếu có)
)
```

**Các loại source:**
- `MANUAL`: Thêm bằng tay
- `SPLIT`: Từ Split module 
- `RECURRING`: Từ recurring rule
- `RECEIPT`: Từ ảnh hóa đơn (OCR)

---

### 2. **Category** (Danh Mục)
```kotlin
data class StoredCategory(
    val id: String,              // ID duy nhất (ví dụ: "c_food")
    val name: String,            // Tên (ví dụ: "Dining Out")
    val icon: String,            // Icon code (ví dụ: "FD")
    val type: TransactionType,   // INCOME hoặc EXPENSE
    val isCustom: Boolean,       // User tạo hay default?
    val description: String,     // Mô tả
    val amountLabel: String,     // Hiển thị theo tháng (ví dụ: "50,000 VND")
    val progress: Float,         // Progress bar (0f to 1f)
    val isActive: Boolean        // Đang dùng?
)
```

**Default Categories:**
- **EXPENSE**: Food, Grocery, Transit, Entertainment
- **INCOME**: Salary, Bonus, Gift, Other

---

### 3. **SpendingReminder** (Nhắc Nhở Chi Tiêu)
```kotlin
data class SpendingReminder(
    val id: String,
    val userId: String,
    val categoryId: String?,         // null = tất cả categories
    val categoryName: String,        // "Overall" nếu null
    val budgetAmount: Double,        // Ngân sách
    val currentSpent: Double,        // Đã chi bao nhiêu?
    val threshold: Float,            // Cảnh báo ở 80% budget
    val reminderType: ReminderType,  // DAILY, WEEKLY, MONTHLY, MILESTONE
    val isEnabled: Boolean,
    val lastAlertedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
```

**Cách hoạt động:**
- User tạo budget (ví dụ: 1,000,000 VND cho Transit)
- Khi chi 800,000 VND (80% của 1,000,000) → Gửi notification cảnh báo
- Mỗi ngày chỉ cảnh báo 1 lần (check `lastAlertedAt`)

---

### 4. **RecurringRule** (Hóa Đơn Lặp Lại)
```kotlin
data class RecurringRule(
    val id: String,
    val userId: String,
    val name: String,               // "Netflix", "Electricity"
    val amount: Double,             // Số tiền
    val type: TransactionType,      // INCOME hoặc EXPENSE
    val categoryId: String,
    val categoryName: String,
    val cadence: RecurringCadence,  // WEEKLY hoặc MONTHLY
    val dayOfMonth: Int,            // Ngày thanh toán (1-31)
    val nextRunAt: Long,            // Lần chạy tiếp theo
    val isEnabled: Boolean,         // Bật/tắt
    val createdAt: Long,
    val updatedAt: Long
)
```

**Ví dụ:**
- Netflix: 100,000 VND, ngày 15 hàng tháng → Tự động tạo transaction vào ngày 15
- Salary: 10,000,000 VND, ngày 1 hàng tháng → Tự động tạo transaction income

---

### 5. **PersonalGoal** (Mục Tiêu Tiết Kiệm)
```kotlin
data class PersonalGoal(
    val id: String,
    val userId: String,
    val title: String,              // "Mua laptop", "Du lịch"
    val targetAmount: Double,       // 5,000,000 VND
    val currentAmount: Double,      // 1,500,000 VND (đã tiết kiệm)
    val categoryId: String?,        // null = overall goal
    val deadlineAt: Long,           // Hạn chót
    val status: GoalStatus,         // ACTIVE, COMPLETED, PAUSED
    val createdAt: Long,
    val updatedAt: Long
)
```

---

### 6. **PersonalWallet** (Ví Tiền)
```kotlin
data class PersonalWallet(
    val id: String,
    val userId: String,
    val name: String,               // "Tiền mặt", "Ngân hàng"
    val type: WalletType,           // CASH, BANK, EWALLET, CREDIT
    val balance: Double,            // Số dư hiện tại
    val color: String,              // Màu hiển thị (#AB2D00)
    val isArchived: Boolean,        // Lưu trữ hay hoạt động?
    val createdAt: Long,
    val updatedAt: Long
)
```

---

## 🔌 Layer Repository

**File:** `FirebasePersonalRepository.kt`

**Mục đích:** Bridge giữa ViewModel và Firestore. Xử lý tất cả CRUD operations.

### Firestore Collections Structure:

```
user_personal/
├── {userId}/                      ← Phân tách theo user
│   ├── categories/
│   │   ├── c_food (danh mục)
│   │   ├── c_salary
│   │   └── ...
│   ├── transactions/
│   │   ├── txn_001 (giao dịch)
│   │   ├── txn_002
│   │   └── ...
│   ├── reminders/
│   │   ├── rem_001 (nhắc nhở)
│   │   └── ...
│   ├── recurring_rules/
│   │   ├── rule_001 (hóa đơn lặp)
│   │   └── ...
│   ├── goals/
│   │   ├── goal_001 (mục tiêu)
│   │   └── ...
│   └── wallets/
│       ├── wallet_001 (ví)
│       └── ...
```

### Key Methods:

```kotlin
// Transactions
suspend fun getAllTransactions(): List<Transaction>
suspend fun insertTransaction(transaction: Transaction)
suspend fun updateTransaction(transaction: Transaction)

// Categories
suspend fun getCategories(): List<StoredCategory>
suspend fun insertCategory(category: StoredCategory)
suspend fun updateCategory(category: StoredCategory)
suspend fun deleteCategory(categoryId: String)

// Reminders
suspend fun getSpendingReminders(): List<SpendingReminder>
suspend fun insertSpendingReminder(reminder: SpendingReminder)
suspend fun deleteSpendingReminder(reminderId: String)

// Recurring Rules
suspend fun getRecurringRules(): List<RecurringRule>
suspend fun insertRecurringRule(rule: RecurringRule)
suspend fun deleteRecurringRule(ruleId: String)

// Goals
suspend fun getGoals(): List<PersonalGoal>
suspend fun insertGoal(goal: PersonalGoal)
suspend fun deleteGoal(goalId: String)

// Wallets
suspend fun getWallets(): List<PersonalWallet>
suspend fun insertWallet(wallet: PersonalWallet)
suspend fun deleteWallet(walletId: String)

// Utilities
suspend fun uploadReceiptImage(transactionId: String, receiptUri: Uri): Result<String>
```

### Security:
```kotlin
private fun requireCurrentUserId(): String {
    return FirebaseProviders.auth.currentUser?.uid
        ?: throw IllegalStateException("Please sign in to use Personal data")
}
```

Tất cả operations yêu cầu user đã login. Firestore rules kiểm tra `isOwner(uid)` trên backend.

---

## 🎮 ViewModel

**File:** `PersonalViewModel.kt`

**Mục đích:** 
- Quản lý state (transactions, categories, goals, etc.)
- Xử lý business logic (thêm, sửa, xóa)
- Gửi notifications
- Tính toán insights

### State Flows:

```kotlin
// Main data
val transactions: StateFlow<List<Transaction>>
val categories: StateFlow<List<StoredCategory>>
val reminders: StateFlow<List<SpendingReminder>>
val recurringRules: StateFlow<List<RecurringRule>>
val goals: StateFlow<List<PersonalGoal>>
val wallets: StateFlow<List<PersonalWallet>>

// Charts
val chartState: StateFlow<PersonalChartState>
// Contains: pieSlices, dailyExpenseBars, monthlySummary, insights, safeToSpend

// UI State
val uiState: StateFlow<PersonalUiState>
// Contains: isLoading, isSaving, errorMessage, totalIncome, totalExpense, balance
```

### Main Functions:

#### 1. **Add/Update/Delete Transactions**
```kotlin
fun addTransaction(transaction: Transaction)
    ↓
repository.insertTransaction()  // Save to Firestore
notificationRepository.insertNotification()  // Trigger notification
refreshStateInternal()  // Reload UI
```

#### 2. **Manage Categories**
```kotlin
fun addCategory(name, description, type, isCustom)
fun updateCategory(categoryId, name, type, ...)
    ↓ Nếu type thay đổi (EXPENSE → INCOME)
    ↓ Tất cả transactions với category này cũng thay đổi type
    ↓
refreshStateInternal()  // Reload UI
```

#### 3. **Spending Reminders**
```kotlin
fun addSpendingReminder(categoryId, budgetAmount, threshold, reminderType)
    ↓
repository.insertSpendingReminder()
checkSpendingReminders()  // Tính currentSpent + check ngưỡng
    ↓ Nếu spent >= threshold && chưa cảnh báo hôm nay
    ↓
notificationRepository.insertNotification()  // Cảnh báo
```

#### 4. **Recurring Rules**
```kotlin
fun addRecurringRule(name, amount, type, categoryId, cadence, dayOfMonth)
    ↓
repository.insertRecurringRule()
notificationRepository.insertNotification()
// Hệ thống backend sau sẽ tự động tạo transaction vào dayOfMonth
```

#### 5. **Goals & Wallets**
```kotlin
fun addGoal(title, targetAmount, currentAmount, categoryId)
fun addWallet(name, type, balance)
    ↓
repository.insertGoal/insertWallet()
notificationRepository.insertNotification()
refreshStateInternal()
```

### Performance Optimization:

```kotlin
// Debounce để tránh quá nhiều updates
private var lastUpdateCategoryTime = 0L
private val minUpdateIntervalMs = 500L  // 500ms

// Memory optimization: Load only current month by default
// Full load only when filtering or for reminders check
val monthFilter = currentMonthFilter.value
val filteredTransactions = if (monthFilter != null) {
    filterTransactions(allTransactions, monthFilter)
} else {
    // Load current month only
}

// Limit to 500 transactions per month
.take(500)
```

### Chart State Calculation:

```kotlin
_chartState.value = PersonalChartState(
    pieSlices = filteredTransactions.toPieCategorySlices(),  // Per-category breakdown
    dailyExpenseBars = filteredTransactions.toDailyExpenseBars(),  // Daily chart
    monthlySummary = filteredTransactions.toMonthlySummary(),  // Month totals
    insights = allTransactions.toMonthlyInsights(),  // Anomalies, trends
    safeToSpend = allTransactions.toSafeToSpendForecast(...)  // Spending forecast
)
```

---

## 📱 UI Screens

### 1. **PersonalScreen** (Main Tab)
**File:** `PersonalScreen.kt`

**Elements:**
- **Top Bar**: User avatar, refresh button
- **Quick Stats Card**: 
  - Income total
  - Expense total
  - Balance (Income - Expense)
  
- **Charts**:
  - Pie chart: Expense per category (%)
  - Daily bar chart: Spending per day
  - Monthly summary: Income vs Expense
  
- **Quick Actions**:
  - "Add Transaction" (FAB)
  - "History" button
  - "Categories" button
  - "Reminders" button
  
- **Intelligence Section**:
  - Anomaly Radar: Detect unusual spending
  - Autopilot Queue: Gợi ý next actions
  
- **Automation Count**: Number of recurring + goals + wallets

---

### 2. **PersonalPlansScreen** (Plans Tab)
**File:** `PersonalPlansScreen.kt`

**Contains:**
1. **Plan Cockpit**:
   - Monthly fixed expenses
   - Wallet total balance
   - Goal progress (%)

2. **Recurring Radar** (Add recurring bills):
   - Form: Name, Amount, DayOfMonth
   - List existing recurring rules
   - Delete button

3. **Add Goal Section**:
   - Form: Title, Target Amount
   - Deadline selection

4. **Add Wallet Section**:
   - Form: Name, Type (Cash/Bank/EWallet/Credit), Balance

---

### 3. **PersonalIntelligenceScreen** (Insights)
**File:** `PersonalIntelligenceScreen.kt`

**Shows:**
- Anomaly Radar: Unusual spending patterns
- Autopilot Queue: Smart recommendations
- Monthly trends
- Safe to spend forecast

---

### 4. **CategoryManagementScreen**
**File:** `CategoryManagementScreen.kt`

**Features:**
- List all categories (grouped by type)
- Add custom category
- Edit category (name, type, description)
- Delete category
- Toggle active/inactive

---

### 5. **AddTransactionScreen** (Add/Edit)
**File:** `AddEditTransactionScreen.kt`

**Form fields:**
- Amount (required)
- Category picker dropdown
- Transaction type (INCOME/EXPENSE)
- Date picker (Material3 DatePickerDialog)
- Note (optional)
- Receipt image upload (optional)

**Handling multi-screen state:**
```kotlin
private val AddEditTransactionInputSaver = run {
    mapSaver(  // Custom Saver vì rememberSaveable không hỗ trợ data class
        save = { input ->
            mapOf(
                "id" to input.id,
                "amount" to input.amount,
                "type" to input.type.name,
                "categoryId" to input.categoryId,
                "categoryName" to input.categoryName,
                "note" to input.note,
                "dateMillis" to input.dateMillis
            )
        },
        restore = { map ->
            // Restore from map
        }
    )
}

var input by rememberSaveable(stateSaver = AddEditTransactionInputSaver) {
    mutableStateOf(...)
}
```

---

### 6. **SpendingReminderScreen**
**File:** `SpendingReminderScreen.kt`

**Features:**
- View all reminders
- Add reminder (Budget, Category, Threshold)
- Delete reminder
- Show current spent vs budget

---

### 7. **HistoryScreen**
**File:** `HistoryScreen.kt`

**Features:**
- List all transactions (most recent first)
- Filter by category
- Filter by month
- View transaction details
- Delete transaction (with confirmation)

---

### 8. **MonthlySummaryScreen**
**File:** `MonthlySummaryScreen.kt`

**Shows:**
- Month selector
- Total income vs expense
- Category breakdown
- Pie chart
- Spending vs budget comparison

---

## 📊 Workflow (Quy Trình Hoạt Động)

### Scenario 1: User thêm transaction
```
1. User nhấn FAB "Add Transaction"
   ↓
2. PersonalScreen → AddEditTransactionScreen
   ↓
3. User nhập:
   - Amount: 500,000 VND
   - Category: "Dining Out"
   - Date: May 30, 2026
   - Note: "Lunch with team"
   ↓
4. User nhấn "Save"
   ↓
5. AddEditTransactionScreen gọi:
   PersonalViewModel.addTransaction(transaction)
   ↓
6. ViewModel xử lý:
   a) repository.insertTransaction()
      → Write to Firestore (user_personal/{uid}/transactions)
   b) notificationRepository.insertNotification()
      → Gửi notification "Transaction Added"
   c) refreshStateInternal()
      → Reload transactions, recalculate charts
   ↓
7. UI Update:
   - Chart recalculate: Pie chart update (Dining 500k)
   - Bar chart update (May 30 axis)
   - Balance recalculate
   - Dismiss screen → Back to PersonalScreen
```

### Scenario 2: User update category type
```
1. User vào CategoryManagementScreen
   ↓
2. Edit "Dining Out" category
   - Thay type từ EXPENSE → INCOME (lỗi logic nhưng possible)
   ↓
3. User nhấn "Save"
   ↓
4. ViewModel.updateCategory() kiểm tra:
   - Type thay đổi? (EXPENSE → INCOME)
   - Có → Cập nhật TẤT CẢ transactions với category này
   ↓
5. Cập nhật transactions:
   ```
   SELECT * FROM transactions WHERE categoryId = "c_food"
   UPDATE type = INCOME for each
   ```
   ↓
6. refreshStateInternal()
   - Tính lại balance
   - Tính lại pie chart (Dining OUT → Dining IN)
   - Tính lại income/expense totals
   ↓
7. Back to PersonalScreen, UI update ngay lập tức
```

### Scenario 3: User đặt spending reminder
```
1. User vào PersonalPlansScreen
   ↓
2. "Add Spending Reminder"
   - Category: "Dining Out"
   - Budget: 1,000,000 VND
   - Threshold: 80%
   - Reminder Type: MONTHLY
   ↓
3. ViewModel.addSpendingReminder()
   a) repository.insertSpendingReminder()
   b) Mỗi ngày, ViewModel.checkSpendingReminders():
      - Get current month transactions
      - Sum expenses cho category "Dining Out"
      - current = 950,000 VND
      - threshold = 1,000,000 * 0.8 = 800,000 VND
      - Nếu 950,000 >= 800,000 && chưa cảnh báo hôm nay
         → notificationRepository.insertNotification()
         → "You've spent 950K/1M on Dining Out"
```

### Scenario 4: Recurring rule auto-triggers
```
1. User thêm RecurringRule:
   - Name: "Netflix"
   - Amount: 100,000 VND
   - Cadence: MONTHLY
   - Day: 15
   ↓
2. Hệ thống backend (Firebase Cloud Function hoặc scheduled task):
   - Kiểm tra ngày 15 hàng tháng
   - Nếu rule.nextRunAt <= now
   - Tạo transaction tự động:
     Transaction(
       id = "recur_netflix_may2026",
       amount = 100,000,
       category = "Entertainment",
       date = May 15, 2026,
       source = TransactionSource.RECURRING,
       recurringRuleId = rule.id
     )
   - Cập nhật nextRunAt → June 15
   ↓
3. ViewModel detect transaction mới:
   - refreshStateInternal() auto-trigger (polling)
   - UI update: "Netflix charge 100K"
```

---

## 🎯 Feature Details

### A. **Anomaly Radar**
**Detect:**
- Unusual spending (vs. rolling average)
- Pacing risk (chi quá nhanh trong tháng)
- Split-heavy months (quá nhiều giao dịch chia)
- Category concentration (1 category > 50%)

**Implementation:**
```kotlin
insights = allTransactions.toMonthlyInsights()
// Calculate:
- avgMonthlySpend (last 3-6 months)
- currentMonthSpend
- anomaly = currentMonthSpend > avgMonthlySpend * 1.5
- categoryShareTop = max(categoryExpenses.values)
- concentrationRisk = categoryShareTop > 0.5
```

---

### B. **Autopilot Queue**
**Gợi ý hành động dựa trên:**
- Recurring radar: "Add recurring for top expense"
- Stretch goal: "Create goal for savings"
- Wallet coverage: "Add new wallet"

**Logic:**
```kotlin
val automationCount = uiState.recurringRules.size + 
                      uiState.goals.size + 
                      uiState.wallets.size

val recommendations = mutableListOf<AutopilotAction>()

// If nhiều category mà ít recurring → Suggest add recurring
if (categoryCount > recurringRules.size + 2) {
    recommendations.add(
        AutopilotAction(
            title = "Add recurring rule",
            description = "Set up automatic bills",
            target = SCREEN_PLANS
        )
    )
}

// If total expense cao mà ít goals → Suggest add goal
if (totalExpense > 1M && goals.isEmpty()) {
    recommendations.add(
        AutopilotAction(
            title = "Create savings goal",
            description = "Set a target to save",
            target = SCREEN_PLANS
        )
    )
}
```

---

### C. **Safe to Spend Forecast**
**Tính toán:**
```kotlin
fun toSafeToSpendForecast(
    upcomingRecurringExpense: Double,
    savingsGoal: Double
): SafeToSpendForecast {
    val currentMonthExpense = filteredTransactions
        .filter { it.type == EXPENSE }
        .sumOf { it.amount }
    
    val daysLeft = daysRemainingInMonth()
    val dailyRate = currentMonthExpense / daysPassed()
    val projectedMonthlyExpense = dailyRate * daysInMonth()
    
    val safeAmount = (100M VND budget) 
                   - projectedMonthlyExpense 
                   - upcomingRecurringExpense
                   - savingsGoal
    
    return SafeToSpendForecast(
        amount = safeAmount.coerceAtLeast(0.0),
        message = "You can safely spend $safeAmount by month end"
    )
}
```

---

### D. **Charts Rendering**

#### Pie Chart (Per-Category Breakdown)
```kotlin
LineItem.Icon = "FD"  // Dining icon
LineItem.Label = "Dining Out"
LineItem.Amount = 5,000,000 VND
LineItem.Percentage = 50%  // 5M / 10M total

// Visual:
🥘 Dining Out        50%  |████████████████████░░░░░░░░░░░░░░░
🚗 Transit           30%  |████████████░░░░░░░░░░░░░░░░░░░░░░░░░
🎬 Entertainment     20%  |████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░
```

#### Daily Bar Chart
```
May 24: █████ (500k)
May 25: ████████ (800k)
May 26: ██ (200k)
May 27: ███████████ (1.1M)
May 28: ████ (400k)
```

---

### E. **Notification Integration**

**Trigger Points:**
```kotlin
// 1. Transaction added
notificationFactory.transactionAdded(
    amount = 500k,
    categoryName = "Dining Out",
    type = EXPENSE,
    userId = uid,
    transactionId = id
)

// 2. Category created
notificationFactory.categoryCreated(
    categoryName = "Gym",
    type = EXPENSE,
    userId = uid
)

// 3. Reminder created
notificationFactory.reminderCreated(
    categoryName = "Dining Out",
    budgetAmount = 1M,
    userId = uid
)

// 4. Budget threshold exceeded
notificationFactory.fromReminderTrigger(
    PersonalReminderTrigger(
        categoryId = "c_food",
        categoryName = "Dining Out",
        currentSpent = 900k,
        budgetLimit = 1M,
        thresholdPercent = 0.8
    ),
    userId = uid
)

// 5. Split bill added to Personal
notificationFactory.fromPersonalTrigger(
    PersonalNotificationTrigger(
        relatedId = split.id,
        label = "Dinner with friends",
        amount = 250k,
        categoryName = "Dining Out",
        triggerType = SPLIT_BRIDGED_TO_PERSONAL
    ),
    userId = uid
)
```

**Notification Flow:**
```
ViewModel.addTransaction()
  ↓
notificationFactory.transactionAdded()  (create Notification object)
  ↓
notificationRepository.insertNotification()  (save to Firestore)
  ↓
App sends to NotificationService
  ↓
Android displays push notification
  ↓
User taps notification
  ↓
Deep link routes to PersonalScreen (with transaction data)
```

---

### F. **Receipt Image Upload**

**Flow:**
```
1. User selects image from gallery
2. Store as local Uri (content://)
3. Submit transaction with receiptImageUrl = "content://..."

4. ViewModel.addTransaction():
   a) transaction.withUploadedReceiptIfNeeded()
   b) Check if Uri is local (content:// or file://)
   c) If yes:
      - Upload to Firebase Storage at "receipts/{uid}/{transactionId}.jpg"
      - Get download URL
      - Update transaction.receiptImageUrl = "https://..."
      - Save updated transaction to Firestore
   d) transaction.source = TransactionSource.RECEIPT

5. Later, ViewModel can use OCR to extract amount/category from image
```

---

## 🔐 Security (Firestore Rules)

**File:** `firestore.rules`

```
match /user_personal/{uid} {
  allow read, write: if isOwner(uid);
  
  match /transactions/{docId} {
    allow create: if validPersonalTransaction();
    allow read, update, delete: if isOwner(uid);
  }
  
  match /categories/{docId} {
    allow create: if validPersonalCategory();
    allow read, update, delete: if isOwner(uid);
  }
  
  // ... reminders, recurring_rules, goals, wallets same pattern
}

function isOwner(uid) {
  return request.auth.uid == uid;
}

function validPersonalTransaction() {
  let data = request.resource.data;
  return data.amount is number 
      && data.type in ["INCOME", "EXPENSE"]
      && data.categoryId is string
      && data.date is number;
}
```

**Key Points:**
- ✅ User chỉ có thể read/write data của chính họ
- ✅ Backend validates data structure
- ✅ Các lệnh write phải pass validation trước

---

## 📈 Performance Tips

1. **Transaction Limit**: Load max 500 per month (prevent memory bloat)
2. **Debounce Category Updates**: Min 500ms between updates (prevent rapid UI recalcs)
3. **Lightweight Refresh**: `refreshCategoriesOnly()` khi chỉ category thay đổi (skip transaction reload)
4. **Chart Caching**: Use `remember()` để cache pie slices, bar data
5. **Lazy Load**: Load reminders in background (separate coroutine)
6. **Image Cache**: Coil ImageLoader bounded to 15% memory + 100MB disk

---

## 🐛 Common Issues & Solutions

### Issue 1: Chart shows 0% hoặc NaN
**Cause**: Division by zero hoặc percentage * 100 twice
**Fix**: 
```kotlin
val percentage = (amount / total) // NOT * 100
// UI handles formatting: String.format("%.1f%%", percentage * 100)
```

### Issue 2: Category update không reflect ngay
**Cause**: ViewModel không refresh state
**Fix**: 
```kotlin
updateCategory() {
    repository.updateCategory(...)
    refreshStateInternal()  // Always refresh after update
}
```

### Issue 3: Spending reminder không show
**Cause**: ViewModel chưa call `checkSpendingReminders()`
**Fix**:
```kotlin
fun refreshStateInternal() {
    syncSpendingReminders(allTransactions) // Launch in background
}
```

### Issue 4: Notifications từ Split không xuất hiện
**Cause**: Split module chưa call `PersonalViewModel.addSplitBillTransaction()`
**Fix**: Wire Split module → trigger notification

---

## 🚀 Deployment Checklist

- [ ] Firestore rules deployed
- [ ] Default categories exist
- [ ] Notifications trigger properly
- [ ] Charts render correctly
- [ ] Debounce prevents ANR
- [ ] Memory usage < 100MB
- [ ] APK size optimized
- [ ] Deep links work
- [ ] Reminders check daily
- [ ] Recurring rules auto-create (backend)

---

**End of Document**

