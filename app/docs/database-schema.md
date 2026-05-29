# DineSplit — Firestore Database Schema

## Tổng quan kiến trúc

```
Firebase Services:
├── Firestore      → Tất cả structured data
├── Storage        → Ảnh (avatars, posts, bill receipts)
├── Auth           → Authentication (đã có)
├── Messaging      → Push notifications (FCM)
└── Cloud Functions → Triggers (notifications, QR payment verify)
```

---

## Collections & Documents

---

### 1. `users` — Thông tin người dùng

```
/users/{uid}
{
  uid: string,
  displayName: string,
  username: string,           // unique, lowercase
  email: string,
  avatarUrl: string,
  bio: string,
  diningStyles: string[],    // ["fine_dining", "cafe_hopper", "nightlife"]
  followersCount: number,
  followingCount: number,
  postsCount: number,
  fcmToken: string,           // cho push notification
  createdAt: timestamp,
  updatedAt: timestamp
}
```

**Sub-collection:**
```
/users/{uid}/followers/{followerUid}
{
  uid: string,
  followedAt: timestamp
}

/users/{uid}/following/{followingUid}
{
  uid: string,
  followedAt: timestamp
}
```

---

### 2. `usernames` — Username uniqueness (đã có)

```
/usernames/{username}
{
  uid: string
}
```

---

### 3. `groups` — Nhóm chia tiền

```
/groups/{groupId}
{
  id: string,
  name: string,
  avatarUrl: string,
  category: string,           // "food", "travel", "housing", "other"
  createdBy: string,          // uid của người tạo
  admins: string[],           // danh sách uid có quyền admin
  members: string[],          // danh sách tất cả uid (bao gồm admins)
  memberCount: number,
  totalSpent: number,         // tổng chi tiêu nhóm
  isSettled: boolean,         // nhóm đã tất toán chưa
  qrPaymentEnabled: boolean,  // bật tính năng QR chung
  qrBankAccount: string,      // số tài khoản nhận tiền chung (nếu có)
  qrBankName: string,         // tên ngân hàng
  qrAccountHolder: string,    // tên chủ tài khoản
  createdAt: timestamp,
  updatedAt: timestamp
}
```

**Sub-collection:**
```
/groups/{groupId}/members_detail/{uid}
{
  uid: string,
  displayName: string,
  avatarUrl: string,
  role: string,               // "admin" | "member"
  joinedAt: timestamp,
  totalOwed: number,          // tổng nợ trong nhóm
  totalPaid: number           // tổng đã trả
}
```

---

### 4. `bills` — Hóa đơn trong nhóm

```
/bills/{billId}
{
  id: string,
  groupId: string,
  title: string,
  totalAmount: number,
  currency: string,           // "VND"
  paidBy: string,             // uid người trả tiền
  splitMethod: string,        // "equal" | "custom" | "by_item"
  date: timestamp,            // ngày hóa đơn
  note: string,
  receiptUrl: string,         // ảnh hóa đơn (optional)
  createdBy: string,          // uid người tạo bill
  isSettled: boolean,         // tất cả đã trả chưa
  createdAt: timestamp,
  updatedAt: timestamp
}
```

**Sub-collection:**
```
/bills/{billId}/splits/{uid}
{
  uid: string,
  displayName: string,
  amount: number,             // số tiền phải trả
  isPaid: boolean,
  paidAt: timestamp | null,
  paidVia: string | null      // "manual" | "qr_transfer" | null
}
```

---

### 5. `settlements` — Lịch sử thanh toán / chốt sổ

```
/settlements/{settlementId}
{
  id: string,
  groupId: string,
  billId: string | null,      // null nếu là settle tổng hợp
  fromUid: string,            // người trả
  toUid: string,              // người nhận
  amount: number,
  method: string,             // "manual" | "qr_transfer"
  qrTransactionRef: string,   // mã giao dịch từ bank (nếu QR)
  note: string,
  status: string,             // "pending" | "confirmed" | "rejected"
  confirmedAt: timestamp | null,
  createdAt: timestamp
}
```

---

### 6. `qr_payments` — QR Payment chung (VNPay-style)

