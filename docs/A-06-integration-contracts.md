# A-06 - Integration Contracts for Người A

## Mục tiêu
Người A phải làm sao để phần của mình không chặn các thành viên khác.

## Contract 1 - Main tabs phải sẵn sàng để cắm module
Main graph phải có slot hoặc route rõ cho:
- Feed
- Split
- Personal
- Profile

Dù module chưa xong, placeholder vẫn phải render ổn.

## Contract 2 - User session là source of truth chung
Các module khác sẽ giả định:
- user đã đăng nhập thì có `uid`
- current user profile có thể đọc được
- logout sẽ reset về auth an toàn

## Contract 3 - Profile model không đổi bừa
Nếu người A tạo user/profile model ban đầu thì phải:
- giữ tên field ổn định,
- không đổi liên tục,
- document rõ field nào bắt buộc.

## Contract 4 - Navigation contract rõ ràng
Team khác cần biết:
- route name
- cách mở tab
- cách navigate tới edit profile hoặc protected screens
- pattern deep link nội bộ nếu có

## Contract 5 - Shared scaffold phải dùng lại được
Nếu A tạo AppScaffold/AppTopBar thì module khác cần cắm vào được dễ dàng, không phụ thuộc auth code.

## Contract 6 - Error/loading pattern phải có chuẩn
Người A nên tạo pattern dùng lại:
- loading full-screen
- inline loading
- error with retry
- snackbar/message

## Những thứ phải chốt sớm với team
- user profile fields
- username có unique hay không
- avatar upload do A làm hay người khác hỗ trợ
- Google Sign-In có làm bản chính thức hay placeholder
- settings screen có thuộc A hay chỉ để sau

## Cách bàn giao cho team
Người A nên bàn giao ít nhất:
- sơ đồ route
- list shared components đã có
- auth/profile API hoặc repository contract
- danh sách file/folder đã tạo
- assumptions còn mở
