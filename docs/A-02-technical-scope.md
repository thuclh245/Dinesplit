# A-02 - Technical Scope for Người A

## Công nghệ khóa cứng
Người A phải bám đúng các quyết định sau:
- Platform: Android
- Language: Kotlin
- UI toolkit: Jetpack Compose
- Architecture: MVVM + lightweight Clean Architecture
- Backend: Firebase Auth + Firestore + Storage + FCM
- Main tabs: Feed / Split / Personal / Profile

## Package structure định hướng
Người A nên dựng khung theo kiểu:

```text
app/
  presentation/
    auth/
    profile/
    main/
    navigation/
    common/
  domain/
    model/
    repository/
    usecase/
    validation/
  data/
    remote/
    repository/
    mapper/
    model/
  core/
    ui/
    util/
    navigation/
```

## Phạm vi màn hình A chịu trách nhiệm
### Bắt buộc
- Splash
- Login
- Register
- Complete Profile
- Own Profile
- Edit Profile
- Main App Container with Bottom Navigation

### Có thể cần thêm
- Settings base
- Session/logout entry
- Simple follower/following count display placeholder

## Phạm vi logic A chịu trách nhiệm
### Auth
- login email/password
- register email/password
- Google Sign-In nếu project đã chốt tích hợp
- logout
- session restore

### Profile
- load current profile
- edit basic fields
- upload/change avatar nếu team muốn giao luôn cho A
- save profile data

### Navigation
- route sau splash
- route sau login/register
- protected route logic
- bottom nav switching

## Những gì A chỉ cần dựng khung, không cần làm sâu
- Feed tab content
- Split tab content
- Personal tab content
- Notification center
- Assistant entry

A chỉ cần đảm bảo:
- route tồn tại,
- tab đi được,
- scaffold ổn,
- placeholder không crash.

## Output tối thiểu
Người A hoàn thành tốt khi repo có:
- app mở lên chạy được,
- auth flow đi từ Splash -> Login/Register -> Main,
- profile flow cập nhật được,
- 4 bottom tabs hoạt động,
- logout quay về auth đúng,
- code đủ sạch để team khác tiếp tục.
