# A Remaining Scope Spec - Auth/Profile + Foundation Completion

> Reference only. Canonical compact spec: `A_master_spec.md`.

## 1. Mục đích

Tài liệu này là source of truth cho **toàn bộ phần còn lại của Người A** sau khi team đã chia lại workload 4 tuần cuối.

Mục tiêu chính:
- A hoàn thiện nốt phần foundation/auth/profile.
- A không tiếp tục ôm module của B/C/D.
- A freeze các contract cần thiết để team tiếp tục làm mà ít phụ thuộc vào A.
- A bàn giao một nền app đủ ổn định cho demo và integration.

## 2. Hiện trạng đã xác nhận

### Firebase
- App đã kết nối Firebase project thật.
- Có `google-services.json`.
- Gradle đã bật Google Services plugin.
- Firebase BOM/dependencies đã khai báo.
- Firebase Auth backend dùng thật.
- Firestore đã gọi thật trong profile repository.
- Storage mới có provider baseline, chưa có avatar upload thật.

### Auth
- Register chạy thật.
- Session check chạy thật.
- Logout chạy thật.
- Login backend có thật nhưng UI chưa nối end-to-end.
- Login success không tự quyết Main/Complete Profile bằng UI; route sau đăng nhập phải đi qua resolver dựa trên profile completeness.

### Profile
- Complete Profile ghi Firestore thật.
- Own Profile đọc Firestore thật.
- Edit Profile update Firestore thật.
- Chưa có avatar upload.
- Username unique mới check query, chưa atomic.
- `username_claims` phải là source of truth cuối cùng cho uniqueness khi implement xong.

### Navigation
- `AppNavHost` là source of truth cấp app.
- `MainContainerScreen` là source of truth in-app navigation.
- Route constants khá centralized.
- Bottom nav ổn.
- Logout/back stack cơ bản đúng.

### Shared UI
Đã có foundation core:
- AppScaffold
- AppTopBar
- AppTextField
- PrimaryButton
- SecondaryButton
- AppCard
- LoadingBlock
- EmptyStateBlock
- ErrorStateBlock

## 3. Vai trò còn lại của A

A là owner của:
- Firebase/Auth/Profile foundation
- Auth/Profile user experience
- Current user/profile contract
- Top-level navigation stability
- Shared component consistency mức core
- Contract freeze

A không là owner của:
- Feed business logic
- Search/feed social flow
- Personal finance business logic
- Notification owner logic
- Split bill business logic
- Smart Split Engine
- Module-specific data layers của B/C/D

## 4. Mục tiêu cuối cùng của A

A hoàn thành khi:
- auth/profile flow chạy thật, không còn mock/bypass,
- username claim đủ chắc cho MVP mạnh,
- avatar upload hoạt động,
- profile flow đủ đẹp để demo,
- route/shared/current user contract ổn định,
- B/C/D có thể tiếp tục module của họ dựa trên contract đã freeze.

## 5. Scope chính thức của A

### In Scope
1. Login end-to-end thật.
2. Register regression check.
3. Splash/session routing regression check.
4. Logout/back stack regression check.
5. Complete Profile polish.
6. Own Profile polish.
7. Edit Profile polish.
8. Validation/loading/error state MVP mạnh.
9. Atomic username claim.
10. Release username cũ khi đổi username.
11. Avatar upload MVP.
12. Firebase/Auth/Profile repository cleanup nếu cần.
13. Current user/profile contract freeze.
14. Route/top-level contract freeze.
15. Shared UI core contract freeze.
16. Top-level stability fixes.

### Out of Scope
1. Feed business logic.
2. Create post upload logic.
3. Like/comment/search implementation.
4. Personal finance logic.
5. Transaction/category/chart logic.
6. Notification center implementation.
7. Spending reminder.
8. Group/bill/share persistence.
9. Smart Split Engine.
10. Full production-level security/rule hardening.
11. Crop/camera avatar as required feature.
12. Advanced profile stats/followers UI.

## 6. Must / Should / Defer

