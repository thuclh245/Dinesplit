# A-07 - Definition of Done for Người A

## Người A chỉ được coi là xong khi đạt đủ các điều sau

### 1. App start and routing
- app mở lên không crash
- splash route đúng
- có session thì vào main
- không có session thì vào auth

### 2. Login/Register
- login chạy được happy path
- register chạy được happy path
- validation cơ bản hoạt động
- error message có hiển thị
- không duplicate submit

### 3. Profile flow
- complete profile chạy được
- own profile xem được
- edit profile lưu được
- logout dùng được

### 4. Main shell
- 4 bottom tabs hiển thị
- chuyển tab không crash
- route protected hợp lý
- back behavior chấp nhận được

### 5. Code quality
- không gọi Firebase trực tiếp trong composable
- ViewModel/state có tổ chức
- naming đủ rõ
- package structure không lộn xộn
- có thể merge mà không phá module khác

### 6. Reusable foundation
- shared scaffold hoặc top bar dùng lại được
- input/button/loading/error component dùng lại được ở mức cơ bản

### 7. Handover quality
Người A phải bàn giao được:
- file list đã tạo/sửa
- phần nào là placeholder
- phần nào đã thật sự chạy
- known issues
- assumptions còn mở

## Không đạt Definition of Done nếu
- UI nhìn xong nhưng route lỗi
- login chạy nhưng logout/back sai
- profile chỉ là mock không lưu thật
- bottom nav chỉ là hình thức
- code phụ thuộc hardcode quá nhiều vào phần chưa làm
