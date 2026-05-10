# Project Structure v1 - Android Studio

## 1. Mục tiêu
Cấu trúc này giúp team hiểu project trước khi chia task và tránh lệch package khi nhiều người code song song.

## 2. Package structure đề xuất
```text
app/
  src/main/java/com/dinesplitsocial/app/
    MainActivity.kt
    core/
      navigation/
      ui/
      util/
      common/
    data/
      remote/
      repository/
      mapper/
      model/
    domain/
      model/
      repository/
      usecase/
      validation/
    presentation/
      auth/
      feed/
      split/
      personal/
      profile/
      notification/
      assistant/
```

## 3. Gợi ý thư mục theo giai đoạn đầu
### core/navigation
- AppRoute.kt
- DineSplitApp.kt
- AppNavHost.kt
- BottomNavItem.kt

### core/ui
- AppScaffold.kt
- PlaceholderScreen.kt
- EmptyStateBlock.kt
- LoadingBlock.kt

### presentation/auth
- SplashScreen.kt
- LoginScreen.kt
- RegisterScreen.kt
- CompleteProfileScreen.kt

### presentation/feed
- FeedScreen.kt
- CreatePostScreen.kt
- PostDetailScreen.kt
- SearchScreen.kt

### presentation/split
- GroupListScreen.kt
- CreateGroupScreen.kt
- GroupDetailScreen.kt
- CreateBillScreen.kt
- BillDetailScreen.kt
- SettleSummaryScreen.kt

### presentation/personal
- PersonalDashboardScreen.kt
- AddTransactionScreen.kt
- TransactionHistoryScreen.kt

### presentation/profile
- ProfileScreen.kt
- EditProfileScreen.kt

### presentation/notification
- NotificationScreen.kt

### presentation/assistant
- AssistantScreen.kt

## 4. Mục tiêu của structure này
- nhìn vào là hiểu product map
- dễ gắn navigation
- dễ chia task sau này
- ít đụng nhau khi merge
- phù hợp MVVM + lightweight Clean Architecture
