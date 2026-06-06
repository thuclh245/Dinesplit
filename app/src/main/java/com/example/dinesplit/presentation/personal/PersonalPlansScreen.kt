package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppShapes
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.PersonalGoal
import com.example.dinesplit.domain.model.PersonalWallet
import com.example.dinesplit.domain.model.RecurringCadence
import com.example.dinesplit.domain.model.RecurringRule
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.model.WalletType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PersonalPlanFocus(val routeValue: String, val label: String) {
    OVERVIEW("overview", "Tổng quan"),
    RECURRING("recurring", "Lặp lại"),
    GOALS("goals", "Mục tiêu"),
    WALLETS("wallets", "Ví");

    companion object {
        fun fromRouteValue(value: String?): PersonalPlanFocus {
            return values().firstOrNull { it.routeValue == value } ?: OVERVIEW
        }
    }
}

@Composable
fun PersonalPlansScreen(
    onBack: () -> Unit,
    initialFocus: PersonalPlanFocus = PersonalPlanFocus.OVERVIEW,
    categories: List<StoredCategory>,
    recurringRules: List<RecurringRule>,
    goals: List<PersonalGoal>,
    wallets: List<PersonalWallet>,
    onAddRecurring: (String, Double, TransactionType, String, String, RecurringCadence, Int) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    onAddGoal: (String, Double, Double, String?) -> Unit,
    onUpdateGoal: (String, String, Double, Double, String?) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onAddWallet: (String, WalletType, Double) -> Unit,
    onDeleteWallet: (String) -> Unit,
) {
    var selectedFocusRoute by rememberSaveable(initialFocus.routeValue) {
        mutableStateOf(initialFocus.routeValue)
    }
    val selectedFocus =
        remember(selectedFocusRoute) {
            PersonalPlanFocus.fromRouteValue(selectedFocusRoute)
        }
    val orderedSections =
        remember(selectedFocus) {
            val sections =
                listOf(
                    PersonalPlanFocus.RECURRING,
                    PersonalPlanFocus.GOALS,
                    PersonalPlanFocus.WALLETS,
                )
            if (selectedFocus == PersonalPlanFocus.OVERVIEW) {
                sections
            } else {
                listOf(selectedFocus) + sections.filterNot { it == selectedFocus }
            }
        }

    AppScaffold(
        title = "Kế hoạch Ví",
        navigationIcon = {
            BackNavigationButton(onClick = onBack)
        },
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            Text(
                text = "Bảng điều khiển kế hoạch",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )

            PlanFocusChips(
                selectedFocus = selectedFocus,
                onFocusSelected = { selectedFocusRoute = it.routeValue },
            )

            if (selectedFocus == PersonalPlanFocus.OVERVIEW) {
                PlanCockpitCard(
                    recurringRules = recurringRules,
                    goals = goals,
                    wallets = wallets,
                )
            }

            orderedSections.forEach { focus ->
                key(focus) {
                    PlanFocusSection(
                        focus = focus,
                        categories = categories,
                        recurringRules = recurringRules,
                        goals = goals,
                        wallets = wallets,
                        onAddRecurring = onAddRecurring,
                        onDeleteRecurring = onDeleteRecurring,
                        onAddGoal = onAddGoal,
                        onUpdateGoal = onUpdateGoal,
                        onDeleteGoal = onDeleteGoal,
                        onAddWallet = onAddWallet,
                        onDeleteWallet = onDeleteWallet,
                    )
                }
            }

            if (selectedFocus != PersonalPlanFocus.OVERVIEW) {
                PlanCockpitCard(
                    recurringRules = recurringRules,
                    goals = goals,
                    wallets = wallets,
                )
            }
        }
    }
}

@Composable
private fun PlanFocusChips(
    selectedFocus: PersonalPlanFocus,
    onFocusSelected: (PersonalPlanFocus) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        PersonalPlanFocus.values().forEach { focus ->
            FilterChip(
                selected = selectedFocus == focus,
                onClick = { onFocusSelected(focus) },
                label = { Text(focus.label) },
            )
        }
    }
}

