# A-04 - Auth and Profile Specification

## Mục tiêu
Auth và Profile của Người A phải đủ hoàn chỉnh để toàn app có nền tảng dùng được thật.

## Auth screens

### 1. Splash
#### Mục đích
- kiểm tra session
- xác định route tiếp theo

#### Trạng thái cần có
- loading
- route success
- route error fallback nếu cần

### 2. Login
#### Bắt buộc
- email input
- password input
- login button
- navigate to register
- Google Sign-In button nếu project dùng
- loading state
- error message readable

#### Validation
- email format hợp lệ
- password không rỗng
- block duplicate submit

### 3. Register
#### Bắt buộc
- email
- password
- confirm password
- register button
- navigate to login
- loading state
- validation state

#### Validation
- email hợp lệ
- password đạt rule tối thiểu
- confirm password khớp

### 4. Complete Profile
#### Bắt buộc
- display name
- username hoặc handle nếu project dùng
- avatar upload hoặc avatar placeholder
- bio ngắn optional
- save profile

#### Quy tắc
- profile chưa đủ thì không cho vào main app hoàn chỉnh

## Profile screens

### Own Profile
#### Nội dung tối thiểu
- avatar
- display name
- username
- bio
- basic stats placeholder nếu cần
- entry to edit profile
- logout/settings entry

### Edit Profile
#### Chức năng
- load current values
- update display name
- update bio
- update avatar nếu được giao
- save and reflect lại lên profile

## Data fields tối thiểu nên thống nhất
- uid
- displayName
- username
- email
- avatarUrl
- bio
- createdAt
- updatedAt

## UI state bắt buộc
Mỗi màn hình quan trọng phải có:
- loading
- form content
- validation error
- submit loading
- submit success/failure

## Repository/use case gợi ý
### Domain
- LoginUseCase
- RegisterUseCase
- ObserveSessionUseCase
- GetCurrentUserProfileUseCase
- UpdateProfileUseCase
- LogoutUseCase

### Data
- AuthRepository
- ProfileRepository
- FirebaseAuthDataSource
- UserProfileRemoteDataSource

## Điều quan trọng
Không được để:
- composable gọi Firebase trực tiếp,
- state rời rạc khó test,
- màn hình submit nhiều lần gây duplicate request.
