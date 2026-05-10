# A-05 - Shared UI and Conventions

## Vai trò của file này
Người A là người dựng tầng khung, nên phải giữ cho app đồng bộ ngay từ đầu.

## Shared UI foundation nên có
### Components cơ bản
- AppScaffold
- AppTopBar
- AppBottomNavigation
- PrimaryButton
- SecondaryButton
- AppTextField
- PasswordTextField
- LoadingBlock
- EmptyStateBlock
- ErrorStateBlock
- UserAvatar

## Quy tắc style
- Không tự đặt style riêng cho từng auth screen
- Dùng spacing token nhất quán
- Dùng typography hierarchy nhất quán
- App bar và back behavior giống nhau
- Button states phải đồng nhất

## Form conventions
- label rõ ràng
- error text dễ hiểu
- disabled state đúng
- loading button khi submit
- preserve input khi submit fail nếu hợp lý

## Naming conventions đề xuất
### File/class
- `LoginScreen`
- `LoginViewModel`
- `LoginUiState`
- `RegisterScreen`
- `ProfileScreen`
- `EditProfileScreen`

### State/event/effect
- `UiState`
- `UiEvent`
- `UiEffect`

### Route constants
- tập trung ở 1 chỗ nếu có thể
- không hardcode string route khắp nơi

## Code conventions
- state immutable
- business logic không nằm trong composable
- composable chỉ render state và gửi event
- repository interface đặt ở domain nếu team đang theo hướng đó
- mapper tách riêng nếu DTO khác domain model

## Những lỗi thường gặp phải tránh
- ViewModel ôm quá nhiều trách nhiệm
- route string hardcode lung tung
- validation viết lặp
- Firebase code nằm trong UI
- dùng data class khác nhau cho cùng một ý nghĩa user profile
