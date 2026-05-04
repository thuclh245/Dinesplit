# System Audit Report - Comprehensive Analysis

**Date:** May 4, 2026  
**Status:** Full System Check  

---

## 🔍 AUDIT FINDINGS

### ✅ GOOD: Database Schema Consistency

| Table | Columns | Seeding | Notes |
|-------|---------|---------|-------|
| **accounts** | uid, email, password_hash | ❌ NO SEED | Only created on user registration |
| **users** | uid, email, display_name, username, avatar_url, bio, created_at, updated_at | ❌ NO SEED | Only created on user registration + default values |
| **transactions** | id, user_id, amount, type, category_id, category, note, date_millis, created_at | ✅ SEEDED | 5 sample transactions (user_id = "user_1") |
| **categories** | id, name, icon, type, is_custom, description, amount_label, progress, is_active | ✅ SEEDED | 8 categories (4 EXPENSE, 4 INCOME) |

**Assessment:** Schema is clean and consistent. ✅

---

## ⚠️ ISSUE #1: CRITICAL - Seed Data References Non-Existent User

### Problem:
- Database seeds **transactions** with `user_id = "user_1"` on initialization
- BUT there is **NO user account** with uid "user_1" in the **accounts** table
- User must first **register/login** to create their own uid

### Code Evidence:
```kotlin
// PersonalDatabaseHelper.kt line 113-117
val transactions: List<Array<Any?>> = listOf(
    arrayOf<Any?>("tx_1", "user_1", 525000.0, ...),  // ← user_id = "user_1"
    arrayOf<Any?>("tx_2", "user_1", 187500.0, ...),
    ...
)

// LocalAuthRepository.kt line 81
val uid = UUID.randomUUID().toString()  // ← New users get random UUID!
```

### Impact:
- Transactions in history belong to "user_1" (hardcoded)
- But logged-in user has different uid (generated UUID)
- **Result:** After user registers, seed transactions disappear from their history!

### Example Flow:
```
1. App starts, database created
2. seedTransactions() runs → 5 transactions with user_id = "user_1"
3. User registers → uid = "abc-123-def" (random UUID)
4. User opens Personal tab
5. PersonalRoute calls repository.getAllTransactions()
6. Repository returns all transactions (no user filter yet)
7. UI shows seed transactions ✅ (works by accident)
8. BUT if we add user_id filtering: ❌ BREAKS because user_id != "user_1"
```

### ⚠️ This Could Break If:
- Repository adds `WHERE user_id = ?` filter in getAllTransactions()
- PersonalViewModel filters transactions by currentUser
- Multi-user support is added

### Fix Options:
1. **Seed with null user_id** (transactions belong to no one, shared sample)
2. **Seed with empty user_id** ("")
3. **Don't seed transactions, only categories**
4. **Create system user in seed** (uid = "system-seed" or "user_demo")

---

## ⚠️ ISSUE #2: User Profile Not Synced on Registration

### Problem:
- **LocalAuthRepository** creates both `accounts` and `users` entries on register
- **LocalProfileRepository** reads from `users` table  
- BUT **AddTransactionScreen** + other screens don't load user context
- TransactionDetailScreen loads transaction but never loads user

### Code:
```kotlin
// LocalAuthRepository.kt line 94-108
val userValues = android.content.ContentValues().apply {
    put("uid", uid)
    put("email", normalizedEmail)
    put("display_name", "")      // ← EMPTY on register
    put("username", "")           // ← EMPTY on register
    put("avatar_url", "")
    put("bio", "")
    put("created_at", now)
    put("updated_at", now)
}
writeDb.insert("users", null, userValues)
```

**But then:**
- CompleteProfileScreen should fill in displayName + username
- No verification that CompleteProfileScreen actually saves to database

### Check:
```kotlin
// CompleteProfileScreen calls profileViewModel.saveProfile()
// Which calls: LocalProfileRepository.upsertProfile()
// Which does: UPDATE or INSERT into users table
// ✅ This should work IF CompleteProfileScreen is used
```

**Assessment:** Could work, but flow depends on completing profile. ⚠️

---

## ⚠️ ISSUE #3: Transaction Created with Hardcoded "user_1"

### Problem:
- When user adds transaction via AddTransactionScreen
- PersonalViewModel.addTransaction() creates Transaction(userId = ...?)

### Check Code:
Let me verify...AddTransactionScreen doesn't show userId input. So how is it set?

---

## ⚠️ ISSUE #4: No User Context Management

