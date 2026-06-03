package com.example.dinesplit.data.seeder

import com.example.dinesplit.domain.repository.PersonalRepository
import com.example.dinesplit.domain.repository.NotificationRepository
import com.example.dinesplit.domain.repository.FeedRepository
import com.example.dinesplit.domain.repository.SplitRepository
import com.example.dinesplit.domain.model.Notification
import com.example.dinesplit.domain.model.NotificationType
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.Post
import com.example.dinesplit.domain.model.UserProfile
import com.example.dinesplit.domain.model.Group
import com.example.dinesplit.domain.model.Member
import com.example.dinesplit.domain.model.Bill
import com.example.dinesplit.domain.model.SplitMethod
import java.util.Date
import java.util.UUID

/**
 * Debug-only seeder to inject demo transactions + notifications + posts + splits.
 * Use only for dev/demo, not in production release.
 *
 * Usage:
 *   viewModelScope.launch(Dispatchers.IO) {
 *       DemoDataSeeder.seedDemoTransactions(personalRepo, userId)
 *       DemoDataSeeder.seedDemoNotifications(notificationRepo, userId)
 *       DemoDataSeeder.seedDemoPosts(feedRepo, currentUserProfile)
 *   }
 */
@Suppress("unused", "ObjectName")
object DemoDataSeeder {
    @Suppress("unused")
    suspend fun seedDemoTransactions(
        personalRepo: PersonalRepository,
        userId: String
    ) {
        val now = System.currentTimeMillis()

        val expenses = listOf(
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 150000.0,
                type = TransactionType.EXPENSE,
                categoryId = "c_food",
                category = "Ăn ngoài",
                note = "Ăn trưa cùng đội",
                date = now - 1000 * 60 * 60 * 2,
                createdAt = now
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 50000.0,
                type = TransactionType.EXPENSE,
                categoryId = "c_transit",
                category = "Di chuyển",
                note = "Đi xe công nghệ đến văn phòng",
                date = now - 1000 * 60 * 60 * 24,
                createdAt = now
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 320000.0,
                type = TransactionType.EXPENSE,
                categoryId = "c_grocery",
                category = "Tạp hóa",
                note = "Mua sắm hằng tuần",
                date = now - 1000 * 60 * 60 * 48,
                createdAt = now
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 200000.0,
                type = TransactionType.INCOME,
                categoryId = "c_bonus",
                category = "Thưởng",
                note = "Thưởng hoàn thành dự án",
                date = now - 1000 * 60 * 60 * 72,
                createdAt = now
            ),
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = 100000.0,
                type = TransactionType.EXPENSE,
                categoryId = "c_fun",
                category = "Giải trí",
                note = "Vé xem phim",
                date = now - 1000 * 60 * 60 * 96,
                createdAt = now
            )
        )

        expenses.forEach { transaction ->
            try {
                personalRepo.insertTransaction(transaction)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("unused")
    suspend fun seedDemoNotifications(
        notificationRepo: NotificationRepository,
        userId: String
    ) {
        val now = System.currentTimeMillis()
        val notifications = listOf(
            Notification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Cảnh báo chi tiêu: Ăn ngoài",
                subtitle = "Bạn đã dùng 75% ngân sách 300,000 VND",
                type = NotificationType.TRANSACTION_ALERT,
                relatedId = "c_food",
                isRead = false,
                createdAt = now - 1000 * 60 * 30,
                updatedAt = now - 1000 * 60 * 30,
                deepLinkDestination = "SPENDING_REMINDERS",
                deepLinkTargetId = "c_food"
            ),
            Notification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "John đã thanh toán cho bạn",
                subtitle = "Đã tất toán chia tiền bữa tối nhóm - 120,000 VND",
                type = NotificationType.PAYMENT_COMPLETED,
                relatedId = "bill_123",
                isRead = true,
                createdAt = now - 1000 * 60 * 60 * 2,
                updatedAt = now - 1000 * 60 * 60 * 2,
                deepLinkDestination = "SPLIT_SETTLE",
                deepLinkTargetId = "bill_123"
            ),
            Notification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Đã tạo hóa đơn mới",
                subtitle = "Kế hoạch chuyến đi cuối tuần - 500,000 VND",
                type = NotificationType.BILL_CREATED,
                relatedId = "bill_456",
                isRead = false,
                createdAt = now - 1000 * 60 * 60 * 3,
                updatedAt = now - 1000 * 60 * 60 * 3,
                deepLinkDestination = "SPLIT_DETAIL",
                deepLinkTargetId = "bill_456"
            )
        )

        notifications.forEach { notification ->
            try {
                notificationRepo.insertNotification(notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun seedDemoSplit(
        splitRepo: SplitRepository,
        userId: String
    ) {
        val groupId = "demo_group_id"
        val billId = "demo_bill_id"

        val group = Group(
            id = groupId,
            name = "Team Ăn Nhậu Sài Gòn 🍻",
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=800",
            memberCount = 4,
            totalExpense = 600000.0,
            yourBalance = -150000.0,
            createdAt = System.currentTimeMillis(),
            ownerId = "chef_hoang_uid"
        )

        val members = listOf(
            Member(id = "chef_hoang_uid", name = "Minh Tú", initial = "T"),
            Member(id = "foodie_lan_uid", name = "Khánh Linh", initial = "L"),
            Member(id = "cafe_huy_uid", name = "Thế Huy", initial = "H"),
            Member(id = userId, name = "Tôi", initial = "M", isMe = true)
        )

        val bill = Bill(
            id = billId,
            groupId = groupId,
            name = "Tiệc Nướng BBQ Cuối Tuần 🥩",
            totalAmount = 600000.0,
            payerId = "chef_hoang_uid",
            method = SplitMethod.EQUAL,
            shares = mapOf(
                "chef_hoang_uid" to 150000.0,
                "foodie_lan_uid" to 150000.0,
                "cafe_huy_uid" to 150000.0,
                userId to 150000.0
            ),
            paidMemberIds = listOf("chef_hoang_uid", "foodie_lan_uid"),
            date = System.currentTimeMillis() - 1000 * 60 * 60 * 24
        )

        try {
            splitRepo.createGroup(group, members)
            splitRepo.saveBill(bill)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun seedDemoPosts(
        feedRepo: FeedRepository,
        currentUserProfile: UserProfile?
    ) {
        val now = System.currentTimeMillis()
        val posts = mutableListOf(
            Post(
                id = UUID.randomUUID().toString(),
                authorUid = "chef_hoang_uid",
                authorName = "Minh Tú",
                authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                caption = "Bát phở thơm ngon chuẩn vị gia truyền! Thơm lừng gừng sả nước dùng thanh ngọt, thịt bò tái chín siêu mềm. Sáng ra cứ phải làm một bát cho ấm cái bụng 🍜❤️",
                imageUrls = listOf("https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=800&auto=format&fit=crop"),
                location = "Phở Gia Truyền Bát Đàn, Hà Nội",
                likesCount = 12,
                commentsCount = 3,
                visibility = "public",
                createdAt = Date(now - 1000 * 60 * 60 * 2),
                updatedAt = Date(now - 1000 * 60 * 60 * 2)
            ),
            Post(
                id = UUID.randomUUID().toString(),
                authorUid = "foodie_lan_uid",
                authorName = "Khánh Linh",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                caption = "Cuối tuần tụ tập anh em làm bữa đại tiệc sườn nướng BBQ ngập tràn phô mai kéo sợi. Ngon quên lối về! 🍖🔥",
                imageUrls = listOf("https://images.unsplash.com/photo-1544025162-d76694265947?w=800&auto=format&fit=crop"),
                location = "Gogi House, Vincom Đồng Khởi",
                likesCount = 28,
                commentsCount = 5,
                visibility = "public",
                linkedGroupId = "demo_group_id",
                linkedBillId = "demo_bill_id",
                createdAt = Date(now - 1000 * 60 * 60 * 24),
                updatedAt = Date(now - 1000 * 60 * 60 * 24)
            ),
            Post(
                id = UUID.randomUUID().toString(),
                authorUid = "cafe_huy_uid",
                authorName = "Thế Huy",
                authorAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                caption = "Tìm một góc bình yên giữa lòng Sài Gòn tấp nập. Espresso đắng nhẹ hòa quyện cùng bánh sừng bò thơm ngậy bơ. Không gian cực chill cho ngày mưa nhẹ ☕️🥐",
                imageUrls = listOf("https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800&auto=format&fit=crop"),
                location = "The Coffee House, Quận 1, TP. HCM",
                likesCount = 18,
                commentsCount = 2,
                visibility = "public",
                createdAt = Date(now - 1000 * 60 * 60 * 48),
                updatedAt = Date(now - 1000 * 60 * 60 * 48)
            )
        )

        if (currentUserProfile != null) {
            posts.add(
                Post(
                    id = UUID.randomUUID().toString(),
                    authorUid = currentUserProfile.uid,
                    authorName = currentUserProfile.displayName.ifBlank { "Tôi" },
                    authorAvatar = currentUserProfile.avatarUrl,
                    caption = "Trà sữa full topping cho ngày dài làm việc mệt mỏi! Nhìn trân châu hoàng kim dai giòn sần sật cưng xỉu luôn á mọi người ơi 🧋✨",
                    imageUrls = listOf("https://images.unsplash.com/photo-1541658016709-82535e94bc69?w=800&auto=format&fit=crop"),
                    location = "Gong Cha, Hà Nội",
                    likesCount = 5,
                    commentsCount = 1,
                    visibility = "public",
                    createdAt = Date(now),
                    updatedAt = Date(now)
                )
            )
        }

        posts.forEach { post ->
            try {
                feedRepo.createPost(post)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

