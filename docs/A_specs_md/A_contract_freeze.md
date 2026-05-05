# A Contract Freeze - Stable Foundation for B/C/D

> Reference only. Canonical compact spec: `A_master_spec.md`.

## 1. Mục đích

File này là contract freeze do Người A bàn giao để Người B, C, D có thể tiếp tục module riêng mà không phải đoán lại foundation.

Contract này không mô tả toàn bộ business logic của app.

Nó chỉ mô tả những phần nền mà A chịu trách nhiệm giữ ổn định:
- Auth/session contract
- Current user/profile contract
- Route/navigation contract
- Shared UI core contract
- Firebase baseline contract
- Out-of-scope boundary của A

## 2. Firebase Baseline Contract

### Stable Assumptions
- App đã kết nối Firebase project thật.
- `google-services.json` đã có trong app.
- Google Services plugin đã bật.
- Firebase BOM/dependencies đã khai báo.
- Firebase Auth available.
- Firestore available.
- Storage available.
- FCM dependency/provider baseline có thể tồn tại nhưng notification owner là C.

### Services
A đảm bảo baseline cho:
```text
FirebaseAuth
FirebaseFirestore
FirebaseStorage
FirebaseMessaging
```

### What B/C/D can rely on
- Firebase initialized enough for module repositories to use.
- Auth current user can be accessed.
- Firestore can be used for module collections.
- Storage can be used for media upload.
- A does not implement module-specific Firestore logic for B/C/D.

### What A does not own
- Feed post/comment/like repository.
- Personal transaction/category repository.
- Notification repository implementation beyond baseline.
- Split group/bill/share repository.

## 3. Auth/Session Contract

### Stable User Session
A owns:
- Login
- Register
- Session check
- Logout
- Auth routing

### Expected Auth Behavior
- Logged out user routes to Auth/Login.
- Registered user routes to Complete Profile if profile incomplete.
- Logged in user with completed profile routes to Main.
- Logout clears Firebase session and navigates back to Auth without allowing back navigation to Main.
- Login success UI must not hardcode the next route by itself.
- The route decision after login is owned by A's resolver flow (Splash / start-destination resolver / profile-completeness resolver).
- If the profile is incomplete, the resolver sends the user to Complete Profile; otherwise it sends the user to Main.

### Current User Access
B/C/D can assume:
- authenticated user has `uid`.
- current user profile can be loaded from profile repository/use case.
- FirebaseAuth current user is the base identity source.

### B/C/D should not
- implement their own auth session logic.
- bypass A's auth/session flow.
- directly manipulate top-level auth routing.

## 4. User/Profile Contract

### User Profile Collection
Recommended profile document:
```text
user_profiles/{uid}
```

### UserProfile fields
Minimum stable fields:
```text
uid: String
email: String
displayName: String
username: String
usernameLower: String
avatarUrl: String?
bio: String
createdAt: Timestamp
updatedAt: Timestamp
```

### Username Claims Collection
Stable claim collection:
```text
username_claims/{usernameLower}
```

Suggested fields:
```text
uid: String
username: String
usernameLower: String
claimedAt: Timestamp
updatedAt: Timestamp
```

### Username Rules
- Username is unique.
- Complete Profile must claim username atomically.
- Edit Profile username change must claim new username atomically.
- Old username claim must be released when username changes.
- Conflict must return readable error.
- `username_claims` is the source of truth for uniqueness.
- `user_profiles` is profile data only and must not be treated as the final uniqueness authority.

### Avatar Rules
- `avatarUrl` is the source of truth for profile avatar display.
- Avatar upload is owned by A for profile.
- Post image upload is owned by B, not A.
- Other module media upload should not depend on profile avatar upload implementation except shared Storage baseline.
- Preferred profile avatar storage path should be stable and team-visible, e.g. `avatars/{uid}/profile_avatar`.
- Camera/crop/compression are implementation choices, not contract requirements.

## 5. Route / Navigation Contract

### App-Level Source of Truth
A owns:
```text
AppNavHost
```

### In-App Source of Truth
A owns:
```text
MainContainerScreen
```

### Main Tabs
Main bottom navigation contains:
```text
Feed
Split
Personal
Profile
```

### Route Constants
Route constants should remain centralized, typically in:
```text
AppRoute
BottomTab
```

### Stable Top-Level Routes
Expected route concepts:
```text
Splash
Auth/Login
Auth/Register
CompleteProfile
Main
Feed
Split
Personal
Profile
EditProfile
Notifications
Assistant
```

