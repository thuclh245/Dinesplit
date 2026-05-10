# Wireframe Low-fi Main Screens - DineSplit Social

## 1. Mục tiêu
Đây là wireframe mức khung để dựng Figma/Stitch hoặc code placeholder screens trong Android Studio.

---

## 2. Splash
```text
+--------------------------------+
|                                |
|        DineSplit Social        |
|        [App Logo Placeholder]  |
|                                |
|        Loading / Session...    |
|                                |
+--------------------------------+
```

## 3. Login
```text
+--------------------------------+
|            Welcome Back        |
|  Social dining + split bills   |
|                                |
|  [ Email TextField ]           |
|  [ Password TextField ]        |
|                                |
|  [ Login Button ]              |
|  [ Google Sign-In Button ]     |
|                                |
|  Don't have account? Register  |
+--------------------------------+
```

## 4. Register
```text
+--------------------------------+
|           Create Account       |
|                                |
|  [ Name TextField ]            |
|  [ Email TextField ]           |
|  [ Password TextField ]        |
|  [ Confirm Password ]          |
|                                |
|  [ Register Button ]           |
|                                |
|  Already have account? Login   |
+--------------------------------+
```

## 5. Complete Profile
```text
+--------------------------------+
|         Complete Profile       |
|                                |
|       [ Avatar Placeholder ]   |
|       [ Upload Avatar ]        |
|                                |
|  [ Display Name ]              |
|  [ Username ]                  |
|  [ Bio ]                       |
|                                |
|  [ Continue Button ]           |
+--------------------------------+
```

## 6. Feed Home
```text
+--------------------------------+
| Feed                [Bell][+ ] |
| [Search users/posts/places]    |
|--------------------------------|
| [Post Card]                    |
|  Avatar  Name     Location     |
|  [Image Placeholder]           |
|  Caption...                    |
|  [Like] [Comment]              |
|--------------------------------|
| [Post Card]                    |
+--------------------------------+
| Feed | Split | Personal | Prof |
+--------------------------------+
```

## 7. Create Post
```text
+--------------------------------+
| < Back       Create Post       |
|                                |
| [ Image Upload Placeholder ]   |
| [ Caption TextArea ]           |
| [ Location TextField ]         |
|                                |
| [ Publish Button ]             |
+--------------------------------+
```

## 8. Post Detail
```text
+--------------------------------+
| < Back       Post Detail       |
|                                |
| [Image Placeholder]            |
| Author / Location              |
| Caption...                     |
| [Like] [Comment]               |
|--------------------------------|
| Comments                       |
| [Comment Row]                  |
| [Comment Row]                  |
| [Add Comment Field]            |
+--------------------------------+
```

## 9. Search
```text
+--------------------------------+
| < Back          Search         |
| [ Search TextField ]           |
| [Users] [Posts] [Places]       |
|--------------------------------|
| Search Result Item             |
| Search Result Item             |
| Search Result Item             |
+--------------------------------+
```

## 10. Group List
```text
+--------------------------------+
| Split               [ + Group ]|
|--------------------------------|
| [Group Card]                   |
| Group name                     |
| 4 members / 2 open bills       |
|--------------------------------|
| [Group Card]                   |
+--------------------------------+
| Feed | Split | Personal | Prof |
+--------------------------------+
```

## 11. Group Detail
```text
+--------------------------------+
| < Back       Group Detail      |
| Group Name      [ + Bill ]     |
| Members: A, B, C, D            |
|--------------------------------|
| Open Bills                      |
| [Bill Card]                    |
| [Bill Card]                    |
+--------------------------------+
```

## 12. Create Bill
```text
+--------------------------------+
| < Back        Create Bill      |
|                                |
| [ Title TextField ]            |
| [ Total Amount ]               |
| [ Payer Dropdown ]             |
| [ Split Type Chips ]           |
| [ Participants Selector ]      |
| [ Share Preview Card ]         |
|                                |
| [ Save Bill Button ]           |
+--------------------------------+
```

## 13. Bill Detail
```text
+--------------------------------+
| < Back         Bill Detail     |
| Dinner at ABC                  |
| Total: 480k | Payer: Minh      |
|--------------------------------|
| Shares                         |
| A - 120k - unpaid              |
| B - 120k - paid                |
| C - 120k - unpaid              |
| D - 120k - paid                |
|--------------------------------|
| [ View Settle Summary ]        |
+--------------------------------+
```

## 14. Settle Summary
```text
+--------------------------------+
| < Back      Settle Summary     |
|                                |
| A pays B 120k                  |
| C pays B 120k                  |
|                                |
| [ Mark Settled / Close ]       |
+--------------------------------+
```

## 15. Personal Dashboard
```text
+--------------------------------+
| Personal            [ + Add ]  |
|--------------------------------|
| [Income Card] [Expense Card]   |
| [Balance Card]                 |
| [Chart Placeholder]            |
|--------------------------------|
| Recent Transactions            |
| [Transaction Item]             |
| [Transaction Item]             |
+--------------------------------+
| Feed | Split | Personal | Prof |
+--------------------------------+
```

## 16. Add Transaction
```text
+--------------------------------+
| < Back      Add Transaction    |
| [ Expense / Income Toggle ]    |
| [ Amount TextField ]           |
| [ Category Dropdown ]          |
| [ Note TextField ]             |
| [ Date Picker ]                |
|                                |
| [ Save Button ]                |
+--------------------------------+
```

## 17. Transaction History
```text
+--------------------------------+
| < Back     Transaction History |
| [Type Filter] [Date Filter]    |
|--------------------------------|
| [Transaction Item]             |
| [Transaction Item]             |
| [Transaction Item]             |
+--------------------------------+
```

## 18. My Profile
```text
+--------------------------------+
| Profile             [Edit]     |
| [Avatar]  Display Name         |
| @username                      |
| Bio                            |
|--------------------------------|
| My Posts                       |
| [Post Grid / Post Card]        |
+--------------------------------+
| Feed | Split | Personal | Prof |
+--------------------------------+
```

## 19. Notifications
```text
+--------------------------------+
| < Back       Notifications     |
|--------------------------------|
| [Notification Item]            |
| Minh commented on your post    |
|--------------------------------|
| [Notification Item]            |
| New bill added in Weekend Eat  |
+--------------------------------+
```

## 20. Notes cho Figma/Stitch
- Dùng card-based layout
- Feed ưu tiên image-rich
- Personal ưu tiên summary + chart
- Split ưu tiên clarity về status và amount
- Tất cả màn nên có empty/loading/error state
