# Personal Features and UI Roadmap

Ngay hien tai phan Personal da co nen tang chinh: giao dich, category, reminder, bieu do chi tieu va du lieu Firebase rieng theo tung user. Tai lieu nay de xuat cac tinh nang moi lien quan den nguoi dung ca nhan, dong thoi danh gia viec co nen update UI toan app hay khong.

Ket luan ngan: nen update UI truoc hoac song song voi tinh nang moi. Personal dang kha on, nhung app hien co nhieu style khac nhau giua Personal, Split va Profile. Neu them tinh nang ngay ma khong chuan hoa UI, man moi se de bi lech tone va kho bao tri.

## 1. Muc tieu san pham

- Bien Personal thanh trung tam tai chinh ca nhan, khong chi la noi ghi thu chi.
- Noi du lieu Split voi Personal de nguoi dung thay ro anh huong cua an uong/hoa don nhom len tien ca nhan.
- Tao cam giac app thong minh hon: tu nhac, tu phan tich, tu goi y hanh dong tiep theo.
- Giu giao dien gon, scan nhanh, phu hop app tai chinh-xa hoi dung hang ngay.

## 2. Tinh nang moi de xuat cho Personal

### 2.1 Smart Monthly Insights

Mo ta:
Personal tu dong tao the nhan xet theo thang, vi du:
- "Thang nay an uong tang 18% so voi thang truoc."
- "Ban da chi nhieu nhat vao cuoi tuan."
- "Chi tieu tu Split chiem 42% tong expense."

Gia tri:
- Nguoi dung khong phai tu doc bieu do.
- Lam Personal co cam giac thong minh va khac biet hon mot ledger binh thuong.

MVP:
- So sanh tong income/expense thang hien tai voi thang truoc.
- Tim category tang/giam manh nhat.
- Tim ngay trong tuan chi nhieu nhat.
- Hien 2-3 insight cards ngay duoi BalanceCard.

Du lieu can them:
- Co the tinh client-side tu transactions hien tai.
- Neu muon cache: `user_personal/{uid}/insights/{yyyyMM}`.

Do uu tien: Cao.

### 2.2 Safe-to-Spend Forecast

Mo ta:
Tinh so tien "co the chi moi ngay" den cuoi thang dua tren:
- So du hien tai.
- Thu nhap da ghi nhan.
- Expense thang nay.
- Budget/reminder dang bat.
- Cac recurring expense sap toi.

Gia tri:
- Khac voi app chi thong bao "ban da chi bao nhieu", tinh nang nay tra loi cau hoi "hom nay minh nen chi toi da bao nhieu".

MVP:
- The "Safe to spend today" tren Personal.
- Cong thuc don gian:
  `safeDaily = max(0, (monthlyIncome - monthlyExpense - upcomingRecurring - savingsGoal) / daysLeftInMonth)`
- Mau sac theo trang thai: tot, can canh giac, vuot muc.

Du lieu can them:
- `recurring_rules` neu co recurring.
- `monthlyGoal` hoac `savingsGoal` neu co muc tieu tiet kiem.

Do uu tien: Cao.

### 2.3 Recurring Expense Radar

Mo ta:
Tu dong phat hien hoac cho nguoi dung tao cac khoan lap lai: tien nha, Netflix, gym, internet, hoc phi, luong.

Gia tri:
- Lam forecast chinh xac hon.
- Nhac truoc ngay phai tra tien.
- Giam nhap lieu lap lai.

MVP:
- Man "Recurring" trong Personal.
- Tao rule: ten, amount, category, type, chu ky, ngay lap lai.
- Den ngay lap lai, hien card de user bam "Post transaction".

Mo rong:
- Tu phat hien giao dich lap lai dua tren ten/category/amount gan nhau.
- Cho phep auto-post neu user bat.

Du lieu can them:
- `user_personal/{uid}/recurring_rules/{ruleId}`.
- Transaction them optional field `recurringRuleId`.

Do uu tien: Trung binh cao.

### 2.4 Split-to-Personal Bridge

Mo ta:
Khi user tao bill, thanh toan, hoac duoc nhan tien trong Split, app goi y ghi mot transaction tuong ung vao Personal.

Gia tri:
- Ket noi hai phan quan trong nhat cua app.
- Personal se phan anh dung chi tieu an uong/nhom ban thay vi user phai nhap lai.

Luồng de xuat:
- Tao bill trong Split: neu user la payer, goi y expense "Paid for group".
- Thanh vien mark paid: voi payer, goi y income "Reimbursement".
- Voi nguoi tra tien, goi y expense "Split payment".

MVP:
- Sau khi tao/settle bill, hien bottom sheet "Add to Personal?".
- Transaction co `source = SPLIT`, `sourceGroupId`, `sourceBillId`.

