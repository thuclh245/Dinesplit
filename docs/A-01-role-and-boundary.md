# A-01 - Role and Boundary

## Vai trò chính của Người A
Người A chịu trách nhiệm cho **phần khung chung của ứng dụng** và các luồng nền tảng.

### Người A sở hữu chính
- App entry
- Splash / session check
- Auth navigation
- Main navigation with 4 bottom tabs
- Login
- Register
- Complete Profile / Edit Profile cơ bản
- Own Profile base
- Session persistence
- Logout
- Shared app scaffold ở mức top-level
- Top app structure để team khác gắn module vào

## Người A hỗ trợ phụ
- Shared component system base
- Route naming conventions
- App-wide state and navigation consistency
- Một phần settings/session logic

## Người A không được tự ý ôm thêm
Để tránh lệch scope, Người A **không tự ý** triển khai sâu các phần sau nếu chưa được phân công lại:
- Feed business logic chi tiết
- Create Post flow hoàn chỉnh
- Split bill calculation engine
- Personal finance calculation/chart logic
- Notification engine hoàn chỉnh
- Assistant logic
- Firebase schema redesign ngoài phạm vi auth/profile cần dùng

## Ranh giới kỹ thuật
Người A được phép:
- tạo navigation graph,
- tạo placeholder screens cho tab chưa hoàn thiện,
- define app shell,
- define auth-related state,
- create base profile repository/usecase/viewmodel nếu cần.

Người A không nên:
- viết Firestore call trực tiếp trong composable,
- hardcode data model của module khác,
- đặt ra enum/status mới không thống nhất với team,
- đổi package/folder structure sau khi team đã dùng.

## Mục tiêu chấm điểm mà phần A ảnh hưởng mạnh
Phần của A tác động trực tiếp tới:
- logic màn hình/chức năng,
- giao diện đồng bộ,
- ít lỗi,
- ấn tượng khi demo,
- mức độ “app có tổ chức”.

## Điều bắt buộc
Nếu có thiếu thông tin, Người A phải:
1. giữ nguyên kiến trúc đã khóa,
2. dùng placeholder/fallback hợp lý,
3. không tự mở rộng feature mới.
