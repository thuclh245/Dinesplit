package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dinesplit.core.ui.AppCard
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.ui.theme.DineSplitTheme
import kotlinx.coroutines.launch

enum class CategoryTypeFilter(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income")
}

data class ManagedCategory(
    val id: String,
    val name: String,
    val icon: String,
    val type: CategoryTypeFilter,
    val isCustom: Boolean,
    val description: String,
    val amountLabel: String,
    val progress: Float,
    val isActive: Boolean
)

data class CategoryEditorInput(
    val name: String,
    val description: String,
    val isCustom: Boolean,
    val type: CategoryTypeFilter
)

@Composable
fun CategoryManagementScreen(
    categories: List<ManagedCategory> = previewManagedCategories(),
    usedCategoryIds: Set<String> = emptySet(),
    onAddCategory: (CategoryEditorInput) -> Unit = {},
    onUpdateCategory: (ManagedCategory, CategoryEditorInput) -> Unit = { _, _ -> },
    onDeleteCategory: (ManagedCategory) -> Unit = {},
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    var selectedType by remember { mutableStateOf(CategoryTypeFilter.EXPENSE) }
    var creatingType by rememberSaveable { mutableStateOf(CategoryTypeFilter.EXPENSE.name) }
    var editingCategory by remember { mutableStateOf<ManagedCategory?>(null) }
    var deletingCategory by remember { mutableStateOf<ManagedCategory?>(null) }
    var isCreateDialogOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }
    val featuredCategory = filteredCategories.firstOrNull()
    val sideCategories = filteredCategories.drop(1)

    AppScaffold(
        title = "Category Management",
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            SnackbarHost(hostState = snackbarHostState)

            if (isCompact) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                    CategoryHeaderTitle()
                    NewCategoryActionCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            creatingType = selectedType.name
                            isCreateDialogOpen = true
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    CategoryHeaderTitle()
                    NewCategoryActionCard(
                        modifier = Modifier.fillMaxWidth(0.42f),
                        onClick = {
                            creatingType = selectedType.name
                            isCreateDialogOpen = true
                        }
                    )
                }
            }

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

            featuredCategory?.let { category ->
                FeaturedCategoryCard(
                    category = category,
                    onEdit = { editingCategory = category },
                    onDelete = { deletingCategory = category }
                )
            }

            sideCategories.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    rowItems.forEach { category ->
                        CategoryTileCard(
                            modifier = Modifier.weight(1f),
                            category = category,
                            onEdit = { editingCategory = category },
                            onDelete = { deletingCategory = category }
                        )
                    }
                    repeat(2 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceXs))
        }

        if (isCreateDialogOpen) {
            CategoryEditorDialog(
                title = "Create Category",
                initialValue = CategoryEditorInput(
                    name = "",
                    description = "",
                    isCustom = true,
                    type = CategoryTypeFilter.valueOf(creatingType)
                ),
                onDismiss = { isCreateDialogOpen = false },
                onConfirm = { input ->
                    onAddCategory(input)
                    isCreateDialogOpen = false
                    coroutineScope.launch { snackbarHostState.showSnackbar("Category created") }
                }
            )
        }

        editingCategory?.let { category ->
            CategoryEditorDialog(
                title = "Edit Category",
                initialValue = CategoryEditorInput(
                    name = category.name,
                    description = category.description,
                    isCustom = category.isCustom,
                    type = category.type
                ),
                onDismiss = { editingCategory = null },
                onConfirm = { input ->
                    onUpdateCategory(category, input)
                    editingCategory = null
                    coroutineScope.launch { snackbarHostState.showSnackbar("Category updated") }
                }
            )
        }

        deletingCategory?.let { category ->
            AlertDialog(
                onDismissRequest = { deletingCategory = null },
                title = { Text("Delete category") },
                text = { Text("Delete ${category.name}? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (category.id in usedCategoryIds) {
                                deletingCategory = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Cannot delete category in use by transactions")
                                }
                                return@TextButton
                            }
                            onDeleteCategory(category)
                            deletingCategory = null
                            coroutineScope.launch { snackbarHostState.showSnackbar("Category deleted") }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingCategory = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun CategoryHeaderTitle() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
        Text(
            text = "Category Management",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Taxonomies.",
            style = MaterialTheme.typography.displayMedium
        )
    }
}

@Composable
private fun NewCategoryActionCard(
    modifier: Modifier,
    onClick: () -> Unit
) {
    AppCard(
        modifier = modifier,
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "New Category",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Expand Classification",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onClick)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun FeaturedCategoryCard(
    category: ManagedCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.large
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = if (category.isActive) "Active" else if (category.isCustom) "Custom" else "Default",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(text = category.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = category.amountLabel, style = MaterialTheme.typography.displaySmall)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(category.progress.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.small
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun CategoryTileCard(
    modifier: Modifier = Modifier,
    category: ManagedCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = category.icon, style = MaterialTheme.typography.labelLarge)
            }

            Text(text = category.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = category.amountLabel, style = MaterialTheme.typography.titleLarge)

            if (category.progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(category.progress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    title: String,
    initialValue: CategoryEditorInput,
    onDismiss: () -> Unit,
    onConfirm: (CategoryEditorInput) -> Unit
) {
    var name by remember(initialValue) { mutableStateOf(initialValue.name) }
    var description by remember(initialValue) { mutableStateOf(initialValue.description) }
    var isCustom by remember(initialValue) { mutableStateOf(initialValue.isCustom) }
    var selectedType by remember(initialValue) { mutableStateOf(initialValue.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                    CategoryTypeFilter.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
                FilterChip(
                    selected = isCustom,
                    onClick = { isCustom = !isCustom },
                    label = { Text(if (isCustom) "Custom" else "Default") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isEmpty()) return@TextButton
                    onConfirm(
                        CategoryEditorInput(
                            name = name.trim(),
                            description = description.trim(),
                            isCustom = isCustom,
                            type = selectedType
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun previewManagedCategories(): List<ManagedCategory> {
    return listOf(
        ManagedCategory(
            id = "c_food",
            name = "Dining Out",
            icon = "FD",
            type = CategoryTypeFilter.EXPENSE,
            isCustom = false,
            description = "Restaurants, cafes, and delivery.",
            amountLabel = "$1,450.00",
            progress = 0.65f,
            isActive = true
        ),
        ManagedCategory(
            id = "c_grocery",
            name = "Groceries",
            icon = "GR",
            type = CategoryTypeFilter.EXPENSE,
            isCustom = false,
            description = "Supermarkets and local markets.",
            amountLabel = "$820.45",
            progress = 0.40f,
            isActive = false
        ),
        ManagedCategory(
            id = "c_transit",
            name = "Transit",
            icon = "TR",
            type = CategoryTypeFilter.EXPENSE,
            isCustom = false,
            description = "Rideshares and public transport.",
            amountLabel = "$340.00",
            progress = 0.0f,
            isActive = false
        ),
        ManagedCategory(
            id = "c_salary",
            name = "Salary",
            icon = "SL",
            type = CategoryTypeFilter.INCOME,
            isCustom = false,
            description = "Monthly fixed salary income.",
            amountLabel = "$3,500.00",
            progress = 0.72f,
            isActive = true
        )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CategoryManagementScreenPreview() {
    DineSplitTheme {
        CategoryManagementScreen(onBack = {})
    }
}
