# A Week 6 Spec - Auth/Profile Completion + Foundation Freeze

> Reference only. Canonical compact spec: `A_master_spec.md`.

## 1. Mục đích của tài liệu

Tài liệu này là spec chi tiết cho **Người A trong Week 6** sau khi team đã thống nhất lại phạm vi.

Week 6 của A **không còn là tuần làm Notification owner**. Notification đã được chuyển sang Người C.

Week 6 của A tập trung vào:
- hoàn thiện auth/profile flow ở mức MVP mạnh,
- xử lý các điểm còn thiếu trong foundation,
- chốt contract để B/C/D có thể tiếp tục làm module mà ít phụ thuộc vào A,
- giữ top-level navigation/app shell ổn định.

## 2. Bối cảnh hiện tại

### Đã có
- App đã kết nối Firebase project thật.
- Đã có `google-services.json`.
- Firebase plugin/dependencies đã khai báo.
- Firestore đã được gọi thật trong profile repository.
- Storage đã có provider baseline nhưng chưa có avatar upload thật.
- Navigation baseline đã tương đối ổn:
  - `AppNavHost` là app-level source of truth.
  - `MainContainerScreen` là in-app navigation source of truth.
  - Bottom navigation gồm Feed / Split / Personal / Profile.
  - Logout/back stack cơ bản đúng.
- Shared UI foundation đã có:
  - `AppScaffold`
  - `AppTopBar`
  - `AppTextField`
  - `PrimaryButton`
  - `SecondaryButton`
  - `AppCard`
  - `LoadingBlock`
  - `EmptyStateBlock`
  - `ErrorStateBlock`

### Còn thiếu
- `LoginScreen` chưa nối thật vào `LoginViewModel`.
- Login UI hiện còn bypass login logic và gọi thẳng `onLoginSuccess()`.
- Validation/loading/error state cần polish mức MVP mạnh.
- Username unique mới check trùng bằng query, chưa atomic.
- Chưa có avatar upload thật.
- Own Profile / Edit Profile đã có lõi thật nhưng cần polish thêm.
- Cần freeze contract để B/C/D dùng tiếp.

## 3. Vai trò của A trong Week 6

A là owner của:
- Auth/Profile completion
- Firebase/Auth/Profile foundation
- Top-level navigation stability
- Shared UI consistency mức core
- Contract freeze cho current user/profile/routes/components

A **không** là owner của:
- Feed business logic
- Create post / like / comment / search logic
- Personal finance logic
- Notification module logic
- Split bill logic
- Smart Split Engine
- Module-specific repositories của B/C/D

## 4. Mục tiêu Week 6

Cuối Week 6, A phải bàn giao được một nền tảng đủ sạch để:
- login/register/profile chạy thật,
- profile có avatar upload thật,
- username claim đủ chặt ở mức MVP mạnh,
- B/C/D có thể dùng current user/profile/routes/shared components mà không phải chờ A sửa nền nữa,
- top-level app không còn là nút thắt.

## 5. Workstream 1 - Fix Login End-to-End

### Hiện trạng
- `LoginViewModel` đã có validation và gọi `LoginUseCase`.
- `LoginUseCase` gọi `AuthRepository.login()`.
- `FirebaseAuthRepository.login()` dùng Firebase Auth thật.
- Nhưng `LoginScreen` chưa bind vào `LoginViewModel`.
- Nút Login đang gọi thẳng `onLoginSuccess()`.

### Việc phải làm
- Bind `LoginScreen` vào `LoginViewModel`.
- Form input phải update state trong ViewModel.
- Nút Login phải gọi `LoginViewModel.submit()`.
- Chỉ xác nhận auth/session thành công sau khi login Firebase thành công thật; route đích cuối cùng do resolver của A quyết định.
- Nếu login lỗi, hiển thị error message từ state/effect.
- Không để UI tự quyết định login success nếu chưa có Firebase result.
- Login success chỉ xác nhận auth/session thành công; route đích cuối cùng phải do resolver của A quyết định dựa trên profile completeness.