### Must-have
- Login end-to-end.
- Auth/profile UI wiring.
- Validation/loading/error state.
- Atomic username claim for Complete Profile + Edit Profile.
- Release old username when changed.
- Avatar upload.
- Own/Edit Profile completion.
- Contract freeze docs.

### Should-have
- Rename obvious typo files if low risk, such as `BottonTab.kt`.
- Improve error message consistency.
- Add clear comments for route/current user/profile contracts.
- Improve profile UI polish enough for demo.
- Add simple manual QA checklist.

### Can Defer
- Camera avatar source.
- Crop avatar.
- Image compression.
- Delete old avatar file from Storage.
- Username suggestion.
- Reserved username list.
- Advanced profile stats.
- Profile social aggregation.

## 7. Implementation Order

### Step 1 - Fix Login End-to-End
Do first because it is the clearest broken auth flow.

Tasks:
- Bind `LoginScreen` to `LoginViewModel`.
- Replace direct `onLoginSuccess()` click with `viewModel.submit()`.
- Navigate only after success effect.
- Show validation and Firebase error.

### Step 2 - Normalize Auth/Profile State Handling
Tasks:
- Review Login/Register/CompleteProfile/Profile/EditProfile states.
- Ensure loading/error/submit disabled behavior.
- Ensure no silent failures.

### Step 3 - Implement Atomic Username Claim
Tasks:
- Add or update repository method for atomic username claim.
- Use `username_claims/{usernameLower}` as claim source.
- Apply to Complete Profile.
- Apply to Edit Profile.
- Release old username claim when changed.

### Step 4 - Implement Avatar Upload MVP
Tasks:
- Add image picking entry in Complete/Edit Profile if desired.
- Upload selected image to Storage.
- Get URL/reference.
- Update profile.
- Display avatar in Own/Edit Profile.

### Step 5 - Polish Own/Edit Profile
Tasks:
- Prefill edit form.
- Save profile cleanly.
- Refresh own profile.
- Handle username conflict/avatar upload failure.
- Keep form data on failure.

### Step 6 - Freeze Contracts
Tasks:
- Write or update `A_contract_freeze.md`.
- Include routes/current user/profile/shared components.
- Communicate out-of-scope clearly.

### Step 7 - Final Regression
Tasks:
- Test auth/profile flows.
- Test logout/back stack.
- Test avatar upload.
- Test username change conflict.
- Test profile reload.
- Test app still navigates to all main tabs.

## 8. Detailed Requirements

### 8.1 Login End-to-End
Functional Requirements:
- Login must call Firebase Auth through existing use case/repository chain.
- Login must not navigate on button click directly.
- Login success must be based on Firebase success result.
- Login failure must be shown to user.

Acceptance Criteria:
- Correct email/password logs in.
- Wrong password shows error.
- Invalid email shows validation.
- Loading state appears during submit.
- Button disabled while submitting.
- On success, navigate to correct destination.

### 8.2 Register Regression
Functional Requirements:
- Register must continue using Firebase Auth.
- Register success should route to Complete Profile.
- Register error must remain visible.

Acceptance Criteria:
- Register new user succeeds.
- Existing email shows error.
- Password mismatch shows error.
- Register does not break after login wiring changes.

### 8.3 Session Check
Functional Requirements:
- Splash/session check must route to:
  - Auth
  - Complete Profile
  - Main
- Route decision should use current Firebase session and profile existence/completeness.

Acceptance Criteria:
- No session -> Auth.
- Session but incomplete profile -> Complete Profile.
- Session with complete profile -> Main.
- Logout then restart -> Auth.

### 8.4 Complete Profile
Functional Requirements:
- Validate display name and username.
- Use atomic username claim.
- Support avatar upload if user chooses avatar.
- Save profile to Firestore.
- Route to Main after success.

Acceptance Criteria:
- New user completes profile successfully.
- Taken username shows error.
- Avatar optional but if selected must upload and save.
- Profile appears in Own Profile after completion.