```
/qr_payments/{paymentId}
{
  id: string,
  groupId: string,
  billId: string | null,
  payerUid: string,           // người chuyển tiền
  receiverUid: string,        // người nhận (admin/người tạo bill)
  amount: number,
  bankTransactionRef: string, // mã giao dịch ngân hàng
  qrContent: string,          // nội dung QR đã generate
  status: string,             // "pending" | "verified" | "failed"
  verifiedAt: timestamp | null,
  createdAt: timestamp
}
```

---

### 7. `posts` — Social Feed

```
/posts/{postId}
{
  id: string,
  authorUid: string,
  authorName: string,         // denormalized cho query nhanh
  authorAvatar: string,       // denormalized
  caption: string,
  imageUrls: string[],        // nhiều ảnh, lưu trên Firebase Storage
  location: string | null,    // "Haidilao Vincom, HCM"
  linkedGroupId: string | null,   // link đến group (optional)
  linkedBillId: string | null,    // link đến bill (optional)
  likesCount: number,
  commentsCount: number,
  visibility: string,         // "public" | "followers_only"
  tags: string[],             // hashtags hoặc tagged users
  createdAt: timestamp,
  updatedAt: timestamp
}
```

**Sub-collections:**
```
/posts/{postId}/likes/{uid}
{
  uid: string,
  likedAt: timestamp
}

/posts/{postId}/comments/{commentId}
{
  id: string,
  authorUid: string,
  authorName: string,
  authorAvatar: string,
  content: string,
  createdAt: timestamp
}
```

---

### 8. `notifications` — Thông báo

```
/notifications/{notificationId}
{
  id: string,
  recipientUid: string,       // người nhận
  type: string,               // xem bảng types bên dưới
  title: string,
  body: string,
  data: map {                 // payload tùy theo type
    groupId: string | null,
    billId: string | null,
    postId: string | null,
    fromUid: string | null,
    fromName: string | null,
    amount: number | null
  },
  isRead: boolean,
  createdAt: timestamp
}
```

**Notification Types:**
| Type | Trigger | Ví dụ |
|------|---------|-------|
| `bill_created` | Có bill mới trong group | "Minh tạo hóa đơn Lẩu Haidilao - 1.200.000đ" |
| `payment_received` | Ai đó trả tiền cho bạn | "Thanh Hằng đã trả 200.000đ" |
| `payment_reminder` | Nhắc nợ | "Minh nhắc bạn trả 150.000đ" |
| `qr_payment_verified` | QR payment xác nhận | "Giao dịch 200.000đ đã được xác nhận" |
| `group_invite` | Được mời vào group | "Bạn được mời vào nhóm Biệt đội lẩu" |
| `post_like` | Ai đó like post | "Thanh Hằng thích bài viết của bạn" |
| `post_comment` | Ai đó comment | "Minh bình luận: Ngon quá!" |
| `new_follower` | Có người follow | "linh_eats bắt đầu theo dõi bạn" |
| `settle_complete` | Group đã tất toán | "Nhóm Cuối tuần ăn vặt đã tất toán!" |

---

### 9. `feed_timeline` — Feed cá nhân (denormalized cho performance)

```
/feed_timeline/{uid}/posts/{postId}
{
  postId: string,
  authorUid: string,
  createdAt: timestamp
}
```

> Khi user A post, Cloud Function sẽ fan-out postId vào feed_timeline của tất cả followers. Giúp query feed nhanh mà không cần join.

---

## Firebase Storage Structure

```
/avatars/{uid}/profile.jpg
/posts/{postId}/{imageIndex}.jpg        // 0.jpg, 1.jpg, 2.jpg...
/bills/{billId}/receipt.jpg
/groups/{groupId}/avatar.jpg
```

---

## Firestore Indexes (cần tạo)

| Collection | Fields | Order |
|-----------|--------|-------|
| `bills` | groupId, createdAt | ASC, DESC |
| `posts` | authorUid, createdAt | ASC, DESC |
| `notifications` | recipientUid, isRead, createdAt | ASC, ASC, DESC |
| `settlements` | groupId, createdAt | ASC, DESC |
| `feed_timeline/{uid}/posts` | createdAt | DESC |