### Acceptance Criteria
- Email/password hợp lệ và đúng tài khoản -> login thành công -> route sang đúng destination do resolver quyết định.
- Email sai format -> báo lỗi validation.
- Password rỗng/ngắn -> báo lỗi validation.
- Sai credential -> báo lỗi rõ ràng.
- Khi đang submit, button disabled hoặc loading.
- Không còn `onLoginSuccess()` được gọi trực tiếp từ click button nếu chưa login thành công thật.

### Output mong đợi
- `LoginScreen` data-driven bởi `LoginUiState`.
- `LoginViewModel` là source of truth cho login form.
- Login flow đồng bộ với Register, Splash, Logout.

## 6. Workstream 2 - Auth/Profile UI Wiring Completion

### Màn hình thuộc scope A
- Splash
- Login
- Register
- Complete Profile
- Own Profile
- Edit Profile

### Việc phải làm
- Rà lại từng màn đã nối ViewModel/state/effect đúng chưa.
- Không để màn nào trong scope A còn bypass business logic.
- Rà navigation event:
  - Splash -> Auth / Complete Profile / Main
  - Register success -> Complete Profile
  - Login success -> Main hoặc Complete Profile nếu profile chưa đủ
  - Edit Profile save success -> Own Profile
  - Logout -> Auth/Login và clear back stack hợp lý

### Acceptance Criteria
- Tất cả màn thuộc A đều dùng ViewModel/state hợp lý.
- Không còn logic Firebase trực tiếp trong Composable.
- Không còn navigation giả khi action chưa thành công.
- Back stack không gây quay lại Main sau logout.
- Flow login/register/complete profile/profile/edit profile demo được liên tục.

## 7. Workstream 3 - Validation / Loading / Error State MVP Strong

### Mục tiêu
Đưa auth/profile từ mức “có nền” lên mức **MVP mạnh**, đủ demo và ít lỗi.

### Áp dụng cho
- Login
- Register
- Complete Profile
- Own Profile
- Edit Profile
- Logout state nếu có

### Yêu cầu validation
- Email format hợp lệ.
- Password không rỗng và đạt rule tối thiểu.
- Confirm password khớp ở Register.
- Display name không rỗng.
- Username hợp lệ:
  - không rỗng,
  - normalize được,
  - không chứa ký tự không mong muốn nếu project đã có rule.
- Bio optional nhưng không vượt quá giới hạn nếu có.

### Yêu cầu loading
- Submit auth/profile có loading.
- Load own profile có loading.
- Avatar upload có loading riêng hoặc thể hiện rõ đang upload.
- Logout có loading hoặc disabled state.

### Yêu cầu error
- Firebase/Auth/Profile error map thành message dễ hiểu.
- Không silent failure.
- Có retry ở Own Profile nếu load fail.
- Form error hiển thị gần field liên quan khi hợp lý.
- Error chung có thể hiển thị bằng snackbar/error block.

### Acceptance Criteria
- Login/Register/Complete Profile/Edit Profile không submit khi invalid.
- Button không cho spam click khi đang submit.
- Error hiển thị rõ.
- Loading không làm mất dữ liệu form không cần thiết.
- Retry profile load hoạt động.

## 8. Workstream 4 - Atomic Username Claim

### Hiện trạng
- Hiện đang có check trùng username bằng query `usernameLower` trong `user_profiles`.
- Cách này chưa atomic và có race condition.
- Firestore rules/schema đã có khái niệm `username_claims`, nhưng repository chưa dùng đúng.
- `username_claims` là source of truth cuối cùng cho uniqueness; `user_profiles` chỉ là profile data, không phải authority cuối cùng cho username.

### Quyết định đã chốt
- Username phải unique.
- Cho phép đổi username trong Edit Profile.
- Atomic claim áp dụng cho cả:
  - Complete Profile
  - Edit Profile
- Nếu đổi username, phải giải phóng username cũ.
- Đây là must-have cho Week 6 của A.

### Mô hình đề xuất
Collection:

```text
username_claims/{usernameLower}
```

Document fields gợi ý:

```text
uid
username
usernameLower
claimedAt
updatedAt
```

Profile collection:

```text
user_profiles/{uid}
```

