# Người C - Kế hoạch 4 tuần cuối

## 1. Vai trò mới của Người C

Người C là **Personal + Notification owner**.

Vì C đang làm Personal tốt nhất

- notification center
- unread/read state
- notification list data flow
- spending reminder flow
- local reminder side
- deep link contract phối hợp với A/B/D

## 2. Mục tiêu của C

Hoàn thiện:

- Personal module gần như full MVP
- Notification module ở mức đủ để ăn điểm và demo
- Reminder flow ở mức thực dụng, không quá phức tạp

## 3. Những gì C đã có nền

- Personal dashboard base
- Add Transaction flow base
- recent history base
- category management base
- chart/dashboard base
- local data flow tốt hơn các module khác

## 4. Phần việc theo 4 tuần cuối

## Tuần cuối 1

### Việc phải làm

- khóa lại Transaction model / Category model / summary contract
- rà lại monthly aggregation logic
- rà dashboard state
- tiếp nhận owner của Notification:
  - notification model
  - list state
  - unread/read
  - deep link fields
- chốt spending reminder scope:
  - local only hoặc hybrid đơn giản

### Deliverables

- personal contract v2
- notification model v1
- reminder scope note
- dashboard state plan

## Tuần cuối 2

### Việc phải làm

- hoàn thiện Personal Dashboard:
  - monthly summary
  - recent transactions
  - category breakdown
  - chart data mapping
- hoàn thiện History:
  - order
  - filter basic
- hoàn thiện Category Management cơ bản
- dựng Notification screen base
- dựng notification list item state

### Deliverables

- Personal Dashboard v1 hoàn chỉnh hơn
- History v1
- Categories v1
- Notification screen base

## Tuần cuối 3

### Việc phải làm

- hoàn thiện add/edit flow nếu cần cho transaction
- fix empty/new user state
- polish chart + summary consistency
- hoàn thiện Notification module:
  - list notification
  - unread/read
  - open related destination theo contract
- hoàn thiện spending reminder UI/local flow
- phối hợp với B/D để nhận trigger points cho like/comment/bill/payment

### Deliverables

- Personal polished v2
- Notification center v1 chạy được
- spending reminder flow v1
- notification trigger contract final

## Tuần cuối 4

### Việc phải làm

- QA Personal
- QA Notification
- fix totals/filter/category issues
- fix reminder issues
- chuẩn bị demo finance data + notification demo data
- hỗ trợ chốt deep link behavior cùng A

### Deliverables

- Personal QA pass
- Notification QA pass
- demo transactions/categories ready
- demo notifications ready

## 5. Notification responsibility của C

### C chịu trách nhiệm chính

- notification list screen
- notification item rendering
- unread/read state
- notification data model
- spending reminder UI/local flow
- test các loại notification hiển thị đúng

### C phối hợp nhưng không ôm một mình

- B cung cấp trigger points social
- D cung cấp trigger points split/payment
- A hỗ trợ deep link shell/top-level navigation

## 6. Out of scope của C

Không làm:

- create post logic
- split engine logic
- group/bill calculation
- top-level navigation ownership
- auth/profile ownership

## 7. Definition of Done cho C

C đạt yêu cầu khi:

- add transaction/history/category/dashboard chạy ổn,
- monthly summary đúng,
- chart đủ ổn để demo,
- notification center mở được,
- unread/read có trạng thái,
- spending reminder có flow rõ,
- deep link notification hoạt động theo contract.

## 8. Prompt mẫu cho chat agent của C

Bạn là coding agent cho Người C của dự án DineSplit Social.

Chỉ làm phần:

- personal dashboard
- add transaction / history / categories
- summary / chart / filter
- notification center
- unread/read
- spending reminder UI/local flow

Không làm:

- feed business logic
- split business logic
- smart split engine
- auth/profile ownership
- top-level navigation ownership

Trước khi code, hãy trả lời:

1. phần Personal/Notification nào bạn sẽ làm,
2. file nào sẽ tạo/sửa,
3. assumption nào đang dùng,
4. phần nào defer vì không thuộc C.

Sau đó mới code.