### Problem:
- App doesn't maintain "current user" anywhere visible
- PersonalViewModel loads ALL transactions (no user filter)
- No getTransactionsByUser(uid) in repository

### Code patterns show:
```kotlin
// PersonalViewModel.kt line 119
val allTransactions = repository.getAllTransactions()  // ← Gets ALL, no filter
```

**This works for single-user demo but breaks with multi-user!**

---

## ⚠️ ISSUE #5: TransactionType Enum Mismatch

### In Database Seed:
```kotlin
"type TEXT NOT NULL"  // Stores "EXPENSE" or "INCOME" as string
→ "EXPENSE", "INCOME"
```

### In Domain Model:
```kotlin
enum class TransactionType {
    EXPENSE, INCOME
}
```

### In Repository:
```kotlin
type = TransactionType.valueOf(it.getString(3))  // ✅ Correct
```

**Assessment:** This is OK because valueOf() converts "EXPENSE" → TransactionType.EXPENSE ✅

---

## ⚠️ ISSUE #6: Navigation Doesn't Validate User

### Problem:
- MainContainerScreen loads PersonalRoute without checking if user is logged in
- If user logs out, they can't navigate to Personal (will crash trying to access data)
- No Session validation in Personal screens

### Evidence:
```kotlin
// MainContainerScreen.kt
// No check like: if (sessionState.uid == null) { return }
// Directly shows PersonalRoute
```

**Assessment:** Could crash if user logs out while viewing Personal. ⚠️

---

## ⚠️ ISSUE #7: Categories Seeded Always, But Not Linked to User

### Problem:
- Categories are seeded globally, not per-user
- All users see same categories
- But custom categories are user-specific (???)

### Schema:
```kotlin
CREATE TABLE categories (
    id TEXT PRIMARY KEY,      // ← No user_id!
    ...
)
```

**Issue:** If multi-user, how do we distinguish user's custom categories?

**For demo:** OK because single user. But architectural issue.

---

## ✅ GOOD: Data Persistence Pattern

- SQLite initialized properly
- Seed data loads only on first app start
- DATABASE_VERSION = 3 handles upgrades
- onUpgrade() drops and recreates (safe for demo)

---

## ⚠️ ISSUE #8: Notification System Completely Placeholder

(Already documented in ASSESSMENT_PERSON_C.md)

---

## 🔴 CRITICAL REVIEW: Does It Actually Work?

Let me trace the **real user flow:**

### Flow 1: First Time User (Demo)
```
1. App starts → SplashScreen
2. Database created, seed data inserted:
   - Categories: 8 items (shared)
   - Transactions: 5 items with user_id = "user_1"
3. User has no session yet → goes to LoginScreen
4. User clicks "Register" → RegisterScreen
5. User enters email + password
6. LocalAuthRepository.register():
   - uid = UUID.randomUUID() (e.g., "abc-123")
   - Creates accounts table entry
   - Creates users table entry (empty displayName/username)
7. Goes to CompleteProfileScreen
8. User enters displayName + username
9. Saved to users table via LocalProfileRepository.upsertProfile()
10. Goes to MainContainerScreen
11. User clicks Personal tab
12. PersonalRoute loads:
    - Gets all 5 seed transactions
    - Gets all 8 categories
    - Renders dashboard
    
RESULT: ✅ WORKS! Shows 5 seed transactions (user_id = "user_1" matches seed)
```

### Flow 2: User Adds Own Transaction (Breaking Scenario?)
```
1. User on AddTransactionScreen
2. User fills: amount, type, category, note
3. Clicks "Save"
4. PersonalViewModel.addTransaction(transaction)
5. Creates Transaction(id, userId = ???, amount, type, ...)

WAIT: Where is userId set? Let me check...
```

Let me find where AddTransaction creates Transaction:

---

---

### CRITICAL FINDING: userId HARDCODED as "user_1"

**Location:** AddTransactionScreen.kt line 226

```kotlin
onSave(
    validInput.toTransaction(
        id = UUID.randomUUID().toString(),
        userId = "user_1"  // ← ⚠️ HARDCODED!
    )
)
```

**ALL transactions created by ANY user get userId = "user_1"!**

This is why the system "works":
- Seed transactions have user_id = "user_1"
- User-created transactions get user_id = "user_1"
- Both show up together! ✅ Works by accident

But this is BROKEN architecture:
- Multi-user not possible
- All users see same transactions
- No user isolation

**Impact:** 🔴 CRITICAL - Prevents multi-user support

---