Fields liên quan:

```text
uid
displayName
username
usernameLower
avatarUrl
bio
email
createdAt
updatedAt
```

### Flow Complete Profile
1. User nhập username.
2. Normalize thành `usernameLower`.
3. Mở Firestore transaction.
4. Kiểm tra `username_claims/{usernameLower}`.
5. Nếu đã tồn tại và `uid` khác current user -> báo username unavailable.
6. Nếu chưa tồn tại -> create claim.
7. Upsert `user_profiles/{uid}` với username mới.
8. Commit transaction.

### Flow Edit Profile đổi username
1. Load profile cũ để biết `oldUsernameLower`.
2. User nhập username mới.
3. Normalize thành `newUsernameLower`.
4. Nếu username không đổi:
   - chỉ update profile fields khác.
5. Nếu username đổi:
   - mở Firestore transaction.
   - kiểm tra `username_claims/{newUsernameLower}`.
   - nếu claim mới tồn tại bởi uid khác -> báo lỗi.
   - tạo hoặc update claim mới cho current uid.
   - xóa hoặc release claim cũ `username_claims/{oldUsernameLower}` nếu claim cũ thuộc current uid.
   - update `user_profiles/{uid}`.
6. Commit transaction.

### Quy tắc release username cũ
- Chỉ xóa username claim cũ nếu document cũ có `uid == currentUser.uid`.
- Không xóa claim của user khác.
- Nếu username cũ không tồn tại claim vì legacy data, không fail toàn bộ flow; log/handle an toàn.

### Acceptance Criteria
- Hai user không thể claim cùng username.
- Complete Profile dùng atomic claim.
- Edit Profile đổi username dùng atomic claim.
- Đổi username giải phóng username cũ đúng owner.
- Username conflict hiển thị message rõ.
- Không còn chỉ dựa vào query `user_profiles` làm source of truth cuối cùng.

### Out of Scope
- Username history.
- Reserved usernames phức tạp.
- Admin moderation username.
- Username suggestion system.

## 9. Workstream 5 - Avatar Upload MVP

### Hiện trạng
- `avatarUrl` đã có trong profile.
- Firebase Storage provider đã có.
- Chưa có flow upload avatar thật.

### Quyết định đã chốt
- Avatar upload là must-have.
- Implementation tối thiểu phải hỗ trợ:
  - chọn ảnh,
  - upload ảnh,
  - lưu/update avatar vào profile.
- Source chọn ảnh không khóa cứng ở bản spec đầu tiên.
- Crop/camera không bắt buộc.
- Nếu làm thêm crop/camera thì là nice-to-have, không phải acceptance criteria.
- Storage path nên thống nhất một kiểu rõ ràng cho team, ví dụ `avatars/{uid}/profile_avatar`.
- `avatarUrl` là field hiển thị cuối cùng trong profile; cách upload/source chỉ là implementation detail.

### Flow tối thiểu
1. User chọn ảnh đại diện từ cơ chế chọn ảnh được implementation hỗ trợ.
2. App nhận URI/local reference.
3. Upload ảnh lên Firebase Storage hoặc storage mechanism đã chốt.
4. Lấy download URL hoặc avatar reference.
5. Update `avatarUrl` trong `user_profiles/{uid}`.
6. Own Profile / Edit Profile phản ánh avatar mới.
7. Nếu upload lỗi, hiển thị error và không làm hỏng profile data cũ.

### Storage path gợi ý
```text
avatars/{uid}/profile_avatar
```

Hoặc nếu muốn versioning:
```text
avatars/{uid}/{timestamp}
```

### Acceptance Criteria
- User có thể chọn ảnh avatar.
- App upload ảnh thành công.
- URL/reference được lưu vào Firestore profile.
- Own Profile hiển thị avatar mới.
- Edit Profile hiển thị avatar mới.
- Upload fail có error state.
- Không bắt buộc crop/camera.

### Nice-to-have
- crop avatar.
- camera capture.
- image compression.
- preview nâng cao.
- delete old avatar file.

## 10. Workstream 6 - Own/Edit Profile Polish

