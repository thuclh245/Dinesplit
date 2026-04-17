# Deliverables - Week 1 (Personal Module)

> Scope ghi nhận theo code/artefact kỹ thuật, **không bao gồm Figma**.

## 1) Transaction model draft

- Domain transaction model cho personal flow đã được định nghĩa.
- Bao gồm các trường chính: `id`, `userId`, `amount`, `type`, `categoryId`, `category`, `note`, `date`, `createdAt`.
- Thiết kế link category bằng `categoryId` để tránh sai lệch khi đổi tên category.

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/domain/model/Transaction.kt`
- `app/src/main/java/com/example/dinesplit/domain/model/TransactionType.kt`

## 2) Category structure draft

- Cấu trúc category cho personal đã được chuẩn hóa để dùng xuyên suốt UI/data.
- Có phân loại income/expense, custom/default, icon, description, active state.

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/presentation/personal/CategoryManagementScreen.kt`
- `app/src/main/java/com/example/dinesplit/data/repository/PersonalRepository.kt` (`StoredCategory`)
- `app/src/main/java/com/example/dinesplit/data/local/PersonalDatabaseHelper.kt` (schema + seed `categories`)

## 3) Chart state list / chart-ready models

- Bộ model cho chart đã có để render dashboard:
  - Pie chart by category
  - Daily expense bars
  - Monthly summary

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalChartModels.kt`
- `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalRoute.kt` (mapping từ transaction -> chart data)

## 4) Entry flow + filter/state groundwork

- Luồng thêm transaction và nền tảng filter tháng/type đã được định nghĩa.
- Làm nền cho dashboard/history vận hành nhất quán ở tuần sau.

**Code tham chiếu:**
- `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalViewModel.kt`
- `app/src/main/java/com/example/dinesplit/presentation/personal/TransactionTypeUiMapper.kt`
