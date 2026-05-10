# A-03 - Navigation and App Shell Spec

## Mục tiêu
Người A phải tạo một **app shell ổn định**, dễ cắm module, không làm team khác bị lệch.

## Cấu trúc điều hướng cấp cao
Ứng dụng có 2 vùng chính:
1. Auth graph
2. Main graph

## Luồng cấp cao
### App start
- App mở
- vào Splash
- kiểm tra session
- nếu chưa đăng nhập -> Auth graph
- nếu đã đăng nhập nhưng chưa hoàn thiện profile -> Complete Profile
- nếu đủ điều kiện -> Main graph

## Main graph
Bottom navigation gồm 4 tab:
- Feed
- Split
- Personal
- Profile

## Quy tắc điều hướng
### Auth screens
- Không được truy cập khi user đã đăng nhập hợp lệ, trừ trường hợp explicit logout
- Sau login/register thành công, route phải rõ ràng

### Protected screens
- Chỉ user đã đăng nhập mới vào được

### Logout
- Clear session liên quan
- Điều hướng về Login
- Không để back quay lại Main graph

## Khuyến nghị route naming
```text
splash
login
register
complete_profile
main/feed
main/split
main/personal
main/profile
edit_profile
settings
```

## App shell responsibilities
Người A nên tạo:
- MainScreen hoặc MainContainer
- BottomNav config
- top-level scaffold pattern
- app-wide snackbar host nếu cần
- common loading/error handling ở tầng route

## Placeholder policy
Với tab chưa do A làm:
- tạo placeholder screen rõ tiêu đề,
- không hardcode business logic giả phức tạp,
- chỉ cần màn hình không crash và giữ chỗ đúng route.

## Back behavior cần test
- Splash -> Login -> Register -> back
- Login -> Main -> back
- Profile -> Edit Profile -> save -> back
- Logout -> không back về màn cũ

## Điều không được làm
- Không nhúng logic business của module khác vào main shell
- Không tạo nested graph quá rối
- Không để mỗi tab tự xử lý scaffold khác nhau nếu chưa cần
