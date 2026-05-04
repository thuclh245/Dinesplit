# Người D - Kế hoạch 4 tuần cuối

## 1. Vai trò của Người D
Người D là **Split + Smart Split Engine owner**.

D là người phải kéo module Split từ mức “UI prototype/mock” lên mức:
- data flow thật,
- split calculation thật,
- bill/group flow thật,
- settle summary thật.

## 2. Mục tiêu của D
Hoàn thiện:
- create group
- group detail
- create bill
- equal split
- custom split
- itemized split baseline
- payment status
- settle summary
- Smart Split Engine end-to-end mức MVP

## 3. Những gì D đã có nền
- GroupListScreen
- CreateGroupScreen
- CreateBillScreen
- GroupDetailScreen
- BillDetailScreen
- SettleSummaryScreen
- UI split khá rõ ràng

## 4. Những gì còn thiếu nhiều
- Group/Bill persistence thật
- model finalization
- equal/custom/itemized calculations thật
- payment status data flow
- bill detail data-driven
- settlement calculation thật
- refresh/update logic

## 5. Phần việc theo 4 tuần cuối

## Tuần cuối 1
### Việc phải làm
- khóa Group/Bill/Share/Settlement model với team
- khóa enum/status values:
  - splitType
  - paymentStatus
  - bill status
- chốt data contract cho:
  - create group
  - create bill
  - bill shares
  - settlement output
- chuẩn bị domain rules cho equal/custom/itemized baseline

### Deliverables
- split contracts final
- status/enum list final
- smart split input-output note
- form state plan

## Tuần cuối 2
### Việc phải làm
- hoàn thiện Create Group thật
- hoàn thiện Group Detail data-driven
- hoàn thiện Create Bill thật
- hoàn thiện equal split baseline
- hoàn thiện custom split baseline
- preview share trước khi lưu
- hiển thị bill list trong group detail

### Deliverables
- Group creation v1
- Group detail v1
- Create Bill v1
- equal/custom preview v1
- bill list v1

## Tuần cuối 3
### Việc phải làm
- hoàn thiện Bill Detail
- hoàn thiện payment status rows
- hoàn thiện itemized split baseline
- hoàn thiện paid/unpaid/partial state
- bắt đầu settle summary bằng logic thật
- phối hợp với C để gửi trigger points notification cho new bill/payment reminder

### Deliverables
- Bill Detail v1
- payment status flow v1
- itemized split basic v1
- settle summary base thật

## Tuần cuối 4
### Việc phải làm
- hoàn thiện Smart Split Engine end-to-end
- tính owed / paid / net balance
- xuất settlement rows rõ ràng
- fix bug create/update flow
- polish split states:
  - loading
  - empty
  - error
- chuẩn bị demo groups/bills/unpaid/partial/paid cases

### Deliverables
- Smart Split Engine MVP hoàn chỉnh
- Settle Summary chạy được
- Split QA pass
- split demo data ready

## 6. Out of scope của D
Không làm:
- notification center screen owner
- personal finance
- feed logic
- auth/profile ownership
- top-level app shell ownership

## 7. Definition of Done cho D
D đạt yêu cầu khi:
- user tạo group được,
- tạo bill được,
- equal/custom/itemized baseline chạy được,
- payment status cập nhật được,
- bill detail hiển thị đúng,
- settle summary chạy được,
- Smart Split Engine đủ thuyết phục để demo/bảo vệ.

## 8. Prompt mẫu cho chat agent của D
Bạn là coding agent cho Người D của dự án DineSplit Social.

Chỉ làm phần Split và Smart Split Engine trong 4 tuần cuối:
- create group
- group detail
- create bill
- equal/custom/itemized split baseline
- bill detail
- payment status
- settle summary
- smart split calculations

Không làm:
- notification center owner work
- personal finance
- feed
- auth/profile ownership
- top-level navigation ownership

Trước khi code, hãy trả lời:
1. phần Split nào bạn sẽ hoàn thiện,
2. file nào sẽ tạo/sửa,
3. assumption nào đang dùng,
4. phần nào defer vì không thuộc D.

Sau đó mới code.
