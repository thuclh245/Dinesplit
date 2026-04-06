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
        listOf(
            ManagedCategory("c_food", "Food", "FD", CategoryTypeFilter.EXPENSE, false),
            ManagedCategory("c_drink", "Drink", "DR", CategoryTypeFilter.EXPENSE, false),
            ManagedCategory("c_travel", "Travel", "TR", CategoryTypeFilter.EXPENSE, false),
            ManagedCategory("c_salary", "Salary", "SL", CategoryTypeFilter.INCOME, false),
            ManagedCategory("c_bonus", "Bonus", "BN", CategoryTypeFilter.INCOME, false)
        )
    }

    var selectedType by remember { mutableStateOf(CategoryTypeFilter.EXPENSE) }

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
            Text(
                text = "Basic category library used by Add Transaction. Week 2 keeps this screen simple: choose a type and browse sample categories.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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