### 8.5 Edit Profile
Functional Requirements:
- Prefill current profile.
- Allow update display name, username, bio, avatar.
- Use atomic username claim when username changes.
- Release old username claim when username changes.
- Save Firestore profile.
- Reflect changes in Own Profile.

Acceptance Criteria:
- Edit display name/bio works.
- Edit username works if available.
- Edit username fails if taken.
- Old username claim released when username changed.
- Avatar update works.
- Error does not wipe form unnecessarily.

### 8.6 Avatar Upload
Functional Requirements:
- Avatar upload is must-have.
- User can choose image through any supported device image selection method.
- Source selection is not locked in this spec.
- Upload to Firebase Storage or agreed storage mechanism.
- Save URL/reference to profile.
- Display avatar after update.
- Preferred storage path should be stable and team-visible, for example `avatars/{uid}/profile_avatar`.
- `avatarUrl` is the source of truth for profile display; upload mechanism is an implementation detail.

Acceptance Criteria:
- User can select avatar.
- Avatar uploads.
- Profile document stores avatar reference.
- Own Profile/Edit Profile show avatar.
- Upload failure shows error.

Nice-to-have only:
- Camera.
- Crop.
- Compression.
- Preview enhancements.
- Old avatar cleanup.

### 8.7 Atomic Username Claim
Functional Requirements:
- Use `username_claims`.
- Claim username in transaction or equivalent atomic mechanism.
- Apply to Complete Profile and Edit Profile.
- Release old username on username change.
- Prevent duplicate usernames.

Acceptance Criteria:
- Conflict cannot pass.
- Same user can keep current username.
- User can change to available username.
- Old username becomes available after release.
- Repository no longer relies only on query check as final source of truth.

## 9. Final Definition of Done for A

A is done when:
- Login/Register/Splash/Logout are real and stable.
- Complete Profile/Own Profile/Edit Profile are real and stable.
- Validation/loading/error states are MVP strong.
- Username claim is atomic.
- Avatar upload works.
- Route/top-level contracts are frozen.
- Shared UI core contract is frozen.
- A has documented what B/C/D can rely on.
- A has documented what A will not own further.

## 10. Manual QA Script

### Auth QA
1. Start app logged out.
2. Confirm Splash routes to Auth.
3. Try invalid login.
4. Try valid login.
5. Logout.
6. Confirm back does not return to Main.
7. Register new account.
8. Complete profile.
9. Restart app and confirm session persists.

### Profile QA
1. Open Own Profile.
2. Confirm data loads from Firestore.
3. Open Edit Profile.
4. Change display name.
5. Change bio.
6. Change username to available username.
7. Try username already taken.
8. Upload avatar.
9. Save.
10. Confirm Own Profile reflects updates.

### Regression QA
1. Open Feed tab.
2. Open Split tab.
3. Open Personal tab.
4. Open Profile tab.
5. Confirm bottom nav still works.
6. Confirm app does not crash after profile update/logout/login.

## 11. Handoff to B/C/D

After A completes this spec, B/C/D can rely on:
- Firebase initialized.
- Auth session available.
- Current user uid available.
- Profile can be read from Firestore.
- Route constants stable enough.
- Bottom nav stable.
- Shared UI core components available.
- AvatarUrl exists as profile field.
- Username uniqueness handled by profile system.

B/C/D should not expect A to implement their module business logic.

## 12. Prompt for Code Agent

```text
You are coding only the remaining scope of Person A in DineSplit Social.

Implement:
- Login end-to-end with Firebase through LoginViewModel
- Auth/profile UI wiring fixes
- MVP-strong validation/loading/error states
- Atomic username claim for Complete Profile and Edit Profile
- Release old username claim when username changes
- Avatar upload MVP
- Own/Edit Profile polish
- Contract freeze documentation

Do not implement:
- Feed business logic
- Personal finance business logic
- Notification module logic
- Split bill logic
- Smart Split Engine

Before coding, list:
1. Files to create/update
2. Existing flows you will preserve
3. Assumptions
4. Out-of-scope items

Then implement step by step.
```
