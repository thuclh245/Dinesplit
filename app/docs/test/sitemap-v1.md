# Sitemap v1 - DineSplit Social

## 1. Nguyên tắc
- Có 2 tầng điều hướng chính:
  - Auth Stack
  - Main App Stack
- Main App dùng bottom navigation 4 tab
- Notifications và Assistant là utility screens, không phải main tabs

## 2. App map tổng thể
```text
DineSplit Social
├── Auth Stack
│   ├── Splash
│   ├── Login
│   ├── Register
│   └── Complete Profile
│
├── Main App
│   ├── Feed Tab
│   │   ├── Feed Home
│   │   ├── Create Post
│   │   ├── Post Detail
│   │   ├── Search
│   │   └── Other User Profile
│   │
│   ├── Split Tab
│   │   ├── Group List
│   │   ├── Create Group
│   │   ├── Group Detail
│   │   ├── Create Bill
│   │   ├── Bill Detail
│   │   └── Settle Summary
│   │
│   ├── Personal Tab
│   │   ├── Personal Dashboard
│   │   ├── Add Transaction
│   │   ├── Transaction History
│   │   ├── Transaction Detail (optional)
│   │   └── Category Management
│   │
│   └── Profile Tab
│       ├── My Profile
│       ├── Edit Profile
│       └── Settings
│
└── Utility Screens
    ├── Notifications
    └── Assistant Entry
```

## 3. Navigation rules
- Splash quyết định route sang Auth hay Main App
- Auth screens không hiển thị bottom navigation
- Main App screens hiển thị bottom navigation ở top-level tabs
- Create / Detail screens có thể mở như nested destination
- Notification deeplink sang màn liên quan

## 4. MVP tối thiểu cho sitemap
Nếu cần rút scope ở giai đoạn đầu, vẫn phải giữ:
- Splash
- Login
- Register
- Feed Home
- Group List
- Personal Dashboard
- My Profile
- Notifications
