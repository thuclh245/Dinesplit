# Project Scope v1 - DineSplit Social

## 1. Mục tiêu tài liệu
Tài liệu này chốt phạm vi cơ bản để dựng **khung chung** cho project trước khi chia việc sâu cho team.

## 2. Product statement
**DineSplit Social** là ứng dụng Android kết hợp:
- social feed về đồ ăn/địa điểm,
- quản lý chi tiêu cá nhân,
- chia bill theo nhóm,
trong cùng một trải nghiệm mobile nhất quán.

## 3. Định hướng triển khai
- Platform: Android
- Language: Kotlin
- UI toolkit: Jetpack Compose
- Architecture: MVVM + lightweight Clean Architecture
- Backend: Firebase Auth + Firestore + Storage + FCM
- Main tabs: Feed / Split / Personal / Profile
- Complex feature: Smart Split Engine
- Secondary assistant: Smart Query Assistant dạng rule-based

## 4. Mục tiêu chấm điểm
Ưu tiên:
- core flows chạy trọn,
- UI đồng bộ,
- logic màn hình rõ,
- notification có hoạt động,
- ít bug,
- complex feature nhìn thấy rõ trên mobile.

Không ưu tiên:
- AI nặng,
- backend quá phức tạp,
- scope rộng nhưng thiếu ổn định.

## 5. In-scope MVP

### 5.1 Auth
- Splash
- Login bằng email/password
- Register bằng email/password
- Google Sign-In
- Logout
- Giữ session đăng nhập

### 5.2 Profile
- Xem hồ sơ cá nhân
- Sửa hồ sơ cơ bản
- Upload/chỉnh avatar
- Follow user khác
- Xem public profile của user khác

### 5.3 Feed
- Xem feed từ followed users + một phần public posts
- Tạo post có image + caption + location
- Like post
- Comment post
- Xem post detail

### 5.4 Search
- Search users
- Search posts theo text đơn giản
- Search places theo locationName đã lưu

### 5.5 Personal Finance
- Add income
- Add expense
- Chọn category
- Xem transaction history
- Xem monthly summary
- Xem chart/category summary cơ bản

### 5.6 Split Bill
- Create group
- View group detail
- Add bill
- Equal split
- Custom split
- Theo dõi payment status
- Hiển thị debt relationships
- Settle summary

### 5.7 Notifications
- Like notification
- Comment notification
- New bill notification
- Payment reminder
- Spending reminder cơ bản

### 5.8 Smart Split Engine
- Tính amount owed theo split mode
- Tính net balance
- Tính ai nợ ai
- Sinh settle summary đơn giản, dễ demo

## 6. Out of scope cho bản khung v1
- Video post / reels / story
- Map integration sâu
- Full AI chatbot
- Receipt OCR hoặc scan hóa đơn phức tạp
- Auto recurring bill engine
- Advanced recommendation engine
- Admin dashboard
- Offline-first sync architecture đầy đủ

## 7. Realtime policy
Chỉ realtime cho phần thật sự cần:
- comments,
- bill/payment status,
- notifications.

Feed list, personal dashboard và reports có thể refresh theo load screen / pull-to-refresh.

## 8. Thành công của bản khung cơ bản
Khung được xem là đạt khi:
- có auth stack,
- có 4 tab chính,
- có navigation skeleton,
- có placeholder screens cho flow chính,
- có schema v1,
- có design guideline mini,
- có Firebase connection plan,
- team nhìn vào là hiểu toàn bộ structure app.

## 9. Ghi chú cho bước tiếp theo
Sau khi có khung này, mới chia task theo module để team code song song.
