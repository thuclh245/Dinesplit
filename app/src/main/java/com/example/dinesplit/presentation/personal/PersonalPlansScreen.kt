package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
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

@Composable
fun PersonalPlansScreen(
    onBack: () -> Unit,
    categories: List<StoredCategory>,
    recurringRules: List<RecurringRule>,
    goals: List<PersonalGoal>,
    wallets: List<PersonalWallet>,
    onAddRecurring: (String, Double, TransactionType, String, String, RecurringCadence, Int) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    onAddGoal: (String, Double, Double, String?) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onAddWallet: (String, WalletType, Double) -> Unit,
    onDeleteWallet: (String) -> Unit
) {
    AppScaffold(
        title = "Personal Plans",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Text(
                text = "Automate, save, and organize.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            RecurringPlanSection(
                categories = categories,
                rules = recurringRules,
                onAdd = onAddRecurring,
                onDelete = onDeleteRecurring
            )
            GoalPlanSection(
                goals = goals,
                onAdd = onAddGoal,
                onDelete = onDeleteGoal
            )
            WalletPlanSection(
                wallets = wallets,
                onAdd = onAddWallet,
                onDelete = onDeleteWallet
            )
        }
    }
}

@Composable
private fun RecurringPlanSection(
    categories: List<StoredCategory>,
    rules: List<RecurringRule>,
    onAdd: (String, Double, TransactionType, String, String, RecurringCadence, Int) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("1") }
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    val category = categories.firstOrNull { it.type == type }

    PlanSectionCard(
        icon = Icons.Default.Repeat,
        title = "Recurring radar",
        subtitle = "Track fixed bills and income before they hit."
    ) {
        TypeChips(selectedType = type, onTypeSelected = { type = it })
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { value -> if (value.all { it.isDigit() }) amount = value },
            label = { Text("Amount") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = day,
            onValueChange = { value -> if (value.all { it.isDigit() }) day = value.take(2) },
            label = { Text("Day of month") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
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
                        day.toIntOrNull() ?: 1
                    )
                    name = ""
                    amount = ""
                    day = "1"
                }
            }
        )

        PlanList(
            emptyTitle = "No recurring rules yet",
            items = rules,
            itemTitle = { it.name },
            itemSubtitle = { "${it.categoryName} - ${formatMoney(it.amount)} - day ${it.dayOfMonth}" },
            onDelete = { onDelete(it.id) }
        )
    }
}

@Composable
private fun GoalPlanSection(
    goals: List<PersonalGoal>,
    onAdd: (String, Double, Double, String?) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var current by rememberSaveable { mutableStateOf("") }

    PlanSectionCard(
        icon = Icons.Default.Flag,
        title = "Goals and challenges",
        subtitle = "Turn budgets into progress you can see."
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Goal title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = target,
            onValueChange = { value -> if (value.all { it.isDigit() }) target = value },
            label = { Text("Target amount") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = current,
            onValueChange = { value -> if (value.all { it.isDigit() }) current = value },
            label = { Text("Current amount") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
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
            }
        )

        PlanList(
            emptyTitle = "No goals yet",
            items = goals,
            itemTitle = { it.title },
            itemSubtitle = {
                "${formatMoney(it.currentAmount)} of ${formatMoney(it.targetAmount)} by ${formatDate(it.deadlineAt)}"
            },
            onDelete = { onDelete(it.id) }
        )
    }
}

@Composable
private fun WalletPlanSection(
    wallets: List<PersonalWallet>,
    onAdd: (String, WalletType, Double) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var balance by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(WalletType.CASH) }

    PlanSectionCard(
        icon = Icons.Default.AccountBalanceWallet,
        title = "Wallets",
        subtitle = "Separate cash, bank, e-wallet, and credit balances."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
            WalletType.entries.forEach { walletType ->
                FilterChip(
                    selected = type == walletType,
                    onClick = { type = walletType },
                    label = { Text(walletType.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Wallet name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = balance,
            onValueChange = { value -> if (value.all { it.isDigit() }) balance = value },
            label = { Text("Starting balance") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
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
            }
        )

        PlanList(
            emptyTitle = "No wallets yet",
            items = wallets,
            itemTitle = { it.name },
            itemSubtitle = { "${it.type.name.lowercase().replaceFirstChar { char -> char.uppercase() }} - ${formatMoney(it.balance)}" },
            onDelete = { onDelete(it.id) }
        )
    }
}

@Composable
private fun PlanSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
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
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        TransactionType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
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
    onDelete: (T) -> Unit
) {
    if (items.isEmpty()) {
        Text(
            text = emptyTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        items.forEach { item ->
            AppCard(contentPadding = PaddingValues(AppDimens.spaceMd)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(itemTitle(item), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(itemSubtitle(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
