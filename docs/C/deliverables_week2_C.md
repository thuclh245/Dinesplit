# Deliverables - Week 2 (Personal Placeholder Owner)

> Scope ghi nhận theo code/artefact kỹ thuật, **không bao gồm Figma**.

## 1) Personal screens + navigation wiring

Các màn core của personal flow đã được dựng và wired điều hướng đầy đủ:

- Personal Dashboard
- Add Transaction
- Transaction History
- Transaction Detail
- Category Management

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalScreen.kt`
- `app/src/main/java/com/example/dinesplit/presentation/personal/AddTransactionScreen.kt`
- `app/src/main/java/com/example/dinesplit/presentation/personal/HistoryScreen.kt`
- `app/src/main/java/com/example/dinesplit/presentation/personal/TransactionDetailScreen.kt`
- `app/src/main/java/com/example/dinesplit/presentation/personal/CategoryManagementScreen.kt`
- `app/src/main/java/com/example/dinesplit/presentation/main/MainContainerScreen.kt`

## 2) Form state plan + validation draft

- Form state của Add Transaction được define rõ ràng:
  - amount, type, category, note, date
  - submit attempt state
  - category option theo transaction type
- Validation input transaction tách module riêng và map về domain model.

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/presentation/personal/AddTransactionScreen.kt`
- `app/src/main/java/com/example/dinesplit/domain/validation/TransactionFormValidator.kt`

## 3) Runtime data readiness (SQLite sync + seed)

- Placeholder đã được nâng cấp dùng SQLite local để app chạy dữ liệu thật ngay:
  - schema `transactions`, `categories`
  - link transaction-category theo `category_id`
  - seed dữ liệu mẫu
  - repository + viewmodel sync
  - add transaction ghi DB và phản ánh lại dashboard/history/category
  - rename category cập nhật realtime ở chart/history/detail/category màn hình

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/data/local/PersonalDatabaseHelper.kt`
- `app/src/main/java/com/example/dinesplit/data/repository/PersonalRepository.kt`
- `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalViewModel.kt`
- `app/src/main/java/com/example/dinesplit/presentation/main/MainContainerScreen.kt`

## 4) Category CRUD + guardrails

- Category Management đã có create/update/delete category.
- Có guard: không cho xóa category đang được transaction sử dụng.
- Có snackbar feedback cho create/update/delete.
- Rename category không làm amount/progress về `0` do toàn bộ mapping dùng `categoryId`.

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/presentation/personal/CategoryManagementScreen.kt`
- `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalViewModel.kt`
- `app/src/main/java/com/example/dinesplit/data/repository/PersonalRepository.kt`
- `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalRoute.kt`
- `app/src/main/java/com/example/dinesplit/presentation/main/MainContainerScreen.kt`

## 5) UI style refresh (theo bộ style cuối)

- Đồng bộ theme màu + typography cho app.
- Làm mới Login/Register và các screen placeholder/feed theo style card-based.
- Bottom navigation dùng icon thật thay placeholder text.

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/ui/theme/Color.kt`
- `app/src/main/java/com/example/dinesplit/ui/theme/Type.kt`
- `app/src/main/java/com/example/dinesplit/ui/theme/Theme.kt`
- `app/src/main/java/com/example/dinesplit/presentation/auth/LoginScreen.kt`
- `app/src/main/java/com/example/dinesplit/presentation/auth/RegisterScreen.kt`
- `app/src/main/java/com/example/dinesplit/core/ui/AppPlaceholderScreen.kt`
- `app/src/main/java/com/example/dinesplit/presentation/feed/FeedScreen.kt`
- `app/src/main/java/com/example/dinesplit/core/navigation/BottonTab.kt`

## 6) Data persistence behavior

- Dữ liệu SQLite local được giữ lại sau khi tắt/mở app.
- Dữ liệu chỉ bị reset khi:
  - gỡ app/xóa app data, hoặc
  - tăng `DATABASE_VERSION` và `onUpgrade()` theo cơ chế drop/recreate.

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/data/local/PersonalDatabaseHelper.kt`

