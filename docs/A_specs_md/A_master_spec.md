# DineSplit - Person A Master Spec

> Canonical spec for Person A. Use this file when coding. The other A spec files are reference material only.

## 1. Goal
Person A owns the app shell, auth/session, profile foundation, shared core UI, and the stable Firebase baseline needed by the rest of the team.

Deliver a clean MVP foundation so B/C/D can keep building without depending on A for top-level flows.

## 2. Confirmed current state
- Firebase project is connected.
- `google-services.json` exists.
- Google Services plugin and Firebase BOM/dependencies are in place.
- Firebase Auth is real.
- Firestore is real for profile data.
- Firebase Storage provider exists, but avatar upload flow is not finished.
- `AppNavHost` is the app-level navigation source of truth.
- `MainContainerScreen` is the in-app navigation source of truth.
- Bottom tabs are Feed / Split / Personal / Profile.
- Shared UI core already exists and is reusable.
- Register, Splash/session check, Logout, Own Profile, Edit Profile have real backend parts.
- Login UI is still the main gap: the screen is not fully wired to `LoginViewModel` yet.

## 3. Person A scope
### In scope
1. Login end-to-end wiring.
2. Register regression check.
3. Splash/session routing.
4. Logout and back stack behavior.
5. Complete Profile polish.
6. Own Profile polish.
7. Edit Profile polish.
8. MVP-strong validation, loading, and error states.
9. Atomic username claim.
10. Release old username on change.
11. Avatar upload MVP.
12. Stable top-level navigation and shell.
13. Shared UI core freeze.
14. Current user/profile contract freeze.
15. Firebase/Auth/Profile cleanup only if needed for A scope.

### Out of scope
- Feed business logic.
- Like/comment/search/create-post logic.
- Personal finance logic.
- Notification business logic.
- Split/bill/share logic.
- Smart Split Engine.
- Module-specific repositories/use cases for B/C/D.
- Mandatory camera/crop avatar.
- Advanced profile stats/followers.
- Production-level moderation features.

## 4. Non-negotiable contracts
### Firebase baseline
A keeps these stable:
- `FirebaseAuth`
- `FirebaseFirestore`
- `FirebaseStorage`
- `FirebaseMessaging`

B/C/D can rely on Firebase being initialized, but A does not own their feature repositories.

### Auth/session contract
- Logged out user -> Auth/Login.
- Logged in user with incomplete profile -> Complete Profile.
- Logged in user with complete profile -> Main.
- Logout clears Firebase session and removes back stack to prevent returning to Main.
- Login success must **not** hardcode the next route in UI.
- Route decision after login is owned by A's resolver flow (Splash / start destination / profile-completeness resolver).

### User/profile contract
Recommended document:
- `user_profiles/{uid}`

Stable fields:
- `uid`
- `email`
- `displayName`
- `username`
- `usernameLower`
- `avatarUrl`
- `bio`
- `createdAt`
- `updatedAt`

Username uniqueness:
- `username_claims/{usernameLower}` is the source of truth.
- `user_profiles` is profile data only.
- Complete Profile and Edit Profile must use atomic claim logic.
- Old username claim must be released when username changes.
- Conflict must show a readable error.

Avatar contract:
- `avatarUrl` is the source of truth for display.
- Avatar upload is owned by A.
- Recommended stable storage path: `avatars/{uid}/profile_avatar`.
- Camera/crop/compression are optional, not required.

### Navigation contract
A owns:
- `AppNavHost`
- `MainContainerScreen`
- route constants (`AppRoute`, `BottomTab`)
- top-level auth/main back stack behavior

Main tabs:
- Feed
- Split
- Personal
- Profile

Dynamic routes should stay centralized through helper methods like `createRoute(...)`.

### Shared UI contract
Reusable core components maintained by A:
- `AppScaffold`
- `AppTopBar`
- `AppTextField`
- `PrimaryButton`
- `SecondaryButton`
- `AppCard`
- `LoadingBlock`
- `EmptyStateBlock`
- `ErrorStateBlock`

Rules:
- reuse the shared components when possible;
- keep styles consistent;
- do not invent different button/input patterns inside A-owned screens.

### Error/loading contract
All A-owned screens should expose the basics:
- loading state
- content/form state
- error state
- disabled submit while loading
- retry where it makes sense

## 5. Current gaps to finish
1. Wire `LoginScreen` to `LoginViewModel`.
2. Make login submit actually call Firebase login through the use case/repository chain.
3. Keep route decision after login in the resolver, not in the button click.
4. Replace username query-check final authority with atomic `username_claims` logic.
5. Add avatar upload MVP and store the resulting `avatarUrl` in Firestore.
6. Polish Complete Profile / Own Profile / Edit Profile so errors do not wipe form state.
7. Keep logout/back stack stable.
8. Keep shared UI and top-level routes frozen.

## 6. Implementation order
1. Fix login end-to-end.
2. Normalize auth/profile state handling.
3. Implement atomic username claim.
4. Implement avatar upload MVP.
5. Polish Own/Edit Profile behavior.
6. Freeze contracts.
7. Run regression QA.

## 7. Acceptance criteria
### Auth
- Login uses Firebase for real.
- Register continues to work.
- Splash routes correctly.
- Logout clears session and back stack.

### Profile
- Complete Profile writes Firestore.
- Own Profile reads Firestore.
- Edit Profile updates Firestore.
- Avatar upload works.
- Username claim is atomic.

### UI state
- Validation works.
- Loading state is visible.
- Errors are readable.
- Submit buttons are disabled while submitting.
- Profile retry works on load failure.

### Integration
- Route constants remain stable.
- App shell does not break.
- Bottom nav stays stable.
- B/C/D can rely on the freeze contract.

## 8. Prompt for code agent
```text
You are coding only Person A in DineSplit Social.

Implement only the remaining A scope:
- login end-to-end
- auth/profile UI wiring
- strong validation/loading/error states
- atomic username claim
- release old username on rename
- avatar upload MVP
- Own/Edit Profile polish
- top-level navigation and shared UI stability
- contract freeze for the rest of the team

Do not implement Feed, Personal, Notification, or Split business logic.

Before coding, list:
1. files to create/update
2. flows you will preserve
3. assumptions
4. out-of-scope items
```

