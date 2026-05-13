# Implementation Plan - Database Models Implementation

This plan outlines the steps to implement the Firestore database schema into Kotlin Data Classes within the Android project. This includes updating existing models and creating new ones to support the "Split", "Social", and "Personal" features of DineSplit.

## User Review Required

- **Timestamp Handling**: I will use `java.util.Date` for fields marked as `timestamp` in the schema. This maps naturally to Firestore and is easy to use in Kotlin. Does this align with your preference, or would you prefer `Long` (milliseconds)?
- **Field Naming**: I will use `camelCase` for Kotlin property names, but I can use `@get:PropertyName` or similar if the Firestore keys must stay exactly as defined (e.g., `author_uid` vs `authorUid`). I'll assume `camelCase` is preferred for both unless specified otherwise.

## Proposed Changes

### [Domain Models] (E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model)

Updating and creating models to match the `database-schema.md`.

#### [UserProfile.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/UserProfile.kt)
- Add `diningStyles`, `followersCount`, `followingCount`, `postsCount`, `fcmToken`.
- Change `createdAt` and `updatedAt` to `Date`.

#### [Group.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/Group.kt)
- Align with schema: `id`, `name`, `avatarUrl`, `category`, `createdBy`, `admins`, `members`, `memberCount`, `totalSpent`, `isSettled`, `qrPaymentEnabled`, `qrBankAccount`, `qrBankName`, `qrAccountHolder`, `createdAt`, `updatedAt`.

#### [Post.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/Post.kt)
- Align with schema: `id`, `authorUid`, `authorName`, `authorAvatar`, `caption`, `imageUrls`, `location`, `linkedGroupId`, `linkedBillId`, `likesCount`, `commentsCount`, `visibility`, `tags`, `createdAt`, `updatedAt`.

#### [NEW] [Bill.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/Bill.kt)
- Model for `/bills/{billId}`.

#### [NEW] [BillSplit.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/BillSplit.kt)
- Model for `/bills/{billId}/splits/{uid}`.

#### [NEW] [Settlement.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/Settlement.kt)
- Model for `/settlements/{settlementId}`.

#### [NEW] [QrPayment.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/QrPayment.kt)
- Model for `/qr_payments/{paymentId}`.

#### [NEW] [Notification.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/Notification.kt)
- Model for `/notifications/{notificationId}`.

#### [NEW] [Comment.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/Comment.kt)
- Model for `/posts/{postId}/comments/{commentId}`.

#### [NEW] [MemberDetail.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/MemberDetail.kt)
- Model for `/groups/{groupId}/members_detail/{uid}`.

---

### [Personal Finance Models]

#### [Transaction.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/model/Transaction.kt)
- Ensure it aligns with potential personal finance storage needs (though not explicitly in `database-schema.md`, it's part of the app).

## Verification Plan

### Automated Tests
- Since these are mainly Data Classes, verification will involve:
    - Ensuring they compile.
    - Creating a small test to verify Firestore serialization/deserialization if possible (using a Mock or simple unit test).

### Manual Verification
- I will check each class against the `database-schema.md` to ensure all fields are present and correctly typed.
- I will run `gradle assembleDebug` to ensure no compilation errors are introduced in the UI layers that use these models.
