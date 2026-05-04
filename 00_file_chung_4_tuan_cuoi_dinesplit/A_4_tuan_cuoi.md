# Người A - Kế hoạch 4 tuần cuối

## 1. Vai trò mới của Người A
Người A **không còn là owner ôm nhiều việc như trước**.

Trong 4 tuần cuối, A chỉ giữ vai trò:
- giữ app ổn định ở tầng top-level,
- polish profile,
- giữ shared UI consistency,
- hỗ trợ integration nhẹ,
- fix route/common issue khi cần.

## 2. A không còn phụ trách chính
A **không còn owner** các phần sau:
- notification module,
- feed business logic,
- split business logic,
- personal business logic,
- smart split engine,
- reminder flow.

## 3. Những gì A được xem là đã có nền
- app shell
- auth flow cơ bản
- session routing cơ bản
- profile flow base
- navigation khung
- shared top-level structure

## 4. Mục tiêu của A trong 4 tuần cuối
### Mục tiêu chung
Giữ ứng dụng ổn định, nhất quán, không vỡ khi các module khác được cắm vào.

## 5. Phần việc của A theo 4 tuần cuối

## Tuần cuối 1
### Việc phải làm
- rà lại route/top-level navigation hiện có
- chốt lại route constants dùng chung
- rà AppNavHost/MainContainer/common entry
- rà protected route logic
- rà logout/back stack
- thống nhất lại shared component usage guideline ở mức tối thiểu
- liệt kê các bug nền tảng đang chặn B/C/D tích hợp

### Deliverables
- route map v2
- navigation issue list
- shared component usage note ngắn
- bug list nền tảng cho team

## Tuần cuối 2
### Việc phải làm
- polish ProfileScreen
- polish EditProfileScreen
- fix loading/error/empty state ở profile nếu còn thiếu
- avatar/profile update flow ở mức ổn định
- fix các issue top-level phát sinh khi B/C/D bắt đầu nối data thật

### Deliverables
- profile flow polished v1
- profile state handling ổn hơn
- shared fixes cho app shell

## Tuần cuối 3
### Việc phải làm
- hỗ trợ integration toàn app ở tầng top-level
- fix route mismatch giữa Feed / Split / Personal / Notification
- fix common UI inconsistency:
  - top bar
  - padding
  - state blocks
  - button states
- review build tích hợp

### Deliverables
- integrated top-level build ổn hơn
- common UI consistency fixes
- integration checklist của A

## Tuần cuối 4
### Việc phải làm
- regression support cho auth/profile/navigation
- fix crash/top-level bug còn lại
- hỗ trợ chốt flow demo:
  - login
  - profile
  - navigation
  - mở đúng tab/đúng screen
- rà release checklist phần A

### Deliverables
- auth/profile/navigation QA pass
- top-level bugs reduced
- demo flow support hoàn tất

## 6. Out of scope của A
Không làm:
- create post logic
- like/comment logic
- search logic
- add transaction logic
- category logic
- split equal/custom/itemized logic
- settle summary logic
- notification records logic
- spending reminder logic

## 7. Definition of Done cho A
A đạt yêu cầu khi:
- auth/profile/navigation không chặn module khác,
- profile đủ ổn để demo,
- app shell không bị lỗi top-level nghiêm trọng,
- shared UI consistency được cải thiện,
- không còn ôm việc sâu của B/C/D.

## 8. Prompt mẫu cho chat agent của A
Bạn là coding agent cho Người A của dự án DineSplit Social.

Chỉ làm đúng phần của Người A trong 4 tuần cuối:
- route/top-level navigation stability
- profile polish
- shared UI consistency
- integration support nhẹ
- auth/profile/navigation regression fixes

Không làm:
- notification owner tasks
- feed business logic
- personal business logic
- split business logic
- smart split engine

Trước khi code, hãy trả lời:
1. phần nào của A bạn sẽ làm,
2. file nào sẽ tạo/sửa,
3. assumption nào đang dùng,
4. phần nào defer vì không thuộc A.

Sau đó mới code.
