# 🛠️ HƯỚNG DẪN THỰC HÀNH: Cải Thiện UX DineSplit

**Mục đích:** Hướng dẫn code từng bước để implement các cải thiện UX  
**Đối tượng:** Developer Team  
**Thời gian:** 2-3 tuần

---

## 📌 PHẦN 1: TOAST NOTIFICATIONS (Quick Win #1)

### Vấn đề
Hiện tại app chưa có cơ chế thông báo (toast) khi user thực hiện hành động. Ví dụ:
- User tạo bill → không có feedback
- User logout → không có notification

### Giải Pháp

**Step 1: Tạo Toast State Management**

```kotlin
// File: app/src/main/java/com/example/dinesplit/core/ui/ToastState.kt

data class ToastState(
    val message: String = "",
    val type: ToastType = ToastType.INFO,
    val isVisible: Boolean = false,
    val durationMillis: Long = 3000L
)

enum class ToastType {
    SUCCESS,  // Màu xanh
    ERROR,    // Màu đỏ
    INFO,     // Màu xanh dương
    WARNING   // Màu cam
}

// ViewModel extension
fun MutableStateFlow<ToastState>.showToast(
    message: String,
    type: ToastType = ToastType.INFO,
    duration: Long = 3000L
) {
    this.value = ToastState(
        message = message,
        type = type,
        isVisible = true,
        durationMillis = duration
    )
}

fun MutableStateFlow<ToastState>.hideToast() {
    this.value = this.value.copy(isVisible = false)
}
```

**Step 2: Tạo Toast Composable**

```kotlin
// File: app/src/main/java/com/example/dinesplit/core/ui/ToastComponent.kt

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AppToast(
    state: ToastState,
    onDismiss: () -> Unit
) {
    LaunchedEffect(state.isVisible, state.durationMillis) {
        if (state.isVisible) {
            delay(state.durationMillis)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        val (bgColor, textColor, icon) = when (state.type) {
            ToastType.SUCCESS -> Triple(
                Color(0xFF00A58E),
                Color.White,
                Icons.Default.CheckCircle
            )
            ToastType.ERROR -> Triple(
                Color(0xFFBA1A1A),
                Color.White,
                Icons.Default.Error
            )
            ToastType.WARNING -> Triple(
                Color(0xFFE85D34),
                Color.White,
                Icons.Default.Warning
            )
            ToastType.INFO -> Triple(
                Color(0xFF1459C7),
                Color.White,
                Icons.Default.Info
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = state.message,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

**Step 3: Integration trong AppScaffold**

```kotlin
// Tìm và sửa file: app/src/main/java/com/example/dinesplit/core/ui/AppScaffold.kt

@Composable
fun AppScaffold(
    title: String,
    onBackClick: (() -> Unit)? = null,
    toastState: ToastState = ToastState(),
    onToastDismiss: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            if (onBackClick != null) {
                // Back button app bar
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            } else {
                // Normal app bar
                TopAppBar(title = { Text(title) })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            content(paddingValues)

            // Toast at bottom-center
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                AppToast(
                    state = toastState,
                    onDismiss = onToastDismiss
                )
            }
        }
    }
}
```

**Step 4: Usage Example**

```kotlin
// File: CreateBillScreen.kt

@Composable
fun CreateBillScreen(
    onBack: () -> Unit,
    viewModel: CreateBillViewModel = hiltViewModel()
) {
    val toastState by viewModel.toastState.collectAsState()

    AppScaffold(
        title = "Tạo hóa đơn",
        onBackClick = onBack,
        toastState = toastState,
        onToastDismiss = { viewModel.hideToast() }
    ) { paddingValues ->
        // Screen content
    }
}

// ViewModel code
class CreateBillViewModel : ViewModel() {
    private val _toastState = MutableStateFlow(ToastState())
    val toastState: StateFlow<ToastState> = _toastState.asStateFlow()