---

## Firestore Security Rules (cơ bản)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users: chỉ owner mới sửa được profile mình
    match /users/{uid} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == uid;

      match /followers/{followerId} {
        allow read: if request.auth != null;
        allow write: if request.auth.uid == followerId;
      }
      match /following/{followingId} {
        allow read: if request.auth != null;
        allow write: if request.auth.uid == resource.data.uid
                     || request.auth.uid == followingId;
      }
    }

    // Usernames: chỉ owner
    match /usernames/{username} {
      allow read: if true;
      allow create: if request.auth != null;
      allow delete: if request.auth != null
                    && resource.data.uid == request.auth.uid;
    }

    // Groups: members mới đọc được, admins mới sửa
    match /groups/{groupId} {
      allow read: if request.auth != null
                  && request.auth.uid in resource.data.members;
      allow create: if request.auth != null;
      allow update: if request.auth != null
                    && request.auth.uid in resource.data.admins;

      match /members_detail/{uid} {
        allow read: if request.auth != null;
        allow write: if request.auth != null
                     && request.auth.uid in get(/databases/$(database)/documents/groups/$(groupId)).data.admins;
      }
    }

    // Bills: group members đọc, admins tạo/sửa
    match /bills/{billId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null;

      match /splits/{uid} {
        allow read: if request.auth != null;
        allow write: if request.auth != null;
      }
    }

    // Posts: public đọc, owner sửa/xóa
    match /posts/{postId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null
                    && request.resource.data.authorUid == request.auth.uid;
      allow update, delete: if request.auth != null
                            && resource.data.authorUid == request.auth.uid;

      match /likes/{uid} {
        allow read: if request.auth != null;
        allow write: if request.auth.uid == uid;
      }
      match /comments/{commentId} {
        allow read: if request.auth != null;
        allow create: if request.auth != null;
        allow delete: if request.auth != null
                      && resource.data.authorUid == request.auth.uid;
      }
    }

    // Notifications: chỉ recipient đọc
    match /notifications/{notifId} {
      allow read: if request.auth != null
                  && resource.data.recipientUid == request.auth.uid;
      allow update: if request.auth != null
                    && resource.data.recipientUid == request.auth.uid;
      allow create: if request.auth != null;
    }

    // Settlements
    match /settlements/{id} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null;
    }

    // QR Payments
    match /qr_payments/{id} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null;
    }

    // Feed timeline
    match /feed_timeline/{uid}/posts/{postId} {
      allow read: if request.auth.uid == uid;
      allow write: if false; // chỉ Cloud Functions mới write
    }
  }
}
```

---

## Cloud Functions cần viết (Node.js/TypeScript)

| Function | Trigger | Mô tả |
|----------|---------|--------|
| `onBillCreated` | Firestore onCreate `/bills/{id}` | Tạo notifications cho members, gửi FCM push |
| `onSettlementCreated` | Firestore onCreate `/settlements/{id}` | Cập nhật bill split isPaid, gửi notification |
| `onPostCreated` | Firestore onCreate `/posts/{id}` | Fan-out vào feed_timeline của followers |
| `onPostLiked` | Firestore onCreate `/posts/{id}/likes/{uid}` | Gửi notification cho post author |
| `onCommentCreated` | Firestore onCreate `/posts/{id}/comments/{cid}` | Gửi notification cho post author |
| `onFollowCreated` | Firestore onCreate `/users/{uid}/followers/{fid}` | Gửi notification, update counts |
| `onQrPaymentCreated` | Firestore onCreate `/qr_payments/{id}` | Verify payment, update settlement status |
| `sendPushNotification` | Callable/helper | Gửi FCM push dựa trên user's fcmToken |

---

## Diagram quan hệ (simplified)

```
users ──────┐
  │         │
  │ follow  │ member of
  ▼         ▼
users    groups
            │
            │ has many
            ▼
          bills
            │
            │ has many
            ▼
         splits ──→ settlements ──→ qr_payments
            
users ──→ posts ──→ likes
                 ──→ comments

Cloud Functions ──→ notifications ──→ FCM Push
                ──→ feed_timeline (fan-out)
```
