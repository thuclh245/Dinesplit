# Final System Audit Summary - All Issues Found

**Date:** May 4, 2026  
**Scope:** Complete Dinesplit System Review

---

## 🔴 CRITICAL ISSUES (Must Fix for Production)

### Issue #1: userId Hardcoded as "user_1" in AddTransactionScreen

**Severity:** 🔴 CRITICAL  
**File:** `AddTransactionScreen.kt` line 226  
**Impact:** Breaks multi-user support, all users see same transactions

```kotlin
// ❌ CURRENT (BROKEN):
userId = "user_1"  // Hardcoded!

// ✅ FIX NEEDED:
// Get currentUserId from LocalAuthRepository.sessionFlow
// Or pass it as parameter from PersonalRoute
```

**How it currently "works":**
- Seed data: user_id = "user_1"
- New transactions: user_id = "user_1"
- Both show up together by accident! ✅

**Why it's broken:**
- After user registers with uid = "abc-123-xyz", their transactions still get "user_1"
- If multiple users logged in, they see each other's transactions
- No user isolation whatsoever

**Fix effort:** 30 minutes
```kotlin
// In PersonalRoute:
val personalViewModel = viewModel<PersonalViewModel>()
val currentUserId = ... // Get from AuthRepository

// Pass to AddTransactionScreen:
AddTransactionScreen(
    currentUserId = currentUserId,
    onSave = { transaction -> 
        // transaction.userId will be currentUserId
    }
)

// In PersonalRepository:
fun getTransactionsByUser(userId: String): List<Transaction> {
    return getAllTransactions().filter { it.userId == userId }
}
```

---

### Issue #2: Seed Transactions Not Linked to Real Users

**Severity:** 🔴 CRITICAL (demo-breaking)  
**File:** `PersonalDatabaseHelper.kt` line 113-118  
**Impact:** Seed data becomes orphaned after user registers

```kotlin
// ❌ CURRENT:
arrayOf<Any?>("tx_1", "user_1", 525000.0, ...),  // Orphaned user_id

// ✅ ALTERNATIVE FIXES:
// Option A: Use empty string ""
arrayOf<Any?>("tx_1", "", 525000.0, ...),

// Option B: Create system user first
// INSERT INTO accounts VALUES ("system-seed", "system@seed", hash)
// INSERT INTO users VALUES ("system-seed", "System", ...)
// Then use uid = "system-seed" in seed data

// Option C: Don't seed transactions, only categories
// Just seed categories, let user start fresh
```

**Why it matters for demo:**
- On first app run, no user exists
- Seed transactions are shown (OK for demo)
- After user registers, they have different uid
- If we filter by userId, seed disappears
- Demo shows empty history! ❌

**Recommended fix:** Don't seed transactions. Only seed categories. Let user create transactions.

---

## ⚠️ MAJOR ISSUES (Should Fix)

### Issue #3: No Current User Context in App

**Severity:** ⚠️ MAJOR  
**Impact:** Cannot properly filter transactions per user

**Current state:**
- PersonalViewModel has no awareness of current user
- PersonalRepository.getAllTransactions() returns ALL transactions (no filter)
- No getUserId() method anywhere in ViewModel layer

**Fix:**
```kotlin
// Add to PersonalViewModel:
private val _currentUserId = MutableStateFlow<String?>(null)

fun setCurrentUserId(userId: String) {
    _currentUserId.value = userId
}

// Change in refreshStateInternal():
val allTransactions = repository.getTransactionsByUser(_currentUserId.value)
```

**Effort:** 1-2 hours

---

### Issue #4: No Session Persistence

**Severity:** ⚠️ MAJOR  
**File:** `LocalAuthRepository.kt` line 19  

```kotlin
private val _sessionFlow = MutableStateFlow<UserSession?>(null)  // ← Always null on app start!
```

**Problem:**
- Session stored only in memory
- After app restart, user must re-login
- No `loadCurrentSession()` method like before

**How it should work:**
```kotlin
override fun loadCurrentSession(): UserSession? {
    val db = dbHelper.readableDatabase
    // Load last logged-in session from accounts table
    // Or use SharedPreferences for quick access
    return ...
}

init {
    _sessionFlow.value = loadCurrentSession()
}
```

**Effort:** 30-45 minutes

---

### Issue #5: TransactionDetailScreen Doesn't Verify User Owns Transaction

**Severity:** ⚠️ MAJOR  
**File:** `MainContainerScreen.kt` line 169-178