    fun createBill() {
        viewModelScope.launch {
            try {
                // Create bill logic
                _toastState.showToast(
                    "Hóa đơn được tạo thành công! ✅",
                    ToastType.SUCCESS
                )
            } catch (e: Exception) {
                _toastState.showToast(
                    "Lỗi: ${e.message}",
                    ToastType.ERROR
                )
            }
        }
    }
}
```

---

## 📌 PHẦN 2: EMPTY STATES (Quick Win #2)

### Vấn đề
Khi list rỗng, app chỉ hiển thị blank screen. Người dùng confused.

### Giải Pháp

**Tạo reusable Empty State Component**

```kotlin
// File: app/src/main/java/com/example/dinesplit/core/ui/EmptyStateComponent.kt

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = Color(0xFFCCCCCC)
        )

        // Title
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1C),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Subtitle
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color(0xFF56423E),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Action Button
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE85D34)
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Predefined empty states cho common cases
@Composable
fun EmptyGroupsState(onCreateGroup: () -> Unit) {
    AppEmptyState(
        icon = Icons.Default.Groups,
        title = "Chưa có nhóm nào",
        subtitle = "Tạo nhóm đầu tiên để chia sẻ hóa đơn với bạn bè",
        actionLabel = "Tạo Nhóm",
        onAction = onCreateGroup
    )
}

@Composable
fun EmptyBillsState(onCreateBill: () -> Unit) {
    AppEmptyState(
        icon = Icons.Default.Receipt,
        title = "Không có hóa đơn nào",
        subtitle = "Thêm hóa đơn đầu tiên của bạn",
        actionLabel = "Tạo Hóa Đơn",
        onAction = onCreateBill
    )
}

@Composable
fun ErrorLoadingState(onRetry: () -> Unit) {
    AppEmptyState(
        icon = Icons.Default.SyncProblem,
        title = "Lỗi tải dữ liệu",
        subtitle = "Không thể tải dữ liệu. Kiểm tra kết nối internet của bạn",
        actionLabel = "Thử Lại",
        onAction = onRetry
    )
}
```

**Usage Example:**

```kotlin
// File: GroupListScreen.kt

@Composable
fun GroupListScreen(
    uiState: GroupListUiState,
    onNavigateToCreateGroup: () -> Unit,
    onRetry: () -> Unit
) {
    when {
        uiState.isLoading -> {
            LoadingBlock("Đang tải nhóm...")
        }
        uiState.error != null -> {
            ErrorLoadingState(onRetry = onRetry)
        }
        uiState.groups.isEmpty() -> {
            EmptyGroupsState(onCreateGroup = onNavigateToCreateGroup)
        }
        else -> {
            LazyColumn {
                items(uiState.groups) { group ->
                    GroupCardItem(group = group)
                }
            }
        }
    }
}
```

---

## 📌 PHẦN 3: REAL-TIME INPUT VALIDATION (Quick Win #3)

### Vấn đề
- Email validation chỉ khi submit
- User chung ngơ có lỗi không
- Số tiền không format tự động

### Giải Pháp

**Tạo Validation Engine**

```kotlin
// File: app/src/main/java/com/example/dinesplit/core/validation/ValidationRules.kt

sealed class ValidationError {
    object EmptyField : ValidationError()
    object InvalidEmail : ValidationError()
    object PasswordTooShort : ValidationError()
    object PasswordNoUpperCase : ValidationError()
    data class CustomError(val message: String) : ValidationError()
}

object ValidationRules {
    fun validateEmail(email: String): ValidationError? {
        return when {
            email.isEmpty() -> ValidationError.EmptyField
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                ValidationError.InvalidEmail
            else -> null
        }
    }

    fun validatePassword(password: String): ValidationError? {
        return when {
            password.isEmpty() -> ValidationError.EmptyField
            password.length < 6 -> ValidationError.PasswordTooShort
            !password.any { it.isUpperCase() } -> ValidationError.PasswordNoUpperCase
            else -> null
        }
    }

