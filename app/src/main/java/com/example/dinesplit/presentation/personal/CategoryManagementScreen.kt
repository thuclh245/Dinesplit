package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.ui.theme.DineSplitTheme

enum class CategoryTypeFilter(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income")
}

data class ManagedCategory(
    val id: String,
    val name: String,
    val icon: String,
    val type: CategoryTypeFilter,
    val isCustom: Boolean
)

@Composable
fun CategoryManagementScreen(
    onBack: () -> Unit
) {
    val categories = remember {
        mutableStateListOf(
            ManagedCategory("c_food", "Food", "FD", CategoryTypeFilter.EXPENSE, false),
            ManagedCategory("c_drink", "Drink", "DR", CategoryTypeFilter.EXPENSE, false),
            ManagedCategory("c_travel", "Travel", "TR", CategoryTypeFilter.EXPENSE, false),
            ManagedCategory("c_salary", "Salary", "SL", CategoryTypeFilter.INCOME, false),
            ManagedCategory("c_bonus", "Bonus", "BN", CategoryTypeFilter.INCOME, false)
        )
    }

    var selectedType by remember { mutableStateOf(CategoryTypeFilter.EXPENSE) }
    var nameInput by remember { mutableStateOf("") }
    var iconInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredCategories = categories.filter { it.type == selectedType }

    AppScaffold(
        title = "Category Management",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                CategoryTypeFilter.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.label) }
                    )
                }
            }

            AppCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    Text(
                        text = "Add custom category",
                        style = MaterialTheme.typography.titleMedium
                    )

                    AppTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            errorMessage = null
                        },
                        label = "Category name",
                        placeholder = "Ex: Snacks"
                    )

                    AppTextField(
                        value = iconInput,
                        onValueChange = { iconInput = it.uppercase() },
                        label = "Icon (optional)",
                        placeholder = "Ex: SN"
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    PrimaryButton(
                        text = "Add Category",
                        onClick = {
                            val normalizedName = nameInput.trim()
                            if (normalizedName.isBlank()) {
                                errorMessage = "Category name is required"
                                return@PrimaryButton
                            }

                            val duplicated = categories.any {
                                it.type == selectedType && it.name.equals(normalizedName, ignoreCase = true)
                            }
                            if (duplicated) {
                                errorMessage = "Category already exists"
                                return@PrimaryButton
                            }

                            val icon = iconInput.trim().takeIf { it.isNotBlank() }
                                ?: normalizedName.take(2).uppercase()

                            categories.add(
                                ManagedCategory(
                                    id = "custom_${System.currentTimeMillis()}",
                                    name = normalizedName,
                                    icon = icon,
                                    type = selectedType,
                                    isCustom = true
                                )
                            )

                            nameInput = ""
                            iconInput = ""
                            errorMessage = null
                        },
                        enabled = nameInput.isNotBlank()
                    )
                }
            }

            Text(
                text = "${selectedType.label} categories",
                style = MaterialTheme.typography.titleSmall
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                items(
                    items = filteredCategories,
                    key = { it.id }
                ) { category ->
                    CategoryItemRow(category = category)
                }
            }
        }
    }
}

@Composable
private fun CategoryItemRow(
    category: ManagedCategory
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = if (category.isCustom) "Custom" else "Default",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CategoryManagementScreenPreview() {
    DineSplitTheme {
        CategoryManagementScreen(onBack = {})
    }
}

