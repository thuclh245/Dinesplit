package com.example.dinesplit.domain.model

/**
 * Enum định nghĩa các đích đến điều hướng (deep link destination) khi người dùng bấm vào thông báo.
 * Mỗi giá trị tương ứng với một màn hình hoặc trang cụ thể trong ứng dụng.
 */
enum class NotificationDestination {
    /** Chi tiết hoạt động mạng xã hội (like, bình luận). */
    ACTIVITY_DETAIL,
    /** Chi tiết bài viết trên nguồn cấp dữ liệu. */
    POST_DETAIL,
    /** Trang hồ sơ cá nhân. */
    PROFILE,
    /** Chi tiết nhóm chia tiền. */
    SPLIT_DETAIL,
    /** Màn hình tất toán / thanh toán hóa đơn chia tiền. */
    SPLIT_SETTLE,
    /** Chi tiết một giao dịch tài chính cá nhân. */
    TRANSACTION_DETAIL,
    /** Màn hình quản lý nhắc nhở chi tiêu ngân sách. */
    SPENDING_REMINDERS,
    /** Màn hình quản lý danh mục tài chính. */
    CATEGORY_MANAGEMENT,
    /** Màn hình quản lý kế hoạch tài chính cá nhân (mục tiêu, ví, quy tắc lặp). */
    PERSONAL_PLANS,
    /** Màn hình chính tài chính cá nhân. */
    PERSONAL,
    /** Không điều hướng - thông báo chỉ mang tính thông tin. */
    NONE,
}