    fun validateUsername(username: String): ValidationError? {
        return when {
            username.isEmpty() -> ValidationError.EmptyField
            username.length < 3 -> 
                ValidationError.CustomError("Username phải >= 3 ký tự")
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> 
                ValidationError.CustomError("Username chỉ chứa chữ, số, và _")
            else -> null
        }
    }

    fun formatCurrency(amount: String): String {
        val numeric = amount.filter { it.isDigit() }
        return if (numeric.isEmpty()) "" 
        else numeric.toLong().let { "%,d".format(it) }
    }
}
```

**Tạo ValidatedTextField Composable**

```kotlin
// File: app/src/main/java/com/example/dinesplit/core/ui/ValidatedTextField.kt

@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validator: (String) -> ValidationError?,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val error = if (isFocused || value.isNotEmpty()) validator(value) else null

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = leadingIcon,
            visualTransformation = if (isPassword) PasswordVisualTransformation() 
                                   else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = error != null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            colors = OutlinedTextFieldDefaults.colors(
                errorBorderColor = Color(0xFFBA1A1A),
                focusedBorderColor = Color(0xFFE85D34)
            )
        )

        // Error message
        if (error != null) {
            Text(
                text = when (error) {
                    ValidationError.EmptyField -> "Không được để trống"
                    ValidationError.InvalidEmail -> "Email không hợp lệ"
                    ValidationError.PasswordTooShort -> "Mật khẩu phải >= 6 ký tự"
                    ValidationError.PasswordNoUpperCase -> "Mật khẩu phải chứa chữ hoa"
                    is ValidationError.CustomError -> error.message
                },
                color = Color(0xFFBA1A1A),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp)
            )
        }
    }
}
```

**Usage Example:**

```kotlin
// File: LoginScreen.kt

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel()
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()

    Column(modifier = Modifier.padding(24.dp)) {
        ValidatedTextField(
            value = email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = "Email",
            validator = { ValidationRules.validateEmail(it) },
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        ValidatedTextField(
            value = password,
            onValueChange = { viewModel.onPasswordChange(it) },
            label = "Mật khẩu",
            validator = { ValidationRules.validatePassword(it) },
            isPassword = true
        )
    }
}
```

---

## 📌 PHẦN 4: LOADING SHIMMER (Optional but Nice)

### Vấn đề
Loading screens không visual, user không biết đã load bao nhiêu

### Giải Pháp

**Thêm dependency:**

```gradle
// build.gradle.kts (app module)
dependencies {
    implementation "com.valentinilk.shimmer:compose-shimmer:1.0.5"
}
```

**Tạo Shimmer Component:**

```kotlin
// File: app/src/main/java/com/example/dinesplit/core/ui/ShimmerLoading.kt

import com.valentinilk.shimmer.shimmer

@Composable
fun GroupCardShimmer(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .shimmer(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray.copy(alpha = 0.3f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun GroupListShimmerLoading() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(5) {
            item { GroupCardShimmer() }
        }
    }
}
```

---

## 🚀 IMPLEMENTATION PRIORITY

**Week 1:**
1. Toast Notifications (2h)
2. Empty States (2h)
3. Real-time Validation (3h)

**Week 2:**
4. Shimmer Loading (2h)
5. Accessibility improvements (3h)

**Week 3+:**
6. Animations
7. Onboarding Flow
8. Performance Optimization

---

## ✅ Testing Checklist

- [ ] Toast appears/disappears correctly
- [ ] Empty states show with correct copy
- [ ] Validation triggers on typing (not just submit)
- [ ] Error messages are clear
- [ ] All screens work with TalkBack
- [ ] Text is readable at 150% font scale
- [ ] Color contrast >= 4.5:1

---

## 📚 References

- [Material 3 Best Practices](https://m3.material.io/)
- [Android Accessibility Guidelines](https://developer.android.com/guide/topics/ui/accessibility)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose/documentation)