@Composable
private fun PlanFocusSection(
    focus: PersonalPlanFocus,
    categories: List<StoredCategory>,
    recurringRules: List<RecurringRule>,
    goals: List<PersonalGoal>,
    wallets: List<PersonalWallet>,
    onAddRecurring: (String, Double, TransactionType, String, String, RecurringCadence, Int) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    onAddGoal: (String, Double, Double, String?) -> Unit,
    onUpdateGoal: (String, String, Double, Double, String?) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onAddWallet: (String, WalletType, Double) -> Unit,
    onDeleteWallet: (String) -> Unit,
) {
    when (focus) {
        PersonalPlanFocus.OVERVIEW -> Unit
        PersonalPlanFocus.RECURRING ->
            RecurringPlanSection(
                categories = categories,
                rules = recurringRules,
                onAdd = onAddRecurring,
                onDelete = onDeleteRecurring,
            )
        PersonalPlanFocus.GOALS ->
            GoalPlanSection(
                categories = categories,
                goals = goals,
                onAdd = onAddGoal,
                onUpdate = onUpdateGoal,
                onDelete = onDeleteGoal,
            )
        PersonalPlanFocus.WALLETS ->
            WalletPlanSection(
                wallets = wallets,
                onAdd = onAddWallet,
                onDelete = onDeleteWallet,
            )
    }
}

@Composable
private fun PlanCockpitCard(
    recurringRules: List<RecurringRule>,
    goals: List<PersonalGoal>,
    wallets: List<PersonalWallet>,
) {
    val monthlyOutflow =
        recurringRules
            .filter { it.isEnabled && it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    val goalTarget = goals.sumOf { it.targetAmount }
    val goalCurrent = goals.sumOf { it.currentAmount }
    val goalProgress =
        if (goalTarget > 0.0) {
            (goalCurrent / goalTarget).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
    val walletTotal = wallets.sumOf { it.balance }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .padding(AppDimens.spaceMd)
                                .size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Công cụ lập kế hoạch Ví",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Radar lặp lại, mục tiêu và ví vẫn ở trong phạm vi C.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { goalProgress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                PlanMetricPill(
                     modifier = Modifier.weight(1f),
                     icon = Icons.Default.Repeat,
                     label = "Cố định hàng tháng",
                     value = formatMoney(monthlyOutflow)
                 )
                 PlanMetricPill(
                     modifier = Modifier.weight(1f),
                     icon = Icons.Default.AccountBalanceWallet,
                     label = "Tổng ví",
                     value = formatMoney(walletTotal)
                 )
            }
            PlanMetricPill(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Flag,
                label = "Tiến độ mục tiêu",
                value = "${formatMoney(goalCurrent)} của ${formatMoney(goalTarget)}"
            )
        }
    }
}

