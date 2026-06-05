package com.example.dinesplit.presentation.personal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import com.example.dinesplit.core.ui.AppDimens
import com.example.dinesplit.core.ui.AppScaffold
import com.example.dinesplit.core.ui.AppTextField
import com.example.dinesplit.core.ui.BackNavigationButton
import com.example.dinesplit.core.ui.PrimaryButton
import com.example.dinesplit.core.ui.SecondaryButton
import com.example.dinesplit.data.model.StoredCategory
import com.example.dinesplit.domain.model.Transaction
import com.example.dinesplit.domain.model.TransactionType
import com.example.dinesplit.domain.validation.TransactionFormInput
import com.example.dinesplit.domain.validation.TransactionFormValidator
import com.example.dinesplit.domain.validation.toTransaction
import com.example.dinesplit.ui.theme.DineSplitTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    initialType: TransactionType? = null,
    currentUserId: String = "",
    availableCategoriesByType: Map<TransactionType, List<StoredCategory>> = emptyMap(),
    onSave: (Transaction) -> Unit = {},
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(initialType ?: TransactionType.EXPENSE) }
    var selectedCategoryId by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var isSubmitAttempted by rememberSaveable { mutableStateOf(false) }

    val currentDateMillis = remember { System.currentTimeMillis() }
    val currentDate = remember(currentDateMillis) { currentDateLabel(currentDateMillis) }
    val categoryOptions =
        remember(selectedType, availableCategoriesByType) {
            categoryTilesForType(
                type = selectedType,
                availableCategories = availableCategoriesByType[selectedType].orEmpty(),
            )
        }

    LaunchedEffect(selectedType) {
        if (selectedCategoryId.isNotBlank() && categoryOptions.none { it.id == selectedCategoryId }) {
            selectedCategoryId = ""
        }
    }

    val selectedCategoryName = categoryOptions.firstOrNull { it.id == selectedCategoryId }?.name.orEmpty()

    val validation =
        TransactionFormValidator.validate(
            input =
                TransactionFormInput(
                    amountText = amountText,
                    type = selectedType,
                    categoryId = selectedCategoryId,
                    categoryName = selectedCategoryName,
                    note = note,
                    dateMillis = currentDateMillis,
                ),
            availableCategoryIds = categoryOptions.map { it.id },
        )

    val isAmountValid = validation.amountError == null
    val isTypeValid = validation.typeError == null
    val isCategoryValid = validation.categoryError == null
    val isFormValid = validation.isValid

    AppScaffold(
         title = "Mục mới",
         navigationIcon = {
             BackNavigationButton(onClick = onBack)
         }
     ) {
         Column(
             modifier = Modifier
                 .fillMaxSize()
                 .verticalScroll(rememberScrollState()),
             verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXl)
         ) {
             Text(
                 text = "Mục mới.",
                 style = MaterialTheme.typography.displaySmall,
                 fontWeight = FontWeight.ExtraBold
             )

            AmountInputBlock(
                value = amountText,
                onValueChange = { amountText = it },
                isError = isSubmitAttempted && !isAmountValid,
                supportingText = validation.amountError,
            )

             Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                 Text(
                     text = "Loại",
                     style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )

                 Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                     TransactionType.entries.forEach { typeOption ->
                         FilterChip(
                             selected = selectedType == typeOption,
                             onClick = { selectedType = typeOption },
                             label = { Text(typeOption.displayLabel()) }
                         )
                     }
                 }

                 if (isSubmitAttempted && !isTypeValid) {
                     Text(
                         text = validation.typeError ?: "",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.error,
                         modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                     )
                 }
             }

             Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                 Text(
                     text = "Chọn danh mục",
                     style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )

                categoryOptions.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                    ) {
                        rowItems.forEach { item ->
                            CategoryTileButton(
                                modifier = Modifier.weight(1f),
                                tile = item,
                                selected = selectedCategoryId == item.id,
                                onClick = {
                                    selectedCategoryId = item.id
                                },
                            )
                        }

                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                 if (categoryOptions.isEmpty()) {
                     Text(
                         text = "Không có danh mục nào có sẵn. Tạo danh mục trước.",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                 }

                if (isSubmitAttempted && !isCategoryValid) {
                    Text(
                        text = validation.categoryError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }

             AppTextField(
                 modifier = Modifier.fillMaxWidth(),
                 value = currentDate,
                 onValueChange = {},
                 readOnly = true,
                 label = "Ngày"
             )

             AppTextField(
                 value = note,
                 onValueChange = { note = it },
                 label = "Ghi chú (tùy chọn)",
                 placeholder = "Thêm ghi chú...",
                 singleLine = false,
                 keyboardOptions = KeyboardOptions(
                     capitalization = KeyboardCapitalization.Sentences
                 ),
                 modifier = Modifier.fillMaxWidth()
             )

             PrimaryButton(
                 text = "Lưu mục",
                 onClick = {
                     isSubmitAttempted = true
                     if (!isFormValid) return@PrimaryButton
                     val validInput = validation.validInput ?: return@PrimaryButton

                     onSave(
                         validInput.toTransaction(
                             id = UUID.randomUUID().toString(),
                             userId = currentUserId
                         )
                     )
                     onBack()
                 },
                 enabled = isFormValid && currentUserId.isNotBlank()
             )

             SecondaryButton(
                 text = "Hủy",
                 onClick = onBack
             )
        }
    }
}

private data class CategoryTile(
    val id: String,
    val name: String,
    val iconCode: String,
)

private fun categoryTilesForType(
    type: TransactionType?,
    availableCategories: List<StoredCategory>,
): List<CategoryTile> {
    return availableCategories.map { category ->
        CategoryTile(id = category.id, name = category.name, iconCode = category.icon)
    }
}

@Composable
private fun AmountInputBlock(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    supportingText: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        ) {
            Text(
                text = "VND",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.displayLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 56.sp,
                    ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                decorationBox = { innerField ->
                    if (value.isBlank()) {
                        Text(
                            text = "0.00",
                            style =
                                MaterialTheme.typography.displayLarge.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 56.sp,
                                ),
                        )
                    }
                    innerField()
                },
            )
        }

        HorizontalDivider(
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
        )

        if (isError && !supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
private fun CategoryTileButton(
    modifier: Modifier = Modifier,
    tile: CategoryTile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        }
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = background,
        contentColor = contentColor,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.spaceMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                            shape = MaterialTheme.shapes.small,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tile.iconCode,
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                )
            }

            Text(
                text = tile.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun currentDateLabel(currentDateMillis: Long): String {
    val formatter = SimpleDateFormat("EEE, dd MMM", Locale("vi", "VN"))
    return formatter.format(Date(currentDateMillis))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AddTransactionScreenPreview() {
    DineSplitTheme {
        AddTransactionScreen(onBack = {})
    }
}
