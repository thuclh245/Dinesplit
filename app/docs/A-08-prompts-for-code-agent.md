# A-08 - Prompt mẫu để đưa cho code agent

## Prompt 1 - Bắt agent đọc và khóa phạm vi
Bạn là coding agent cho dự án Android Kotlin Jetpack Compose.

Hãy đọc toàn bộ các file markdown tôi cung cấp cho Người A.
Chỉ được triển khai đúng phạm vi của Người A:
- app shell
- navigation
- auth
- profile
- shared top-level UI foundation

Không tự ý mở rộng sang:
- feed business logic
- split bill business logic
- personal finance business logic
- assistant logic
- notification engine đầy đủ

Trước khi code, hãy trả lời:
1. Bạn hiểu vai trò Người A là gì?
2. Bạn sẽ tạo/sửa những file nào?
3. Những assumption nào bạn đang dùng?
4. Những phần nào bạn sẽ để placeholder?

Chỉ sau đó mới bắt đầu code.

## Prompt 2 - Ép agent output theo từng bước
Hãy triển khai phần Người A theo thứ tự này:
1. route/constants/navigation skeleton
2. auth graph + main graph
3. splash/session routing
4. login/register UI + ViewModel + validation
5. profile UI + edit profile
6. shared scaffold/components cơ bản
7. final cleanup

Ở mỗi bước:
- nêu file tạo/sửa
- code xong phải giải thích ngắn file đó làm gì
- không nhảy sang bước sau nếu bước hiện tại chưa rõ

## Prompt 3 - Chặn agent tự chế feature
Không được tự tạo thêm tính năng ngoài tài liệu.
Nếu thiếu thông tin:
- dùng assumption tối thiểu,
- nêu assumption rõ ràng,
- giữ code dễ thay thế,
- không tự phát minh nghiệp vụ mới.

## Prompt 4 - Ép agent theo kiến trúc
Tuân thủ:
- MVVM + lightweight Clean Architecture
- no Firebase calls inside composables
- immutable UiState
- use ViewModel for logic
- shared route constants
- reusable app scaffold where possible

## Prompt 5 - Ép agent bàn giao sạch
Sau khi code xong, hãy xuất ra:
1. Summary những gì đã làm
2. Danh sách file đã tạo/sửa
3. Placeholder nào còn lại
4. Risk/known issue
5. Hướng để người B/C/D tích hợp tiếp