@Composable
private fun PlanMetricPill(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RecurringPlanSection(
    categories: List<StoredCategory>,
    rules: List<RecurringRule>,
    onAdd: (String, Double, TransactionType, String, String, RecurringCadence, Int) -> Unit,
    onDelete: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("1") }
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategoryId by rememberSaveable { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val categoryOptions =
        remember(categories, type) {
            categories
                .filter { it.type == type }
                .sortedBy { it.name.lowercase() }
        }
    val selectedCategory =
        categoryOptions.firstOrNull { it.id == selectedCategoryId }

    PlanSectionCard(
         icon = Icons.Default.Repeat,
         title = "Radar lặp lại",
         subtitle = "Theo dõi các hóa đơn cố định và thu nhập trước khi chúng được nhập."
     ) {
         TypeChips(selectedType = type, onTypeSelected = { type = it })
         CategorySelector(
             categories = categoryOptions,
             selectedCategory = selectedCategory,
             expanded = showCategoryDropdown,
             onExpandedChange = { showCategoryDropdown = it },
             onCategorySelected = { category ->
                 selectedCategoryId = category.id
                 validationMessage = null
             },
         )
         AppTextField(
             value = name,
             onValueChange = { name = it },
             label = "Tên",
             modifier = Modifier.fillMaxWidth()
         )
         AppTextField(
             value = amount,
             onValueChange = { value -> if (value.all { it.isDigit() }) amount = value },
             label = "Số tiền",
             modifier = Modifier.fillMaxWidth()
         )
         AppTextField(
             value = day,
             onValueChange = { value -> if (value.all { it.isDigit() }) day = value.take(2) },
             label = "Ngày trong tháng",
             modifier = Modifier.fillMaxWidth()
         )
         validationMessage?.let { message ->
             Text(
                 text = message,
                 style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.error,
             )
         }
         PrimaryButton(
             text = "Thêm quy tắc lặp lại",
             onClick = {
                 val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                 val category = selectedCategory
                 when {
                     name.isBlank() -> validationMessage = "Nhập tên khoản lặp lại."
                     parsedAmount <= 0.0 -> validationMessage = "Số tiền phải lớn hơn 0."
                     category == null -> validationMessage = "Chọn danh mục cho khoản lặp lại."
                     else -> {
                         validationMessage = null
                         onAdd(
                             name,
                             parsedAmount,
                             type,
                             category.id,
                             category.name,
                             RecurringCadence.MONTHLY,
                             day.toIntOrNull() ?: 1
                         )
                         name = ""
                         amount = ""
                         day = "1"
                         selectedCategoryId = ""
                     }
                 }
             }
         )

         PlanList(
             emptyTitle = "Chưa có quy tắc lặp lại nào",
             items = rules,
             itemTitle = { it.name },
             itemSubtitle = { "${it.categoryName} - ${formatMoney(it.amount)} - ngày ${it.dayOfMonth}" },
             onDelete = { onDelete(it.id) }
         )
     }
}

@Composable
private fun CategorySelector(
    categories: List<StoredCategory>,
    selectedCategory: StoredCategory?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCategorySelected: (StoredCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        Text("Danh mục", style = MaterialTheme.typography.labelSmall)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = AppShapes.medium,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.spaceMd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        when {
                            selectedCategory != null -> selectedCategory.name
                            categories.isEmpty() -> "Chưa có danh mục cho loại này"
                            else -> "Chọn danh mục"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    enabled = categories.isNotEmpty(),
                    onClick = { onExpandedChange(!expanded) },
                ) {
                    Text("Chọn")
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun OptionalCategorySelector(
    categories: List<StoredCategory>,
    selectedCategory: StoredCategory?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCategorySelected: (StoredCategory?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        Text("Danh mục liên kết", style = MaterialTheme.typography.labelSmall)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = AppShapes.medium,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.spaceMd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedCategory?.name ?: "Không liên kết",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onExpandedChange(!expanded) }) {
                    Text("Chọn")
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            DropdownMenuItem(
                text = { Text("Không liên kết") },
                onClick = {
                    onCategorySelected(null)
                    onExpandedChange(false)
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${category.name} - ${category.type.displayLabel()}") },
                    onClick = {
                        onCategorySelected(category)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun GoalPlanSection(
    categories: List<StoredCategory>,
    goals: List<PersonalGoal>,
    onAdd: (String, Double, Double, String?) -> Unit,
    onUpdate: (String, String, Double, Double, String?) -> Unit,
    onDelete: (String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var current by rememberSaveable { mutableStateOf("") }
    var editingGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategoryId by rememberSaveable { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val categoryOptions =
        remember(categories) {
            categories.sortedWith(
                compareBy<StoredCategory> { it.type.name }
                    .thenBy { it.name.lowercase() },
            )
        }
    val selectedCategory = categoryOptions.firstOrNull { it.id == selectedCategoryId }
    val isEditing = editingGoalId != null

    fun resetGoalForm() {
        editingGoalId = null
        title = ""
        target = ""
        current = ""
        selectedCategoryId = ""
        validationMessage = null
    }

    PlanSectionCard(
         icon = Icons.Default.Flag,
         title = "Mục tiêu và thử thách",
         subtitle = "Biến ngân sách thành tiến độ mà bạn có thể nhìn thấy."
     ) {
         AppTextField(
             value = title,
             onValueChange = { title = it },
             label = "Tiêu đề mục tiêu",
             modifier = Modifier.fillMaxWidth()
         )
         AppTextField(
             value = target,
             onValueChange = { value -> if (value.all { it.isDigit() }) target = value },
             label = "Số tiền mục tiêu",
             modifier = Modifier.fillMaxWidth()
         )
         AppTextField(
             value = current,
             onValueChange = { value -> if (value.all { it.isDigit() }) current = value },
             label = "Số tiền hiện tại",
             modifier = Modifier.fillMaxWidth()
         )
         OptionalCategorySelector(
             categories = categoryOptions,
             selectedCategory = selectedCategory,
             expanded = showCategoryDropdown,
             onExpandedChange = { showCategoryDropdown = it },
             onCategorySelected = { category ->
                 selectedCategoryId = category?.id.orEmpty()
                 validationMessage = null
             },
         )
         validationMessage?.let { message ->
             Text(
                 text = message,
                 style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.error,
             )
         }
         PrimaryButton(
             text = if (isEditing) "Cập nhật mục tiêu" else "Thêm mục tiêu",
             onClick = {
                 val parsedTarget = target.toDoubleOrNull() ?: 0.0
                 val parsedCurrent = current.toDoubleOrNull() ?: 0.0
                 when {
                     title.isBlank() -> validationMessage = "Nhập tiêu đề mục tiêu."
                     parsedTarget <= 0.0 -> validationMessage = "Số tiền mục tiêu phải lớn hơn 0."
                     isEditing -> {
                         val goalId = editingGoalId ?: return@PrimaryButton
                         validationMessage = null
                         onUpdate(
                             goalId,
                             title,
                             parsedTarget,
                             parsedCurrent,
                             selectedCategoryId.takeIf { it.isNotBlank() },
                         )
                         resetGoalForm()
                     }
                     else -> {
                         validationMessage = null
                         onAdd(
                             title,
                             parsedTarget,
                             parsedCurrent,
                             selectedCategoryId.takeIf { it.isNotBlank() },
                         )
                         resetGoalForm()
                     }
                 }
             }
         )
         if (isEditing) {
             SecondaryButton(
                 text = "Hủy chỉnh sửa",
                 onClick = { resetGoalForm() },
             )
         }

        GoalProgressList(
            goals = goals,
            categories = categoryOptions,
            onEdit = { goal ->
                editingGoalId = goal.id
                title = goal.title
                target = formatAmountInput(goal.targetAmount)
                current = formatAmountInput(goal.currentAmount)
                selectedCategoryId = goal.categoryId.orEmpty()
                validationMessage = null
            },
            onDelete = { goalId ->
                if (editingGoalId == goalId) {
                    resetGoalForm()
                }
                onDelete(goalId)
            },
        )
    }
}

@Composable
private fun GoalProgressList(
    goals: List<PersonalGoal>,
    categories: List<StoredCategory>,
    onEdit: (PersonalGoal) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (goals.isEmpty()) {
         Text(
             text = "Chưa có mục tiêu nào",
             style = MaterialTheme.typography.bodyMedium,
             color = MaterialTheme.colorScheme.onSurfaceVariant
         )
         return
     }

    val categoryNamesById =
        remember(categories) {
            categories.associate { it.id to it.name }
        }

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        goals.forEach { goal ->
            val progress =
                if (goal.targetAmount > 0.0) {
                    (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
            val linkedCategoryName = goal.categoryId?.let { categoryNamesById[it] }

            AppCard(contentPadding = PaddingValues(AppDimens.spaceMd)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onEdit(goal) }) {
                                Text("Sửa")
                            }
                            IconButton(onClick = { onDelete(goal.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa")
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${formatMoney(goal.currentAmount)} / ${formatMoney(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    linkedCategoryName?.let { categoryName ->
                        Text(
                            text = "Danh mục: $categoryName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = "Hạn: ${formatDate(goal.deadlineAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletPlanSection(
    wallets: List<PersonalWallet>,
    onAdd: (String, WalletType, Double) -> Unit,
    onDelete: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var balance by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(WalletType.CASH) }

    PlanSectionCard(
         icon = Icons.Default.AccountBalanceWallet,
         title = "Ví",
         subtitle = "Tách biệt tiền mặt, ngân hàng, ví điện tử và số dư tín dụng."
      ) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
          ) {
              WalletType.entries.forEach { walletType ->
                  FilterChip(
                      selected = type == walletType,
                      onClick = { type = walletType },
                      label = { Text(walletType.displayLabel()) }
                  )
              }
          }
          AppTextField(
             value = name,
             onValueChange = { name = it },
             label = "Tên ví",
             modifier = Modifier.fillMaxWidth()
         )
         AppTextField(
             value = balance,
             onValueChange = { value -> if (value.all { it.isDigit() }) balance = value },
             label = "Số dư ban đầu",
             modifier = Modifier.fillMaxWidth()
         )
         PrimaryButton(
             text = "Thêm ví",
             onClick = {
                 val parsedBalance = balance.toDoubleOrNull() ?: 0.0
                 if (name.isNotBlank()) {
                     onAdd(name, type, parsedBalance)
                     name = ""
                     balance = ""
                 }
             }
         )

         PlanList(
             emptyTitle = "Chưa có ví nào",
             items = wallets,
             itemTitle = { it.name },
             itemSubtitle = { "${it.type.displayLabel()} - ${formatMoney(it.balance)}" },
             onDelete = { onDelete(it.id) }
         )
     }
}

@Composable
private fun PlanSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun TypeChips(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
    ) {
        TransactionType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayLabel()) }
            )
        }
    }
}

@Composable
private fun <T> PlanList(
    emptyTitle: String,
    items: List<T>,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String,
    onDelete: (T) -> Unit,
) {
    if (items.isEmpty()) {
        Text(
            text = emptyTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        items.forEach { item ->
            AppCard(contentPadding = PaddingValues(AppDimens.spaceMd)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            itemTitle(item),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            itemSubtitle(item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onDelete(item) }) {
                         Icon(Icons.Default.Delete, contentDescription = "Xóa")
                     }
                }
            }
        }
    }
}

private fun formatMoney(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(amount.toLong())} VND"
}

private fun formatAmountInput(amount: Double): String {
    val longAmount = amount.toLong()
    return if (amount == longAmount.toDouble()) {
        longAmount.toString()
    } else {
        amount.toString()
    }
}

private fun formatDate(epochMillis: Long): String {
     if (epochMillis <= 0L) return "Không có hạn chót"
     return SimpleDateFormat("dd MMM", Locale("vi", "VN")).format(Date(epochMillis))
 }
