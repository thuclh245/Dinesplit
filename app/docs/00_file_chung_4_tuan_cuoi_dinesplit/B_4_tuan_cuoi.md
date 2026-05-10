# Người B - Kế hoạch 4 tuần cuối

## 1. Vai trò của Người B
Người B là **Feed completion owner**.

B là người phải kéo module Feed từ mức “UI/mock/placeholder” lên mức “MVP chạy được thật”.

## 2. Mục tiêu của B
Hoàn thiện social flow cốt lõi:
- feed list thật,
- create post thật,
- post detail thật,
- like/comment thật,
- search basic thật,
- other/public user profile side cơ bản.

## 3. Những gì B đã có nền
- FeedScreen
- CreatePostScreen
- PostDetailScreen
- SearchScreen
- một phần UI social
- navigation entry cơ bản

## 4. Những gì còn thiếu nhiều
- data flow thật
- create post persistence
- upload error handling
- like/unlike logic
- comment add/read logic
- search results thật
- public profile side cơ bản
- empty/loading/error states chuẩn

## 5. Phần việc theo 4 tuần cuối

## Tuần cuối 1
### Việc phải làm
- khóa Post model / Comment model / Like model với team
- rà lại feed routes và dependencies
- xác định data source cho:
  - feed list
  - create post
  - post detail
  - comments
  - likes
  - search
- thay mock states bằng UI state thật ở mức cơ bản
- chốt contract upload flow với Storage

### Deliverables
- feed data contract v1
- model/status list ổn định
- create post flow note final
- feed state map

## Tuần cuối 2
### Việc phải làm
- hoàn thiện Feed Home với data thật/sample-real hybrid
- hoàn thiện Create Post:
  - chọn ảnh
  - caption
  - location
  - submit
- nối upload ảnh
- save post metadata
- render PostCard từ data thật
- mở Post Detail từ Feed Home

### Deliverables
- Feed Home v1 chạy được
- Create Post v1 chạy được
- PostCard v1 data-driven
- Post Detail mở từ feed đúng

## Tuần cuối 3
### Việc phải làm
- hoàn thiện like/unlike
- hoàn thiện comment list + comment submit
- hoàn thiện Post Detail states
- hoàn thiện Search basic:
  - search users
  - search posts
  - search places basic
- hoàn thiện public/other user profile side ở mức cơ bản
- phối hợp với C cho notification trigger points social

### Deliverables
- like/comment flow MVP
- Search basic v1
- Other user profile basic v1
- social notification trigger map

## Tuần cuối 4
### Việc phải làm
- fix bug Feed
- fix upload edge cases
- fix empty/error/loading state
- polish transitions:
  - feed -> post detail
  - feed -> create post
  - search -> user/post result
- chuẩn bị demo social data:
  - posts
  - comments
  - likes
  - user samples

### Deliverables
- Feed QA pass mức MVP
- social demo data ready
- Feed polished build

## 6. Out of scope của B
Không làm:
- notification center screen owner
- spending reminder
- personal dashboard
- split group/bill logic
- smart split engine
- top-level app shell owner work

## 7. Definition of Done cho B
B đạt yêu cầu khi:
- user xem được feed,
- tạo post được,
- mở post detail được,
- like/comment được,
- search basic dùng được,
- social flow đủ ổn để demo.

## 8. Prompt mẫu cho chat agent của B
Bạn là coding agent cho Người B của dự án DineSplit Social.

Chỉ làm phần Feed trong 4 tuần cuối:
- feed list
- create post
- post detail
- like/comment
- search basic
- other user profile basic
- social loading/empty/error states

Không làm:
- notification center owner work
- personal finance
- split bill
- smart split engine
- app shell/top-level ownership

Trước khi code, hãy trả lời:
1. bạn sẽ hoàn thiện flow Feed nào,
2. file nào sẽ tạo/sửa,
3. assumption nào đang dùng,
4. phần nào defer vì không thuộc B.

Sau đó mới code.
