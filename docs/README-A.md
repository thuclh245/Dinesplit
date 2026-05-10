# Bộ tài liệu giao việc cho Người A

## Mục tiêu của bộ tài liệu này
Bộ tài liệu này được viết để bạn có thể đưa trực tiếp cho một **chat agent gen code** hoặc cho **người A** trong team triển khai mà:
- không bị lạc hướng,
- không tự ý mở rộng scope,
- không code lệch kiến trúc chung,
- không đụng vào phần của người khác quá sâu,
- và luôn bám đúng mục tiêu đồ án mobile.

## Vai trò của người A trong project
Người A là người phụ trách **nền tảng app + auth flow + profile flow + top-level navigation + shared shell**.

Nói đơn giản:
- A dựng khung app,
- A làm flow đăng nhập/đăng ký,
- A làm profile và session,
- A giữ app chạy ổn định ở tầng trên cùng,
- A không ôm hết business logic của Feed / Split / Personal.

## Tài liệu cần đọc theo thứ tự
1. `A-01-role-and-boundary.md`
2. `A-02-technical-scope.md`
3. `A-03-navigation-app-shell.md`
4. `A-04-auth-and-profile-spec.md`
5. `A-05-shared-ui-and-conventions.md`
6. `A-06-integration-contracts.md`
7. `A-07-definition-of-done.md`
8. `A-08-prompts-for-code-agent.md`

## Nguyên tắc sử dụng với code agent
Khi đưa cho code agent, hãy:
- đưa **ít nhất 3 file đầu tiên** trước,
- nói rõ agent chỉ được làm đúng phần A,
- yêu cầu agent **không suy diễn thêm feature ngoài tài liệu**,
- yêu cầu agent **luôn trả ra danh sách file sẽ tạo/sửa trước khi code**,
- yêu cầu agent **nêu assumptions rõ ràng** nếu thiếu dữ liệu.

## Câu lệnh khuyến nghị trước khi bắt đầu
Bạn có thể gửi cho agent như sau:

> Hãy đọc toàn bộ các file markdown tôi cung cấp.  
> Chỉ triển khai đúng phạm vi của Người A.  
> Không tự ý thêm tính năng ngoài tài liệu.  
> Trước khi code, hãy tóm tắt:
> 1. bạn hiểu vai trò của Người A là gì,  
> 2. những file bạn sẽ tạo/sửa,  
> 3. các assumption bạn đang dùng.  
> Sau đó mới bắt đầu viết code.

## Kết quả kỳ vọng từ người A
Sau khi làm xong phần A, project phải có:
- app shell rõ ràng,
- navigation top-level chạy ổn,
- auth flow hoàn chỉnh,
- profile flow cơ bản hoàn chỉnh,
- session routing rõ ràng,
- shared scaffold/component foundation đủ để các thành viên khác cắm vào.