Luu y ky thuat:
- Firestore rules hien dang validate transaction bang `keys().hasOnly(...)`. Neu them field source thi phai update rule va mapper cung luc.

Do uu tien: Cao.

### 2.5 Personal Goals and Challenges

Mo ta:
Nguoi dung tao muc tieu ca nhan:
- "Tiet kiem 2.000.000 VND thang nay."
- "Giu an uong duoi 1.500.000 VND."
- "7 ngay khong vuot budget cafe."

Gia tri:
- Tao dong luc quay lai app.
- Lam Reminder bot kho khan hon: khong chi canh bao, ma co hanh trinh.

MVP:
- Goals list: target, current progress, deadline, linked category optional.
- Progress ring nho tren Personal.
- Badge/streak khi dat muc tieu.

Du lieu can them:
- `user_personal/{uid}/goals/{goalId}`.

Do uu tien: Trung binh.

### 2.6 Receipt Capture and Auto Categorize

Mo ta:
Nguoi dung chup anh hoa don, app tao transaction voi amount/category/note goi y.

Gia tri:
- Giam friction khi nhap expense.
- Rat hop voi DineSplit vi ngu canh an uong/hoa don la core cua app.

MVP:
- Nut "Scan receipt" trong Add Transaction.
- Upload anh, cho user xac nhan amount/category truoc khi save.
- Chua can AI phuc tap: co the bat dau bang manual crop + nhap amount nhanh.

Mo rong:
- OCR amount.
- Goi y category dua tren merchant/keyword.
- Neu receipt co nhieu mon, cho tao Split bill tu receipt.

Du lieu can them:
- Transaction optional `receiptImageUrl`.
- Storage rules cho `receipts/{uid}/{transactionId}.jpg`.

Do uu tien: Trung binh, nen lam sau khi UI/form on dinh.

### 2.7 Personal Wallets

Mo ta:
Quan ly nhieu vi: cash, bank, e-wallet, credit card.

Gia tri:
- Personal chuyen tu ledger don gian thanh tai chinh ca nhan thuc te hon.
- Huu ich voi user Viet Nam co nhieu vi dien tu/ngan hang.

MVP:
- CRUD wallet.
- Transaction chon wallet.
- Tong so du theo wallet.

Luu y:
- Day la thay doi data model lon. Nen lam sau Smart Insights va Split-to-Personal Bridge.

Do uu tien: Thap den trung binh.

## 3. UI co can update khong?

Co. Nen lam mot dot UI consistency pass truoc khi them nhieu man moi.

### 3.1 Van de hien tai

- Ngon ngu hien dang tron: Personal dung English, Split dung Vietnamese, Profile dung English va co mock content.
- Bottom bar dang hard-code mau `Color(0xFFE65100)` va `Color.White`, trong khi cac man khac dung `MaterialTheme.colorScheme`.
- Card radius va elevation chua dong nhat: `AppCard` radius lon, Split co card 20/24dp, Profile lai co layout kieu social grid rieng.
- FAB cua Split dang co padding bottom rieng `115.dp`, de lech voi cac tab khac.
- Profile dang hien sample data/images, chua doc day du tu profile state.
- Mot so chu trong source/log bi mojibake khi doc bang terminal, can de y encoding UTF-8 khi sua text tieng Viet.
- Typography co nhieu override rieng trong tung screen; nen dua ve shared components.
- Mot so text cua finance dung `VND`, cho khac dung `d`/`k`; can chuan hoa format tien.

### 3.2 Huong UI moi

Nguyen tac:
- App tai chinh-xa hoi nen gon, scan nhanh, it trang tri thua.
- Dung 1 ngon ngu hien thi chinh. Neu target Viet Nam, nen Viet hoa Personal/Profile/Feed; giu ten tab neu can nhan dien thuong hieu.
- Dung design token chung: spacing, radius, elevation, color, typography.
- Dung icon + tooltip/contentDescription cho hanh dong ngan.
- Moi man chinh nen co cung cau truc: top bar, summary band, action row, content sections.

Component nen tao/chuan hoa:
- `AppSurfaceCard`: card dung chung cho item/card nho, radius 16dp hoac theo token.
- `DashboardSummaryCard`: card so lieu lon cho Personal/Split.
- `SectionHeader`: title + optional action.
- `QuickActionTile`: icon, title, subtitle.
- `MoneyText`: format tien dong nhat.
- `EmptyState`, `ErrorState`, `LoadingSkeleton`.
- `AppFloatingActionButton`: FAB dung chung, khong hard-code offset theo screen.

### 3.3 De xuat visual cho Personal

