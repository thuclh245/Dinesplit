# Implementation Plan - Comprehensive Database Infrastructure

This plan outlines the steps to build a robust database infrastructure (Repositories) for all entities defined in the Firestore schema.

## Proposed Changes

### [Domain Layer] (E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/repository)

#### [SplitRepository.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/repository/SplitRepository.kt)
- Expand to include `Bill` management and `Settlement` operations.
- Add `getBills(groupId)`, `createBill(bill, splits)`, `getSettlements(groupId)`.

#### [NEW] [NotificationRepository.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/repository/NotificationRepository.kt)
- Interface for fetching and marking notifications as read.

#### [FeedRepository.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/domain/repository/FeedRepository.kt)
- Expand to include `Comment` management and `Like` functionality.

---

### [Data Layer] (E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/data/repository)

#### [FirebaseSplitRepository.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/data/repository/FirebaseSplitRepository.kt)
- Implement full Bill/Split logic using Firestore Transactions or Batched Writes to ensure consistency between `bills`, `bills/splits`, and group `totalSpent`.

#### [FirebaseFeedRepository.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/data/repository/FirebaseFeedRepository.kt)
- Implement `comments` sub-collection access and real-time feed updates.

#### [NEW] [FirebaseNotificationRepository.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/data/repository/FirebaseNotificationRepository.kt)
- Implement notification fetching for specific users.

---

### [Utility/Core]

#### [NEW] [FirestoreCollections.kt](file:///E:/PKI/DineSplit/app/src/main/java/com/example/dinesplit/core/firebase/FirestoreCollections.kt)
- Centralized constants for collection names to avoid typos.

## Verification Plan

### Automated Tests
- Unit tests for Repository logic using Mockk for Firestore (if setup allows).
- Verification of data mapping using small unit tests for each Model.

### Manual Verification
- I will perform a "Dry Run" by writing a temporary verification script/test that simulates:
    1. Creating a Group.
    2. Adding a Bill.
    3. Checking if the sub-collections are structured correctly in the code logic.
- Verify that `Date` objects are correctly handled in the mapping logic.
