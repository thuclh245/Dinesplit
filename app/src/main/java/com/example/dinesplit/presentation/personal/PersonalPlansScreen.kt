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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.PrimaryButton
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
    OVERVIEW("overview", "Overview"),
    RECURRING("recurring", "Recurring"),
    GOALS("goals", "Goals"),
    WALLETS("wallets", "Wallets"),
    ;

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
        title = "Personal Plans",
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
                text = "Plan cockpit",
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
                goals = goals,
                onAdd = onAddGoal,
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
                        text = "Personal planning engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Recurring radar, goals, and wallets stay inside C scope.",
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
                    label = "Monthly fixed",
                    value = formatMoney(monthlyOutflow),
                )
                PlanMetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Wallet total",
                    value = formatMoney(walletTotal),
                )
            }
            PlanMetricPill(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Flag,
                label = "Goal progress",
                value = "${formatMoney(goalCurrent)} of ${formatMoney(goalTarget)}",
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
    val category = categories.firstOrNull { it.type == type }

    PlanSectionCard(
        icon = Icons.Default.Repeat,
        title = "Recurring radar",
        subtitle = "Track fixed bills and income before they hit.",
    ) {
        TypeChips(selectedType = type, onTypeSelected = { type = it })
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { value -> if (value.all { it.isDigit() }) amount = value },
            label = { Text("Amount") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = day,
            onValueChange = { value -> if (value.all { it.isDigit() }) day = value.take(2) },
            label = { Text("Day of month") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = "Add recurring rule",
            onClick = {
                val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && parsedAmount > 0.0 && category != null) {
                    onAdd(
                        name,
                        parsedAmount,
                        type,
                        category.id,
                        category.name,
                        RecurringCadence.MONTHLY,
                        day.toIntOrNull() ?: 1,
                    )
                    name = ""
                    amount = ""
                    day = "1"
                }
            },
        )

        PlanList(
            emptyTitle = "No recurring rules yet",
            items = rules,
            itemTitle = { it.name },
            itemSubtitle = { "${it.categoryName} - ${formatMoney(it.amount)} - day ${it.dayOfMonth}" },
            onDelete = { onDelete(it.id) },
        )
    }
}

@Composable
private fun GoalPlanSection(
    goals: List<PersonalGoal>,
    onAdd: (String, Double, Double, String?) -> Unit,
    onDelete: (String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var current by rememberSaveable { mutableStateOf("") }

    PlanSectionCard(
        icon = Icons.Default.Flag,
        title = "Goals and challenges",
        subtitle = "Turn budgets into progress you can see.",
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Goal title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = target,
            onValueChange = { value -> if (value.all { it.isDigit() }) target = value },
            label = { Text("Target amount") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = current,
            onValueChange = { value -> if (value.all { it.isDigit() }) current = value },
            label = { Text("Current amount") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = "Add goal",
            onClick = {
                val parsedTarget = target.toDoubleOrNull() ?: 0.0
                val parsedCurrent = current.toDoubleOrNull() ?: 0.0
                if (title.isNotBlank() && parsedTarget > 0.0) {
                    onAdd(title, parsedTarget, parsedCurrent, null)
                    title = ""
                    target = ""
                    current = ""
                }
            },
        )

        GoalProgressList(
            goals = goals,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun GoalProgressList(
    goals: List<PersonalGoal>,
    onDelete: (String) -> Unit,
) {
    if (goals.isEmpty()) {
        Text(
            text = "No goals yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        goals.forEach { goal ->
            val progress =
                if (goal.targetAmount > 0.0) {
                    (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }

            AppCard(contentPadding = PaddingValues(AppDimens.spaceMd)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                    ) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            text = "${formatMoney(
                                goal.currentAmount,
                            )} of ${formatMoney(goal.targetAmount)} by ${formatDate(goal.deadlineAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onDelete(goal.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
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
        title = "Wallets",
        subtitle = "Separate cash, bank, e-wallet, and credit balances.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
            WalletType.entries.forEach { walletType ->
                FilterChip(
                    selected = type == walletType,
                    onClick = { type = walletType },
                    label = { Text(walletType.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Wallet name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = balance,
            onValueChange = { value -> if (value.all { it.isDigit() }) balance = value },
            label = { Text("Starting balance") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = "Add wallet",
            onClick = {
                val parsedBalance = balance.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) {
                    onAdd(name, type, parsedBalance)
                    name = ""
                    balance = ""
                }
            },
        )

        PlanList(
            emptyTitle = "No wallets yet",
            items = wallets,
            itemTitle = { it.name },
            itemSubtitle = { "${it.type.name.lowercase().replaceFirstChar { char -> char.uppercase() }} - ${formatMoney(it.balance)}" },
            onDelete = { onDelete(it.id) },
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
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        TransactionType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
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
                        Text(itemTitle(item), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            itemSubtitle(item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDelete(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "No deadline"
    return SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(epochMillis))
}