Personal Home:
- Balance card thanh "monthly command center": balance, income, expense, safe-to-spend.
- Them horizontal insight cards ngay sau Balance.
- Quick actions chuyen thanh 4 o nho: Ledger, Categories, Reminders, Recurring.
- Chart nen co legend ro hon va empty state rieng khi chua co expense.
- Recent transactions nen co icon category + amount + source badge neu den tu Split.

Add Transaction:
- Nut chon Income/Expense dang segmented control.
- Amount input lon, auto format VND.
- Category picker co icon/color.
- Them optional source/receipt/wallet sau nay bang section collapsed.

History:
- Them filter chips: Month, Type, Category, Source.
- Group theo ngay/thang.
- Search trong transaction note/category.

Reminders/Goals:
- Tach "Reminders" va "Goals" hoac gom thanh "Plans".
- Reminder card co progress, threshold, next alert.

## 4. Thu tu trien khai de xuat

### Phase 1: UI Foundation

Muc tieu:
- Chuan hoa giao dien truoc khi them nhieu man moi.

Viec can lam:
- Viet hoa hoac thong nhat ngon ngu hien thi.
- Tao shared components cho card, section, money text, FAB.
- Doi bottom bar sang `MaterialTheme.colorScheme`.
- Dong nhat radius/elevation/spacing.
- Profile doc real user data thay vi sample hard-code.

Rui ro: thap, chu yeu la refactor UI.

### Phase 2: Personal Intelligence MVP

Muc tieu:
- Them gia tri moi ro rang ma it thay doi data model.

Viec can lam:
- Smart Monthly Insights tinh tu transactions hien tai.
- Safe-to-Spend Forecast ban dau.
- UI insight cards tren Personal.

Rui ro: thap den trung binh.

### Phase 3: Split-to-Personal Bridge

Muc tieu:
- Ket noi Split va Personal thanh mot trai nghiem lien mach.

Viec can lam:
- Them source fields cho transaction.
- Update Firestore rules va mapper.
- Bottom sheet goi y ghi transaction sau khi tao/settle bill.

Rui ro: trung binh vi dung data model va rules.

### Phase 4: Recurring, Goals, Receipt

Muc tieu:
- Mo rong thanh quan ly tai chinh ca nhan day du hon.

Viec can lam:
- Recurring rules.
- Personal goals/challenges.
- Receipt capture.

Rui ro: trung binh den cao, tuy pham vi OCR/storage.

## 5. Data model goi y

```text
user_personal/{uid}/transactions/{transactionId}
  existing fields...
  source?: MANUAL | SPLIT | RECURRING | RECEIPT
  sourceGroupId?: string
  sourceBillId?: string
  recurringRuleId?: string
  receiptImageUrl?: string
  walletId?: string

user_personal/{uid}/recurring_rules/{ruleId}
  id, userId, name, amount, type, categoryId, categoryName
  cadence, dayOfMonth, nextRunAt, isEnabled, createdAt, updatedAt

user_personal/{uid}/goals/{goalId}
  id, userId, title, targetAmount, currentAmount
  categoryId?, deadlineAt, status, createdAt, updatedAt

user_personal/{uid}/wallets/{walletId}
  id, userId, name, type, balance, color, isArchived, createdAt, updatedAt
```

Ghi chu quan trong:
- Firestore rules hien validate strict bang `keys().hasOnly(...)`, nen moi field moi phai update rule, repository mapper, va test cung luc.
- Neu muon deploy nhanh, co the lam insight/safe-to-spend truoc vi khong can them collection.

## 6. Definition of Done cho dot UI va Personal moi

- Personal, Split, Profile dung cung theme color, radius, spacing, bottom bar.
- Khong con text sample trong Profile cua user that.
- Money format thong nhat: mot style cho full amount, mot style cho compact amount.
- Cac man chinh co loading, empty, error, success feedback ro rang.
- Personal co it nhat 2 feature moi trong MVP: Smart Monthly Insights va Safe-to-Spend.
- Firestore rules deploy thanh cong va logcat khong co `PERMISSION_DENIED` cho luong Personal/Split chinh.
- Build `:app:compileDebugKotlin` pass.

## 7. Khuyen nghi chon MVP

Nen lam theo thu tu:
1. UI Foundation pass.
2. Smart Monthly Insights.
3. Safe-to-Spend Forecast.
4. Split-to-Personal Bridge.
5. Recurring Expense Radar.

Ly do:
- Hai tinh nang dau tao cam giac moi ngay tren Personal ma it dung backend.
- Split-to-Personal Bridge la tinh nang doc dao nhat cua DineSplit, nhung can sua data model/rules can than.
- Recurring va Receipt rat hay, nhung nen de sau khi UI va data contract da vung.