```kotlin
// Current: Just loads any transaction by ID
val transaction = personalRepo.getAllTransactions()
    .firstOrNull { it.id == transactionId }

// Should be:
val transaction = personalRepo.getAllTransactions()
    .firstOrNull { it.id == transactionId && it.userId == currentUserId }
    // Prevent user from viewing other users' transactions
```

**Effort:** 10 minutes

---

### Issue #6: Categories Not User-Specific

**Severity:** ⚠️ MAJOR  
**File:** `PersonalDatabaseHelper.kt` line 28-42 (schema)

```kotlin
// ❌ CURRENT: No user_id field
CREATE TABLE categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    ...
)

// ✅ SHOULD BE:
CREATE TABLE categories (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,  // ← Add this
    name TEXT NOT NULL,
    ...
    FOREIGN KEY(user_id) REFERENCES users(uid)
)
```

**Problem:**
- All users see same categories
- Can't have user-specific custom categories
- Current categories are "global"

**Impact for demo:**
- OK for single demo user
- Breaks with multiple users
- Seed categories should be user-specific (either global OR per-user)

**Effort:** 2-3 hours (requires migration, schema change)

---

## ⚠️ MODERATE ISSUES (Nice to Have)

### Issue #7: Notification System Completely Placeholder
(Already documented in ASSESSMENT_PERSON_C.md)
- NotificationScreen: Just AppPlaceholderScreen
- No notification model, no repository, no UI
- **Status:** 0% complete, but not blocking demo

### Issue #8: Spending Reminder Not Implemented
- No reminder model
- No UI screen
- No local notification logic
- **Status:** 0% complete, not blocking demo

### Issue #9: Profile Completion Flow

**Problem:**
- User registers → goes to CompleteProfileScreen
- CompleteProfileScreen sets displayName + username
- These are persisted to users table via LocalProfileRepository
- But no confirmation that CompleteProfileScreen actually runs

**Status:** Likely works, but untested

### Issue #10: Password Security

**Current:** SHA-256 (acceptable for demo)  
**Production:** Should use BCrypt or Argon2  
**Status:** Acceptable for current stage

---

## ✅ THINGS THAT ARE GOOD

| Component | Status | Notes |
|-----------|--------|-------|
| **Database Schema** | ✅ Clean | 4 tables properly structured |
| **Migrations** | ✅ Works | onUpgrade() handles version bumps |
| **Navigation** | ✅ Wired | All screens accessible, no dead code |
| **Data Persistence** | ✅ Working | SQLite properly initialized |
| **Personal Module UI** | ✅ Complete | Dashboard, history, categories all functional |
| **Form Validation** | ✅ Working | Transaction form validates inputs |
| **Architecture** | ✅ Clean | ViewModel/Repository pattern followed |

---

## 📊 Summary Scorecard

| Area | Score | Status |
|---|---|---|
| **Single-User Demo** | 85% | Works, some rough edges |
| **Multi-User Ready** | 20% | Broken (hardcoded user_id) |
| **Production Ready** | 40% | Too many user-isolation issues |
| **Navigation** | 95% | Complete and working |
| **Business Logic** | 60% | Works for single user, breaks multi-user |
| **UI Polish** | 80% | Looks good, mostly complete |
| **Database Design** | 70% | Schema OK, but missing user context columns |

---

## 🎯 Recommended Priorities

### 🔴 P0 (Must Fix for Multi-User)
1. Remove hardcoded userId = "user_1" → use authenticated user id
2. Add user context to PersonalViewModel
3. Filter transactions by current user
4. Add user_id to categories table

### 🟠 P1 (Fix Before Production)
5. Implement session persistence
6. Add transaction ownership verification
7. Implement Notification system (C's responsibility)
8. Add user ID to transaction queries

### 🟡 P2 (Nice to Have)
9. Implement spending reminders
10. Better auth state management
11. User ID context throughout app

---

## ✅ Current State for Demo

**This app WORKS as single-user demo because:**
1. ✅ User registers with random UUID
2. ✅ All transactions hardcoded as user_id = "user_1"
3. ✅ Seed data also uses user_id = "user_1"
4. ✅ By coincidence, everything matches!

**It BREAKS the moment:**
1. ❌ Second user registers
2. ❌ Try to filter by actual user ID
3. ❌ Add proper multi-user support

---

**Generated:** May 4, 2026  
**Next Review:** After P0 fixes implemented


