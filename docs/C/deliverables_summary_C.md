# Deliverables Summary (Week 1 & Week 2)

> Không bao gồm Figma, chỉ ghi nhận deliverables kỹ thuật theo code hiện có.

| Week | Item | Status | Evidence | Owner |
|---|---|---|---|---|
| Week 1 | Transaction model draft | Done | `app/src/main/java/com/example/dinesplit/domain/model/Transaction.kt`, `app/src/main/java/com/example/dinesplit/domain/model/TransactionType.kt` | C |
| Week 1 | Category structure draft | Done | `app/src/main/java/com/example/dinesplit/presentation/personal/CategoryManagementScreen.kt`, `app/src/main/java/com/example/dinesplit/data/repository/PersonalRepository.kt`, `app/src/main/java/com/example/dinesplit/data/local/PersonalDatabaseHelper.kt` | C |
| Week 1 | Chart state list / chart models | Done | `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalChartModels.kt`, `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalRoute.kt` | C |
| Week 1 | Entry flow + filter/state groundwork | Done | `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalViewModel.kt`, `app/src/main/java/com/example/dinesplit/presentation/personal/TransactionTypeUiMapper.kt` | C |
| Week 2 | Personal screens + navigation wiring | Done | `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalScreen.kt`, `app/src/main/java/com/example/dinesplit/presentation/personal/AddTransactionScreen.kt`, `app/src/main/java/com/example/dinesplit/presentation/personal/HistoryScreen.kt`, `app/src/main/java/com/example/dinesplit/presentation/personal/TransactionDetailScreen.kt`, `app/src/main/java/com/example/dinesplit/presentation/personal/CategoryManagementScreen.kt`, `app/src/main/java/com/example/dinesplit/presentation/main/MainContainerScreen.kt` | C |
| Week 2 | Form state plan + validation draft | Done | `app/src/main/java/com/example/dinesplit/presentation/personal/AddTransactionScreen.kt`, `app/src/main/java/com/example/dinesplit/domain/validation/TransactionFormValidator.kt` | C |
| Week 2 | Runtime data readiness (SQLite sync + seed) | Done | `app/src/main/java/com/example/dinesplit/data/local/PersonalDatabaseHelper.kt`, `app/src/main/java/com/example/dinesplit/data/repository/PersonalRepository.kt`, `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalViewModel.kt`, `app/src/main/java/com/example/dinesplit/presentation/main/MainContainerScreen.kt` | C |
| Week 2 | Category CRUD + delete guard + feedback | Done | `app/src/main/java/com/example/dinesplit/presentation/personal/CategoryManagementScreen.kt`, `app/src/main/java/com/example/dinesplit/presentation/personal/PersonalViewModel.kt`, `app/src/main/java/com/example/dinesplit/data/repository/PersonalRepository.kt` | C |
| Week 2 | UI style refresh (theme + auth + feed + bottom nav icon) | Done | `app/src/main/java/com/example/dinesplit/ui/theme/Color.kt`, `app/src/main/java/com/example/dinesplit/ui/theme/Type.kt`, `app/src/main/java/com/example/dinesplit/ui/theme/Theme.kt`, `app/src/main/java/com/example/dinesplit/presentation/auth/LoginScreen.kt`, `app/src/main/java/com/example/dinesplit/presentation/auth/RegisterScreen.kt`, `app/src/main/java/com/example/dinesplit/presentation/feed/FeedScreen.kt`, `app/src/main/java/com/example/dinesplit/core/ui/AppPlaceholderScreen.kt`, `app/src/main/java/com/example/dinesplit/core/navigation/BottonTab.kt` | C |

## Quick Notes

- Week 1: Hoàn tất các draft kỹ thuật cốt lõi (model/state/structure) để mở đường cho implementation.
- Week 2: Hoàn tất màn hình placeholder, sau đó nâng cấp lên dữ liệu thật SQLite + chỉnh giao diện theo style mới.
- Nếu cần gửi mentor/PM, có thể đổi cột `Status` thành `Done / In Review / Follow-up` theo chuẩn team.
