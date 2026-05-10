# A-09 - Checklist để bạn review output của code agent

## Kiểm tra phạm vi
- Agent có chỉ làm phần A không?
- Có lấn sang Feed/Split/Personal quá sâu không?
- Có tự thêm feature ngoài docs không?

## Kiểm tra kiến trúc
- Có gọi Firebase trực tiếp trong UI không?
- Có ViewModel riêng cho màn hình chính không?
- Route có bị hardcode lung tung không?
- State có rõ ràng không?

## Kiểm tra UI
- Login/Register/Profile có cùng style không?
- Loading/error/validation có đủ không?
- Bottom nav có hoạt động thật không?

## Kiểm tra flow
- Splash -> Auth/Main đúng không?
- Register xong đi đâu?
- Complete profile xong đi đâu?
- Logout có quay về login và chặn back không?

## Kiểm tra khả năng tích hợp
- Placeholder của tab khác có ổn không?
- Team khác có thể cắm screen vào route hiện có không?
- Shared scaffold có tái sử dụng được không?

## Dấu hiệu output chưa đạt
- agent trả code rất dài nhưng không nói file nào làm gì
- logic nằm hết trong composable
- auth flow chạy demo được nhưng cấu trúc repo rối
- đổi tên field/model vô tội vạ
- thêm nhiều abstraction không cần thiết
