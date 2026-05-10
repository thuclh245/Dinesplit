# Schema v1 - DineSplit Social

## 1. Nguyên tắc
- Chỉ chốt schema đủ dùng cho khung cơ bản
- Tên field rõ ràng, thống nhất
- Money nên lưu dạng số nguyên (Long)
- Dùng timestamp nhất quán

## 2. Collections overview
```text
users
follows
posts
posts/{postId}/comments
posts/{postId}/likes
transactions
users/{userId}/categories
groups
groups/{groupId}/members
groups/{groupId}/bills
groups/{groupId}/bills/{billId}/shares
groups/{groupId}/bills/{billId}/settlements
notifications
```

## 3. User
### Collection: users
- uid: String
- displayName: String
- username: String
- email: String
- avatarUrl: String?
- bio: String?
- isPublic: Boolean
- createdAt: Timestamp
- updatedAt: Timestamp

## 4. Follow
### Collection: follows
- followerId: String
- followingId: String
- createdAt: Timestamp

## 5. Post
### Collection: posts
- authorId: String
- imageUrl: String
- caption: String
- locationName: String?
- taggedUserIds: List<String>
- visibility: String (public / followers)
- likeCount: Int
- commentCount: Int
- createdAt: Timestamp
- updatedAt: Timestamp

## 6. Comment
### Collection: posts/{postId}/comments
- authorId: String
- content: String
- createdAt: Timestamp

## 7. Like
### Collection: posts/{postId}/likes
- userId: String
- createdAt: Timestamp

## 8. Transaction
### Collection: transactions
- transactionId: String
- userId: String
- type: String (income / expense)
- amount: Long
- categoryId: String
- categoryName: String
- note: String?
- createdAt: Timestamp
- updatedAt: Timestamp

## 9. Category
### Collection: users/{userId}/categories
- name: String
- type: String (income / expense)
- iconKey: String
- colorKey: String
- isDefault: Boolean
- createdAt: Timestamp

## 10. Group
### Collection: groups
- groupId: String
- name: String
- type: String
- ownerId: String
- memberCount: Int
- createdAt: Timestamp
- updatedAt: Timestamp

## 11. Group Member
### Collection: groups/{groupId}/members
- userId: String
- role: String
- joinedAt: Timestamp

## 12. Bill
### Collection: groups/{groupId}/bills
- billId: String
- title: String
- totalAmount: Long
- splitType: String (equal / custom / itemized)
- payerId: String
- createdBy: String
- status: String (open / settled)
- createdAt: Timestamp
- updatedAt: Timestamp

## 13. Share
### Collection: groups/{groupId}/bills/{billId}/shares
- userId: String
- amountOwed: Long
- amountPaid: Long
- paymentStatus: String (unpaid / partial / paid)

## 14. Settlement
### Collection: groups/{groupId}/bills/{billId}/settlements
- fromUserId: String
- toUserId: String
- amount: Long

## 15. Notification
### Collection: notifications
- receiverId: String
- type: String
- refType: String
- refId: String
- title: String
- body: String
- isRead: Boolean
- createdAt: Timestamp

## 16. Validation rules v1
- email phải đúng format
- password có độ dài tối thiểu theo rule team chốt
- amount > 0
- bill total > 0
- sum(shares) == totalAmount
- không tạo bill nếu participant list rỗng
- paymentStatus không được vượt amountOwed

## 17. Source of truth v1
- Auth state: Firebase Auth
- Profile basic: users
- Feed content: posts + comments + likes
- Personal finance: transactions + categories
- Split data: groups + bills + shares + settlements
- In-app notification: notifications