### Việc phải làm
- Own Profile load data thật ổn định.
- Edit Profile prefill data thật.
- Save profile update Firestore thật.
- Save success refresh lại Own Profile đúng.
- Logout state rõ ràng.
- Avatar update reflected immediately hoặc sau reload có kiểm soát.
- Username conflict không làm mất form data.

### Acceptance Criteria
- Mở Own Profile thấy đúng display name/username/bio/avatar.
- Vào Edit Profile thấy data hiện tại.
- Sửa display name/bio/avatar/username và save được.
- Nếu username mới bị taken, chỉ field username báo lỗi, không mất các field khác.
- Save success quay lại hoặc cập nhật profile rõ ràng.
- Logout vẫn hoạt động đúng.

## 11. Workstream 7 - Top-Level Stability and Shared Foundation

### Việc phải làm
- Rà route constants.
- Sửa typo nếu có, ví dụ `BottonTab.kt` nếu project muốn clean naming.
- Rà `AppNavHost`.
- Rà `MainContainerScreen`.
- Rà bottom nav state.
- Rà shared component usage trong auth/profile.
- Không refactor lớn nếu không cần.

### Acceptance Criteria
- Auth graph/Main graph hoạt động đúng.
- 4 bottom tabs vẫn ổn.
- Edit Profile/Profile route không conflict.
- Notification route entry vẫn tồn tại cho C dùng, nhưng A không làm notification logic.
- Shared components core đủ ổn định.

## 12. Deliverables cuối Week 6

A phải bàn giao:
1. Login end-to-end thật.
2. Auth/Profile UI wiring hoàn chỉnh.
3. Validation/loading/error state MVP mạnh.
4. Atomic username claim cho Complete Profile + Edit Profile.
5. Release username cũ khi đổi username.
6. Avatar upload MVP.
7. Own/Edit Profile polished.
8. Top-level route/app shell ổn định.
9. Contract freeze file hoặc section cho team.
10. Danh sách việc A không còn owner.

## 13. Out of Scope Week 6 của A

A không làm:
- Feed create post logic.
- Feed like/comment/search logic.
- Personal finance transaction/category/chart logic.
- Notification center implementation.
- Spending reminder.
- Group/bill/share persistence.
- Smart Split Engine.
- Business repositories của B/C/D.
- Crop/camera avatar bắt buộc.
- Production-level username moderation.

## 14. Review Checklist

### Auth
- [ ] Login dùng Firebase thật.
- [ ] Register vẫn chạy thật.
- [ ] Splash route đúng.
- [ ] Logout clear session và back stack đúng.

### Profile
- [ ] Complete Profile ghi Firestore thật.
- [ ] Own Profile đọc Firestore thật.
- [ ] Edit Profile update Firestore thật.
- [ ] Avatar upload thật.
- [ ] Username claim atomic.

### UI State
- [ ] Loading state có.
- [ ] Error state có.
- [ ] Disable submit khi loading.
- [ ] Retry profile load nếu fail.
- [ ] Error message dễ hiểu.

### Integration
- [ ] Route constants ổn.
- [ ] App shell không vỡ.
- [ ] Bottom nav không vỡ.
- [ ] B/C/D có thể dùng contract đã freeze.

## 15. Prompt mẫu cho code agent

```text
Bạn là coding agent cho Người A trong dự án DineSplit Social.

Chỉ làm Week 6 revised scope:
- Login end-to-end thật
- auth/profile UI wiring
- validation/loading/error state MVP mạnh
- atomic username claim cho Complete Profile + Edit Profile
- giải phóng username cũ khi đổi username
- avatar upload MVP
- Own/Edit Profile polish
- top-level route/shared foundation stability
- contract freeze cho B/C/D

Không làm:
- Feed business logic
- Personal finance logic
- Notification owner logic
- Split bill logic
- Smart Split Engine
- module-specific repositories của B/C/D

Trước khi code, hãy trả lời:
1. Các file bạn sẽ sửa/tạo
2. Flow nào bạn sẽ hoàn thiện trước
3. Assumptions bạn đang dùng
4. Những gì bạn sẽ không làm vì ngoài scope

Sau đó mới code.
```