### Dynamic Routes
Dynamic routes should use helper functions, e.g.:
```text
PostDetail.createRoute(postId)
OtherUserProfile.createRoute(userId)
AddTransaction.createRoute(...)
```

### A Owns
- top-level auth/main navigation.
- bottom nav shell.
- route constants.
- back stack behavior for auth/logout/profile-level flows.

### B Owns
- feed internal navigation details.
- post detail behavior.
- create post flow.
- search result behavior.
- other user profile social side behavior.

### C Owns
- personal internal navigation.
- notification screen logic.
- notification list/read/unread.
- notification deep link interpretation with A's route shell.

### D Owns
- split internal navigation.
- group detail/bill detail/settle summary behavior.

## 6. Shared UI Core Contract

### Core Components Available
A maintains core components:
```text
AppScaffold
AppTopBar
AppTextField
PrimaryButton
SecondaryButton
AppCard
LoadingBlock
EmptyStateBlock
ErrorStateBlock
```

### Component Usage Rules
B/C/D should:
- reuse existing core components when possible.
- avoid inventing inconsistent button/input/card styles.
- follow spacing, app bar, loading/error patterns.
- avoid hardcoding one-off UI patterns unless feature-specific.

### A Owns
- core shared components.
- auth/profile usage of these components.
- light consistency review.

### A Does Not Own
- polishing every screen in B/C/D modules.
- feature-specific cards if those are module-specific.
- fixing all visual issues caused by other owners.

## 7. Error / Loading Contract

### Shared Rules
All major screens should expose:
- loading state
- success/content state
- empty state where applicable
- error state where applicable

### For A-owned screens
A guarantees:
- Login/Register/CompleteProfile/EditProfile submit states.
- Own Profile load state.
- Auth/profile error messages.
- Basic retry behavior where relevant.

### For B/C/D-owned screens
B/C/D must implement their own:
- feed loading/error/empty
- personal loading/error/empty
- split loading/error/empty
- notification loading/error/empty

They may reuse A's shared components.

## 8. Module Dependency Boundaries

### B can rely on A for
- current user uid.
- profile display fields if needed.
- app route shell.
- shared UI components.
- Firebase baseline.

### B must own
- posts.
- comments.
- likes.
- feed search.
- create post upload flow.
- social profile behavior beyond base user/profile.

### C can rely on A for
- current user uid.
- profile fields.
- notification route entry shell.
- shared UI components.
- Firebase baseline.

### C must own
- transactions.
- categories.
- monthly summary.
- charts.
- notification center.
- unread/read notification state.
- spending reminder.

### D can rely on A for
- current user uid.
- route shell for Split tab.
- profile fields for members if needed.
- shared UI components.
- Firebase baseline.

### D must own
- groups.
- bills.
- shares.
- settlements.
- payment states.
- Smart Split Engine.

## 9. Contract Freeze Deliverables

A must deliver:
1. Auth/profile flows working.
2. Firebase baseline stable.
3. `UserProfile` contract documented.
4. `username_claims` contract documented.
5. Route map documented.
6. Shared UI component list documented.
7. Out-of-scope boundaries documented.
8. Manual QA checklist documented.

## 10. What Should Not Change Without Team Agreement

The following should not be changed casually after freeze:
- route names for main tabs.
- auth/main graph separation.
- `UserProfile` required fields.
- username claim collection name.
- bottom nav tab list.
- shared component names.
- logout/session behavior.
- Firebase project config.

## 11. Integration Notes

When B/C/D integrate their modules:
- They should not call Firebase Auth separately to create their own session flow.
- They should use the current user/session contract from A.
- They should avoid redefining `UserProfile`.
- They should not change top-level routes without telling A/team.
- They should use shared UI components where suitable.
- They should own their feature repositories and use cases.

## 12. Final Handoff Statement

After A completes this contract freeze:

A remains available for:
- top-level route bugs,
- auth/profile regressions,
- shared component consistency questions.

A is not responsible for:
- implementing Feed, Personal, Notification, Split, or Smart Split business logic.
- fixing every merge conflict created by module owners.
- redesigning module-specific data models.

## 13. Quick Checklist for B/C/D

Before asking A to change foundation, B/C/D should check:
- Is this a top-level route issue or my module route issue?
- Is this a shared component issue or my screen implementation issue?
- Is this auth/session/profile foundation or my repository logic?
- Am I trying to push feature-specific responsibility back to A?

If it is feature-specific, the module owner should handle it.
