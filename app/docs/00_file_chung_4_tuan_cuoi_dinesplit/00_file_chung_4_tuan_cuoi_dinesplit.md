# DineSplit Social - Kế hoạch 4 tuần cuối

## 1. Mục tiêu của bộ tài liệu này

Bộ tài liệu này được viết lại theo tình trạng thực tế hiện tại của project, không bám máy móc vào kế hoạch cũ.

Mục tiêu:

- giữ được mục tiêu MVP, notification, Smart Split Engine, integration, và demo ổn định.

## 2. Cơ sở để xây lại phân công

Dựa trên:

- docs kiến trúc/project hiện có,
- tình trạng repo thực tế mà bạn đã tự đánh giá:
  - A đang đi xa nhất ở phần nền tảng,
  - C đang có tiến độ tốt ở Personal,
  - B và D còn thiếu nhiều logic thật.

## 3. Nguyên tắc phân công mới

### Principle 1 - Không để A tiếp tục ôm quá nhiều

A không còn là owner của quá nhiều phần tích hợp

### Principle 2 - Ai đang mạnh phần nào thì nhận phần gần đó

- C đang ở Personal + Notification
- D đang làm Split → nhận luôn Smart Split Engine
- B đang làm Feed → phải hoàn thiện Feed thật, không dừng ở placeholder/UI mock

### Principle 3 - Giữ đúng định hướng mobile-first

Không đẩy team đi quá sâu vào backend hoặc abstraction không cần thiết.

### Principle 4 - Chỉ tập trung vào phần còn thiếu để ra demo tốt

4 tuần cuối không phải để “làm thêm cho nhiều”, mà để:

- chốt core flows,
- làm logic thật,
- tăng stability,
- chuẩn bị demo/bảo vệ.

## 4. Phân vai mới cho 4 tuần cuối

| Người | Vai trò chính 4 tuần cuối                               | Trọng tâm                                  |
| ----- | ------------------------------------------------------- | ------------------------------------------ |
| A     | Foundation stability + profile polish + integration nhẹ | giữ app ổn, không ôm logic sâu module khác |
| B     | Feed completion owner                                   | hoàn thiện social flow thật                |
| C     | Personal + Notification owner                           | hoàn thiện personal và owner notification  |
| D     | Split + Smart Split Engine owner                        | hoàn thiện split thật và settle logic      |

## 5. Những gì đã có rồi, không nên làm lại

### Người A

Đã có mức hoàn thiện cao ở:

- app shell
- auth flow cơ bản
- profile flow cơ bản
- navigation khung

### Người C

Đã có mức hoàn thiện khá cao ở:

- personal dashboard base
- add transaction
- history base
- category base
- local data flow phần lớn

### Người B

Đã có:

- khung Feed
- Create Post screen
- Post Detail screen
- Search screen
  Nhưng còn thiếu logic thật.

### Người D

Đã có:

- Group List
- Create Group
- Create Bill
- Group Detail
- Bill Detail
- Settle Summary
  Nhưng còn thiếu logic, data flow, split calculation thật.

## 6. Những gì toàn team phải khóa từ bây giờ

### Scope khóa

Không mở rộng thêm:

- assistant phức tạp,
- recommendation nâng cao,
- map integration sâu,
- feature ngoài MVP.

### Kiến trúc khóa

- Kotlin
- Jetpack Compose
- MVVM + lightweight Clean Architecture
- Firebase Auth / Firestore / Storage / FCM
- 4 bottom tabs: Feed / Split / Personal / Profile

### UI consistency khóa

Dùng chung:

- top app bar pattern,
- bottom nav,
- button styles,
- loading/empty/error blocks,
- card styles,
- spacing/token conventions.

## 7. Mục tiêu 4 tuần cuối theo toàn app

### Tuần cuối 1

- chốt lại contracts thật giữa module
- bắt đầu thay placeholder bằng logic thật ở Feed / Split / Notification
- A chỉ hỗ trợ nền tảng và consistency

### Tuần cuối 2

- Feed và Split phải có business flow thật chạy được
- Personal hoàn thiện các phần còn thiếu
- Notification có data model và UI flow thật

### Tuần cuối 3

- tích hợp toàn app
- sửa bug
- hoàn thiện loading/empty/error states
- chuẩn bị demo data

### Tuần cuối 4

- regression test
- polish UI
- chốt demo script
- chốt release candidate build

## 8. Definition of Done toàn app

App chỉ được coi là đạt nếu:

- auth/profile chạy ổn,
- feed create/like/comment/search chạy được ở mức MVP,
- personal add/history/summary/category chạy được,
- split create group/create bill/equal custom itemized baseline/payment status/settle summary chạy được,
- notification list + read/unread + deep link chạy được,
- major flows không crash,
- demo được trọn mạch.

## 9. Trách nhiệm phối hợp chung

### Weekly integration

Mỗi tuần phải có:

- 1 lần merge/integration,
- 1 checklist bug và mismatch,
- 1 cập nhật docs ngắn.

### Shared contracts bắt buộc giữ ổn định

- User/Profile model
- Post model
- Transaction model
- Group model
- Bill model
- Notification model
- enum/status values

## 10. Những gì không làm trong 4 tuần cuối

- refactor kiến trúc lớn
- thêm module mới
- thêm AI/chatbot phức tạp
- làm backend phức tạp hơn mức cần thiết
- thêm UI phụ không phục vụ demo và rubric

## 11. Cách dùng bộ file này với chat agent

Bạn nên:

1. đưa file chung này trước,
2. sau đó đưa file riêng của từng người,
3. yêu cầu agent chỉ làm đúng phần của người đó,
4. bắt agent liệt kê file sẽ sửa/tạo trước khi code,
5. bắt agent ghi rõ phần nào defer lại nếu vượt scope.
